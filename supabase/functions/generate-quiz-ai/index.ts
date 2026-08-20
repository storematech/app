// supabase/functions/generate-quiz-ai/index.ts
//
// Proxies a quiz-generation prompt (plus an optional PDF or up to a few photos) to an AI provider
// so no API key ever has to ship inside the Android (or web) client — they only ever live here,
// as server-side secrets. Nothing uploaded here is stored anywhere: the PDF/image bytes pass
// straight through to the provider as inline request data and are discarded once this request ends.
//
// Provider fallback chain (Gemini alone was hitting frequent per-minute/per-day rate limits and
// "model overloaded" errors): each request tries providers in this priority order, moving to the
// next only when one errors out, times out, or returns something unusable:
//   1. Groq        — fastest, tried first. Text-only requests use a JSON-mode-capable text model;
//                     photo requests use Groq's vision model. Groq has no PDF/document ingestion
//                     at all, so it's skipped entirely for PDF requests.
//   2. OpenRouter   — text uses a JSON-mode-capable text model; photos use a vision model; PDFs
//                     use OpenRouter's own file-parsing plugin (the only one of the three able to
//                     stand in for Gemini on PDFs) with the text model.
//   3. Cerebras     — text only (no vision/PDF support at all), tried last among the non-Gemini
//                     providers.
//   4. Gemini       — last resort, exactly as before (multimodal: text, PDF, and photos all work
//                      natively). If this also fails, the caller sees the same generic "high
//                      demand" message the app has always shown for a failed AI generation.
// Any provider whose API key isn't set in this function's secrets is skipped rather than erroring,
// so this works fine before all four keys are configured.
//
// Request body:  {
//   "prompt": string,               // topic, or extra context/instructions if pdf/images are attached
//   "questionCount": number,
//   "pdfBase64"?: string,           // base64 of a single PDF (mutually exclusive with images)
//   "images"?: [{ mimeType: string, data: string }]  // up to MAX_IMAGES photos, base64 each
// }
// Response body: { "success": true, "quizTitle": string, "questions": [{ text, options: [{ text, isCorrect }] }] }
//              | { "success": false, "error": string }

const GEMINI_MODEL = "gemini-flash-latest";
const GEMINI_URL = `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent`;

const GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
const OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
const CEREBRAS_URL = "https://api.cerebras.ai/v1/chat/completions";

// Overridable via the function's env vars (Deno.env) without a code change/redeploy.
const DEFAULT_GROQ_TEXT_MODEL = "openai/gpt-oss-120b";
const DEFAULT_GROQ_VISION_MODEL = "qwen/qwen3.6-27b";
const DEFAULT_OPENROUTER_TEXT_MODEL = "meta-llama/llama-3.3-70b-instruct";
const DEFAULT_OPENROUTER_VISION_MODEL = "qwen/qwen3-vl-8b-instruct";
const DEFAULT_CEREBRAS_MODEL = "llama-3.3-70b";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const MIN_QUESTIONS = 1;
const MAX_QUESTIONS = 15; // hard cap so a single request can't run away in cost

// Server-side safety nets — enforced here regardless of what the client already limited,
// since anyone with the anon key could otherwise call this function directly.
const MAX_PDF_BASE64_CHARS = 11_000_000; // ~8MB raw PDF
const MAX_IMAGES = 5;
const MAX_IMAGE_BASE64_CHARS = 2_800_000; // ~2MB raw per photo (client compresses well below this)

function buildPrompt(topic: string, questionCount: number, hasAttachment: boolean): string {
  const source = hasAttachment
    ? `Use the attached document/photo(s) as the source material. ${topic ? `Additional instructions: "${topic}".` : ""}`
    : `The topic is: "${topic}".`;

  return `You are a quiz question generator for a teacher's app. Generate exactly ${questionCount} single-choice quiz questions. ${source}

Rules:
- Each question has exactly 4 options, exactly one marked correct.
- Keep question and option text concise.
- Base every question strictly on the given source material or topic — do not invent unrelated content.
- Do not include explanations, numbering, or markdown — only the JSON described by the response schema.`;
}

const responseSchema = {
  type: "object",
  properties: {
    quizTitle: { type: "string" },
    questions: {
      type: "array",
      items: {
        type: "object",
        properties: {
          text: { type: "string" },
          options: {
            type: "array",
            items: {
              type: "object",
              properties: {
                text: { type: "string" },
                isCorrect: { type: "boolean" },
              },
              required: ["text", "isCorrect"],
            },
          },
        },
        required: ["text", "options"],
      },
    },
  },
  required: ["quizTitle", "questions"],
};

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

