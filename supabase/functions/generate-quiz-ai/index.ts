// supabase/functions/generate-quiz-ai/index.ts
//
// Proxies a quiz-generation prompt (plus an optional PDF or up to a few photos) to Gemini so
// the API key never has to ship inside the Android (or web) client — it only ever lives here,
// as a server-side secret. Nothing uploaded here is stored anywhere: the PDF/image bytes pass
// straight through to Gemini as inline request data and are discarded once this request ends.
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

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  try {
    const apiKey = Deno.env.get("GEMINI_API_KEY");
    if (!apiKey) {
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

    // gemini-flash-latest spends part of maxOutputTokens on internal "thinking" before writing
    // the actual JSON (there's no reliable way to disable this — passing thinkingConfig gets
    // rejected with 400 INVALID_ARGUMENT on this model). So the budget needs enough headroom
    // for that hidden reasoning on top of the answer itself, or the JSON gets truncated
    // mid-string. Still bounded (not unlimited) to keep cost predictable per request.
    const maxOutputTokens = 1536 + questionCount * 120;

    const parts: Array<Record<string, unknown>> = [
      { text: buildPrompt(prompt, questionCount, hasAttachment) },
    ];
    if (pdfBase64) {
      parts.push({ inlineData: { mimeType: "application/pdf", data: pdfBase64 } });
    }
    for (const img of images) {
      parts.push({ inlineData: { mimeType: img.mimeType, data: img.data } });
    }

    const geminiRes = await fetch(GEMINI_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-goog-api-key": apiKey,
      },
      body: JSON.stringify({
        contents: [{ parts }],
        generationConfig: {
          responseMimeType: "application/json",
          responseSchema,
          maxOutputTokens,
          temperature: 0.4,
        },
      }),
    });

    if (!geminiRes.ok) {
      const errText = await geminiRes.text().catch(() => "");
      console.error("Gemini error:", geminiRes.status, errText);
      return jsonResponse({ success: false, error: "AI request failed. Please try again." }, 502);
    }

    const geminiData = await geminiRes.json();
    const finishReason = geminiData?.candidates?.[0]?.finishReason;
    const text = geminiData?.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!text) {
      console.error("Gemini returned no text. finishReason:", finishReason, JSON.stringify(geminiData));
      return jsonResponse({ success: false, error: "AI returned no content. Try a different prompt or file." }, 502);
    }

    let parsed: { quizTitle?: string; questions?: unknown[] };
    try {
      parsed = JSON.parse(text);
    } catch (parseError) {
      console.error("Failed to parse Gemini JSON. finishReason:", finishReason, "raw text:", text);
      return jsonResponse({ success: false, error: "AI response was cut off. Try fewer questions or try again." }, 502);
    }
    if (!Array.isArray(parsed?.questions) || parsed.questions.length === 0) {
      return jsonResponse({ success: false, error: "AI couldn't generate questions for that." }, 502);
    }

    return jsonResponse({
      success: true,
      quizTitle: typeof parsed.quizTitle === "string" && parsed.quizTitle.trim() ? parsed.quizTitle.trim() : (prompt.slice(0, 60) || "AI Generated Quiz"),
      questions: parsed.questions,
    });
  } catch (error) {
    console.error("generate-quiz-ai error:", error);
    return jsonResponse({ success: false, error: "Something went wrong generating the quiz." }, 500);
  }
});