interface QuizResult {
  quizTitle: string;
  questions: unknown[];
}

async function fetchWithTimeout(url: string, init: RequestInit, timeoutMs: number): Promise<Response> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { ...init, signal: controller.signal });
  } catch (err) {
    if (err instanceof Error && err.name === "AbortError") {
      throw new Error(`Request timed out after ${timeoutMs}ms`);
    }
    throw err;
  } finally {
    clearTimeout(timer);
  }
}

// Groq/OpenRouter/Cerebras models are asked nicely (via the prompt) to return only JSON, but
// open-weight models frequently wrap it in a ```json fence anyway despite instructions not to —
// this recovers the object either way instead of failing the whole provider attempt over it.
function extractJsonObject(text: string): string {
  const fenced = text.match(/```(?:json)?\s*([\s\S]*?)```/i);
  if (fenced) return fenced[1].trim();
  const start = text.indexOf("{");
  const end = text.lastIndexOf("}");
  if (start !== -1 && end !== -1 && end > start) return text.slice(start, end + 1);
  return text.trim();
}

function parseQuizJson(rawText: string): QuizResult {
  const parsed = JSON.parse(extractJsonObject(rawText));
  if (!Array.isArray(parsed?.questions) || parsed.questions.length === 0) {
    throw new Error("Response had no questions");
  }
  return {
    quizTitle: typeof parsed.quizTitle === "string" ? parsed.quizTitle : "",
    questions: parsed.questions,
  };
}

function jsonShapeInstructions(): string {
  return `

Respond with ONLY a single JSON object — no markdown code fences, no commentary before or after — matching exactly this shape:
{"quizTitle": string, "questions": [{"text": string, "options": [{"text": string, "isCorrect": boolean}]}]}`;
}

function buildTextMessages(promptText: string): unknown[] {
  return [{ role: "user", content: promptText + jsonShapeInstructions() }];
}

function buildImageMessages(promptText: string, images: Array<{ mimeType: string; data: string }>): unknown[] {
  return [
    {
      role: "user",
      content: [
        { type: "text", text: promptText + jsonShapeInstructions() },
        ...images.map((img) => ({
          type: "image_url",
          image_url: { url: `data:${img.mimeType};base64,${img.data}` },
        })),
      ],
    },
  ];
}

function buildPdfMessages(promptText: string, pdfBase64: string): unknown[] {
  return [
    {
      role: "user",
      content: [
        { type: "text", text: promptText + jsonShapeInstructions() },
        { type: "file", file: { filename: "document.pdf", file_data: `data:application/pdf;base64,${pdfBase64}` } },
      ],
    },
  ];
}

/** Shared caller for the three OpenAI-compatible providers (Groq, OpenRouter, Cerebras). */
async function callChatCompletion(opts: {
  providerName: string;
  url: string;
  apiKey: string;
  model: string;
  messages: unknown[];
  jsonMode: boolean;
  maxTokens: number;
  timeoutMs: number;
}): Promise<QuizResult> {
  const res = await fetchWithTimeout(
    opts.url,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${opts.apiKey}`,
      },
      body: JSON.stringify({
        model: opts.model,
        messages: opts.messages,
        temperature: 0.4,
        max_tokens: opts.maxTokens,
        ...(opts.jsonMode ? { response_format: { type: "json_object" } } : {}),
      }),
    },
    opts.timeoutMs
  );

  if (!res.ok) {
    const errText = await res.text().catch(() => "");
    throw new Error(`${opts.providerName} HTTP ${res.status}: ${errText.slice(0, 300)}`);
  }

  const data = await res.json();
  const text = data?.choices?.[0]?.message?.content;
  if (typeof text !== "string" || !text.trim()) {
    throw new Error(`${opts.providerName} returned no content`);
  }
  return parseQuizJson(text);
}

async function callGemini(opts: {
  apiKey: string;
  promptText: string;
  pdfBase64: string | null;
  images: Array<{ mimeType?: string; data?: string }>;
  maxOutputTokens: number;
  timeoutMs: number;
}): Promise<QuizResult> {
  const parts: Array<Record<string, unknown>> = [{ text: opts.promptText }];
  if (opts.pdfBase64) {
    parts.push({ inlineData: { mimeType: "application/pdf", data: opts.pdfBase64 } });
  }
  for (const img of opts.images) {
    parts.push({ inlineData: { mimeType: img.mimeType, data: img.data } });
  }

  const res = await fetchWithTimeout(
    GEMINI_URL,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-goog-api-key": opts.apiKey,
      },
      body: JSON.stringify({
        contents: [{ parts }],
        generationConfig: {
          responseMimeType: "application/json",
          responseSchema,
          maxOutputTokens: opts.maxOutputTokens,
          temperature: 0.4,
        },
      }),
    },
    opts.timeoutMs
  );

  if (!res.ok) {
    const errText = await res.text().catch(() => "");
    console.error("Gemini error:", res.status, errText);
    throw new Error(`Gemini HTTP ${res.status}`);
  }

  const data = await res.json();
  const finishReason = data?.candidates?.[0]?.finishReason;
  const text = data?.candidates?.[0]?.content?.parts?.[0]?.text;
  if (!text) {
    console.error("Gemini returned no text. finishReason:", finishReason, JSON.stringify(data));
    throw new Error("Gemini returned no content");
  }

  try {
    return parseQuizJson(text);
  } catch (parseError) {
    console.error("Failed to parse Gemini JSON. finishReason:", finishReason, "raw text:", text);
    throw new Error("Gemini response was cut off");
  }
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  try {
    const groqKey = Deno.env.get("GROQ_API_KEY");
    const openRouterKey = Deno.env.get("OPENROUTER_API_KEY");
    const cerebrasKey = Deno.env.get("CEREBRAS_API_KEY");
    const geminiKey = Deno.env.get("GEMINI_API_KEY");

    if (!groqKey && !openRouterKey && !cerebrasKey && !geminiKey) {
      return jsonResponse({ success: false, error: "AI is not configured on the server." }, 500);
    }

    const body = await req.json().catch(() => null);
    const prompt = typeof body?.prompt === "string" ? body.prompt.trim() : "";
    const requestedCount = Number(body?.questionCount) || 5;
    const questionCount = Math.min(MAX_QUESTIONS, Math.max(MIN_QUESTIONS, Math.round(requestedCount)));

    const pdfBase64: string | null = typeof body?.pdfBase64 === "string" ? body.pdfBase64 : null;
    const images: Array<{ mimeType?: string; data?: string }> = Array.isArray(body?.images) ? body.images : [];

    if (pdfBase64 && images.length > 0) {
      return jsonResponse({ success: false, error: "Send either a PDF or photos, not both." }, 400);
    }
    if (pdfBase64 && pdfBase64.length > MAX_PDF_BASE64_CHARS) {
      return jsonResponse({ success: false, error: "That PDF is too large. Please use a smaller file." }, 400);
    }
    if (images.length > MAX_IMAGES) {
      return jsonResponse({ success: false, error: `You can attach at most ${MAX_IMAGES} photos.` }, 400);
    }
    for (const img of images) {
      if (typeof img.data !== "string" || typeof img.mimeType !== "string") {
        return jsonResponse({ success: false, error: "One of the photos couldn't be read. Please retake it." }, 400);
      }
      if (img.data.length > MAX_IMAGE_BASE64_CHARS) {
        return jsonResponse({ success: false, error: "One of the photos is too large. Please retake it." }, 400);
      }
    }

    const hasAttachment = Boolean(pdfBase64) || images.length > 0;
    if (!prompt && !hasAttachment) {
      return jsonResponse({ success: false, error: "Please describe the quiz you want to create." }, 400);
    }

    const promptText = buildPrompt(prompt, questionCount, hasAttachment);
    const validImages = images as Array<{ mimeType: string; data: string }>;

    // gemini-flash-latest (and Groq's gpt-oss models) spend part of their token budget on
    // internal "thinking" before writing the actual JSON, so the budget needs enough headroom
    // for that hidden reasoning on top of the answer itself, or the JSON gets truncated
    // mid-string. Still bounded (not unlimited) to keep cost predictable per request.
    const maxOutputTokens = 1536 + questionCount * 120;

    // Each entry is tried in order until one succeeds; a missing API key just means that
    // provider is skipped rather than attempted. See the file header for why the provider set
    // (and which model within it) differs by attachment type.
    const attempts: Array<() => Promise<QuizResult>> = [];

    if (pdfBase64) {
      // Only OpenRouter (via its own PDF-parsing plugin) can stand in for Gemini here — Groq has
      // no document ingestion at all, and Cerebras has neither vision nor document support.
      if (openRouterKey) {
        attempts.push(() =>
          callChatCompletion({
            providerName: "OpenRouter",
            url: OPENROUTER_URL,
            apiKey: openRouterKey,
            model: Deno.env.get("OPENROUTER_TEXT_MODEL") || DEFAULT_OPENROUTER_TEXT_MODEL,
            messages: buildPdfMessages(promptText, pdfBase64),
            jsonMode: true,
            maxTokens: maxOutputTokens,
            timeoutMs: 20_000,
          })
        );
      }
    } else if (validImages.length > 0) {
      if (groqKey) {
        attempts.push(() =>
          callChatCompletion({
            providerName: "Groq",
            url: GROQ_URL,
            apiKey: groqKey,
            model: Deno.env.get("GROQ_VISION_MODEL") || DEFAULT_GROQ_VISION_MODEL,
            messages: buildImageMessages(promptText, validImages),
            jsonMode: true,
            maxTokens: maxOutputTokens,
            timeoutMs: 12_000,
          })
        );
      }
      if (openRouterKey) {
        attempts.push(() =>
          callChatCompletion({
            providerName: "OpenRouter",
            url: OPENROUTER_URL,
            apiKey: openRouterKey,
            model: Deno.env.get("OPENROUTER_VISION_MODEL") || DEFAULT_OPENROUTER_VISION_MODEL,
            messages: buildImageMessages(promptText, validImages),
            jsonMode: false,
            maxTokens: maxOutputTokens,
            timeoutMs: 20_000,
          })
        );
      }
      // Cerebras has no vision support — skipped entirely for photo requests.
    } else {
      if (groqKey) {
        attempts.push(() =>
          callChatCompletion({
            providerName: "Groq",
            url: GROQ_URL,
            apiKey: groqKey,
            model: Deno.env.get("GROQ_TEXT_MODEL") || DEFAULT_GROQ_TEXT_MODEL,
            messages: buildTextMessages(promptText),
            jsonMode: true,
            maxTokens: maxOutputTokens,
            timeoutMs: 12_000,
          })
        );
      }
      if (openRouterKey) {
        attempts.push(() =>
          callChatCompletion({
            providerName: "OpenRouter",
            url: OPENROUTER_URL,
            apiKey: openRouterKey,
            model: Deno.env.get("OPENROUTER_TEXT_MODEL") || DEFAULT_OPENROUTER_TEXT_MODEL,
            messages: buildTextMessages(promptText),
            jsonMode: true,
            maxTokens: maxOutputTokens,
            timeoutMs: 20_000,
          })
        );
      }
      if (cerebrasKey) {
        attempts.push(() =>
          callChatCompletion({
            providerName: "Cerebras",
            url: CEREBRAS_URL,
            apiKey: cerebrasKey,
            model: Deno.env.get("CEREBRAS_MODEL") || DEFAULT_CEREBRAS_MODEL,
            messages: buildTextMessages(promptText),
            jsonMode: true,
            maxTokens: maxOutputTokens,
            timeoutMs: 12_000,
          })
        );
      }
    }

    // Gemini is always the last resort, exactly as it was the only option before.
    if (geminiKey) {
      attempts.push(() =>
        callGemini({
          apiKey: geminiKey,
          promptText,
          pdfBase64,
          images: validImages,
          maxOutputTokens,
          timeoutMs: 40_000,
        })
      );
    }

    if (attempts.length === 0) {
      // Every relevant provider for this request type is unconfigured (e.g. only CEREBRAS_API_KEY
      // is set but this is a photo request, which Cerebras can't handle at all).
      return jsonResponse({ success: false, error: "AI is not configured for this type of request." }, 500);
    }

    let result: QuizResult | null = null;
    for (const attempt of attempts) {
      try {
        result = await attempt();
        break;
      } catch (err) {
        console.error("AI provider attempt failed:", err instanceof Error ? err.message : err);
      }
    }

    if (!result) {
      // Every provider in the chain failed — same message the app has always shown for this.
      return jsonResponse({ success: false, error: "We're facing high demand. Please try again." }, 502);
    }

    return jsonResponse({
      success: true,
      quizTitle: result.quizTitle.trim() || prompt.slice(0, 60) || "AI Generated Quiz",
      questions: result.questions,
    });
  } catch (error) {
    console.error("generate-quiz-ai error:", error);
    return jsonResponse({ success: false, error: "Something went wrong generating the quiz." }, 500);
  }
});
