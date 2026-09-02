// supabase/functions/generate-full-test/index.ts
//
// Backend for the AI Quiz screen's second mode, "Full Test" — as opposed to the existing "Quick
// Test" (generate-quiz-ai), which takes one prompt/attachment and returns a handful of questions,
// this replicates an actual competitive exam's structure. Two phases, one function (picked via the
// request body's "phase" field), called in sequence by the client:
//
//  1. "research" — given just an exam name (e.g. "NEET UG", "SSC-CGL"), asks the AI for that
//     exam's real-world structure: total questions/marks/duration, question types, negative
//     marking scheme, and a subject -> chapter breakdown with weightage + difficulty split. The
//     client shows this to the user as an editable configuration step (question count, per-chapter
//     counts, difficulty, format, negative marking) before actually generating anything.
//
//  2. "generate" — given the exam name plus the user's *finalized* config from that step (which
//     may differ from the AI's own suggestion — the user can override anything), generates the
//     actual test. Each chapter's requested question count is split into batches of at most
//     MAX_QUESTIONS_PER_BATCH (same reliable ceiling generate-quiz-ai uses) and generated with its
//     own AI call — a 200-question mock test in one single AI call would be unreliably large/slow
//     and would risk getting cut off mid-response; per-chapter batching also means one chapter's
//     provider failure doesn't sink the whole test — see runGenerateBatch's partial-failure
//     handling below. Every batch tries the same Groq -> OpenRouter -> Cerebras -> Gemini fallback
//     chain generate-quiz-ai uses.
//
// Important honesty note: the "research" phase is the AI's own trained knowledge of the named
// exam, not a live web lookup — there's no search/browsing tool wired in here. For an exam whose
// pattern changed recently, or one the model has limited training data on, treat its numbers as a
// reasonable starting point the user can (and should be able to) edit, not an authoritative source.
//
// Auth + rate limiting: same "must resolve to a real signed-in user, trial-expiry blocks outright"
// rule as generate-quiz-ai, but with its OWN, much lower daily cap — a single "generate" call here
// can fan out into dozens of underlying AI calls (one per chapter batch), so it costs far more per
// invocation than a Quick Test generation and needs a correspondingly stricter limit. Tracked in
// its own full_test_generation_log table (see supabase/sql), separate from ai_generation_log.
//
// Request body:
//   { "phase": "research", "examName": string }
//   | { "phase": "generate", "examName": string, "config": GenerateConfig }
// Response body:
//   { "success": true, "phase": "research", "exam": ExamPattern }
//   | { "success": true, "phase": "generate", "quizTitle": string, "questions": GeneratedQuestion[],
//       "failedChapters"?: string[] }
//   | { "success": false, "error": string }

import { createClient } from "jsr:@supabase/supabase-js@2";

const GEMINI_MODEL = "gemini-flash-latest";
const GEMINI_URL = `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent`;

const GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
const OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
const CEREBRAS_URL = "https://api.cerebras.ai/v1/chat/completions";

// Same models generate-quiz-ai uses (text-only path) — overridable via env vars, no code change needed.
const DEFAULT_GROQ_TEXT_MODEL = "openai/gpt-oss-120b";
const DEFAULT_OPENROUTER_TEXT_MODEL = "meta-llama/llama-3.3-70b-instruct";
const DEFAULT_CEREBRAS_MODEL = "llama-3.3-70b";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

// Server-side safety nets, independent of whatever the client already limited to.
const MAX_TOTAL_QUESTIONS = 250; // generous ceiling for something like a full JEE/NEET mock
const MAX_CHAPTERS = 60;
const MAX_QUESTIONS_PER_BATCH = 15; // same proven-reliable ceiling as generate-quiz-ai's MAX_QUESTIONS

// Deliberately much lower than generate-quiz-ai's 10/25 — one "generate" call here can fan out
// into dozens of underlying provider calls (one per chapter batch), so it costs far more per
// invocation. Tracked in full_test_generation_log, not ai_generation_log.
const FULL_TEST_DAILY_LIMIT_FREE = 1;
const FULL_TEST_DAILY_LIMIT_PREMIUM = 3;
const PREMIUM_USER_TYPES = new Set(["starter", "school", "school pro"]);

// Mirrors util/TrialStatus.kt exactly — see generate-quiz-ai's identical copy for the full
// rationale (kept duplicated rather than shared, so this function stays a single self-contained
// file and touching it can never risk generate-quiz-ai's already-live behavior).
const TRIAL_DAYS = 3;

function isTrialExpired(
  profile: { user_type: string | null; license_expired_date: string | null; created_at: string | null } | null,
  now: Date,
): boolean {
  const userType = (profile?.user_type ?? "").toLowerCase();

  if (userType === "trial_extend" && profile?.license_expired_date) {
    const raw = profile.license_expired_date;
    const normalized = raw.includes("T") ? raw : `${raw}T23:59:59Z`;
    const end = new Date(normalized);
    if (!isNaN(end.getTime())) return now >= end;
  }

  if (!profile?.created_at) return true;
  const created = new Date(profile.created_at);
  if (isNaN(created.getTime())) return true;
  const elapsedDays = Math.floor((now.getTime() - created.getTime()) / (24 * 60 * 60 * 1000));
  return elapsedDays >= TRIAL_DAYS;
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

type AuthCheckResult =
  | { ok: true; userId: string; isPremium: boolean; admin: ReturnType<typeof createClient> }
  | { ok: false; status: number; error: string };

/** Auth + trial-expiry check only — shared by both phases. The daily "generate" quota is
 *  deliberately NOT checked or recorded here anymore (see checkGenerateQuota/recordGenerateUsage
 *  below): it used to live in this function and applied to "research" calls too, which meant
 *  just browsing exams before ever generating anything could burn a free user's entire 1/day
 *  quota, and — combined with the insert always firing regardless of outcome — a "generate" call
 *  that failed or timed out (see the 150s Edge Function execution limit note on runGenerate)
 *  still consumed the day's only attempt, locking the user out for 24h with nothing to show for
 *  it. Both bugs are why "research, then generate" so often ended in an immediate 429. */
async function resolveAuthorizedUser(
  req: Request,
  supabaseUrl: string,
  serviceRoleKey: string,
): Promise<AuthCheckResult> {
  const authHeader = req.headers.get("Authorization") ?? "";
  const jwt = authHeader.replace(/^Bearer\s+/i, "").trim();
  if (!jwt) {
    return { ok: false, status: 401, error: "Please sign in and try again." };
  }

  const admin = createClient(supabaseUrl, serviceRoleKey);

  const { data: userData, error: userError } = await admin.auth.getUser(jwt);
  if (userError || !userData?.user) {
    return { ok: false, status: 401, error: "Please sign in and try again." };
  }
  const userId = userData.user.id;

  const { data: profile } = await admin
    .from("profiles")
    .select("user_type, license_expired_date, created_at")
    .eq("id", userId)
    .maybeSingle();
  const isPremium = PREMIUM_USER_TYPES.has((profile?.user_type ?? "").toLowerCase());

  if (!isPremium && isTrialExpired(profile, new Date())) {
    return {
      ok: false,
      status: 403,
      error: "Your free trial has ended. Upgrade to Premium to keep generating full tests with AI.",
    };
  }

  return { ok: true, userId, isPremium, admin };
}

/** Only called for the "generate" phase — checks the rolling 24h count without recording
 *  anything yet, so a request that turns out to fail never touched the quota. */
async function checkGenerateQuota(
  admin: ReturnType<typeof createClient>,
  userId: string,
  isPremium: boolean,
): Promise<{ ok: true } | { ok: false; status: number; error: string }> {
  const dailyLimit = isPremium ? FULL_TEST_DAILY_LIMIT_PREMIUM : FULL_TEST_DAILY_LIMIT_FREE;
  const windowStart = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
  const { count, error: countError } = await admin
    .from("full_test_generation_log")
    .select("id", { count: "exact", head: true })
    .eq("user_id", userId)
    .gte("created_at", windowStart);
  if (countError) {
    console.error("full_test_generation_log count failed:", countError);
    return { ok: true };
  }
  if ((count ?? 0) >= dailyLimit) {
    const upsell = isPremium ? "" : " Upgrade to Premium for a higher daily limit.";
    return {
      ok: false,
      status: 429,
      error: `You've reached your daily Full Test generation limit (${dailyLimit}/day). Try again tomorrow.${upsell}`,
    };
  }
  return { ok: true };
}

/** Called only once a "generate" call has actually produced at least one question — a failed or
 *  fully-empty attempt (our own timeout, every provider down, etc.) must never cost the user their
 *  one daily try. */
async function recordGenerateUsage(admin: ReturnType<typeof createClient>, userId: string, examName: string): Promise<void> {
  const { error } = await admin.from("full_test_generation_log").insert({ user_id: userId, exam_name: examName });
  if (error) console.error("full_test_generation_log insert failed:", error);
}

/** How many of this user's PAST successful full-test generations were for this same exam —
 *  all-time, not just the 24h rate-limit window — so this generation can be titled "Set N"
 *  instead of every attempt getting an identical, indistinguishable quiz title. Call BEFORE
 *  recordGenerateUsage for this generation (so the count doesn't include itself); the result is
 *  this generation's set number minus one. */
async function countPriorGenerations(admin: ReturnType<typeof createClient>, userId: string, examName: string): Promise<number> {
  const { count, error } = await admin
    .from("full_test_generation_log")
    .select("id", { count: "exact", head: true })
    .eq("user_id", userId)
    .ilike("exam_name", examName);
  if (error) {
    console.error("full_test_generation_log set-count failed:", error);
    return 0;
  }
  return count ?? 0;
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

// Same "recover JSON even through a ```json fence" behavior as generate-quiz-ai — open-weight
// models frequently ignore "no markdown" instructions.
function extractJsonObject(text: string): string {
  const fenced = text.match(/```(?:json)?\s*([\s\S]*?)```/i);
  if (fenced) return fenced[1].trim();
  const start = text.indexOf("{");
  const end = text.lastIndexOf("}");
  if (start !== -1 && end !== -1 && end > start) return text.slice(start, end + 1);
  return text.trim();
}

interface ProviderKeys {
  groqKey?: string;
  openRouterKey?: string;
  cerebrasKey?: string;
  geminiKey?: string;
}

/** Text-only provider fallback chain, shared by both phases below — tries each configured
 *  provider in turn, returning the first one that produces a non-empty response; throws only if
 *  every configured provider failed.
 *
 *  Order is deliberately NOT the same as generate-quiz-ai's (Groq first there): production logs
 *  showed this feature's chapter fan-out — up to CHAPTER_CONCURRENCY calls at once, dozens per
 *  request — burning through Groq's shared-organization 8000 TPM/minute cap almost immediately,
 *  which is the same budget Quick Test's generate-quiz-ai draws from for its normal one-call-per-
 *  quiz usage. Trying Cerebras/OpenRouter first here means this bulk feature stops starving that
 *  shared Groq budget for everyone else, and only falls back to Groq (still ahead of Gemini, the
 *  slowest/last-resort option) when they're unavailable. */
async function callWithFallback(
  keys: ProviderKeys,
  prompt: string,
  maxOutputTokens: number,
): Promise<string> {
  const attempts: Array<() => Promise<string>> = [];

  if (keys.cerebrasKey) {
    attempts.push(() => callChatCompletion(CEREBRAS_URL, keys.cerebrasKey!, Deno.env.get("CEREBRAS_MODEL") || DEFAULT_CEREBRAS_MODEL, prompt, maxOutputTokens, 20_000, "Cerebras"));
  }
  if (keys.openRouterKey) {
    attempts.push(() => callChatCompletion(OPENROUTER_URL, keys.openRouterKey!, Deno.env.get("OPENROUTER_TEXT_MODEL") || DEFAULT_OPENROUTER_TEXT_MODEL, prompt, maxOutputTokens, 25_000, "OpenRouter"));
  }
  if (keys.groqKey) {
    attempts.push(() => callChatCompletion(GROQ_URL, keys.groqKey!, Deno.env.get("GROQ_TEXT_MODEL") || DEFAULT_GROQ_TEXT_MODEL, prompt, maxOutputTokens, 20_000, "Groq"));
  }
  if (keys.geminiKey) {
    attempts.push(() => callGemini(keys.geminiKey!, prompt, maxOutputTokens, 45_000));
  }

  if (attempts.length === 0) {
    throw new Error("AI is not configured on the server.");
  }

  let lastError: unknown = null;
  for (const attempt of attempts) {
    try {
      return await attempt();
    } catch (err) {
      lastError = err;
      console.error("Full-test provider attempt failed:", err instanceof Error ? err.message : err);
    }
  }
  throw lastError instanceof Error ? lastError : new Error("Every AI provider failed.");
}

async function callChatCompletion(
  url: string,
  apiKey: string,
  model: string,
  prompt: string,
  maxTokens: number,
  timeoutMs: number,
  providerName: string,
): Promise<string> {
  const res = await fetchWithTimeout(
    url,
    {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${apiKey}` },
      body: JSON.stringify({
        model,
        messages: [{ role: "user", content: prompt }],
        temperature: 0.4,
        max_tokens: maxTokens,
        response_format: { type: "json_object" },
      }),
    },
    timeoutMs,
  );
  if (!res.ok) {
    const errText = await res.text().catch(() => "");
    throw new Error(`${providerName} HTTP ${res.status}: ${errText.slice(0, 300)}`);
  }
  const data = await res.json();
  const text = data?.choices?.[0]?.message?.content;
  if (typeof text !== "string" || !text.trim()) throw new Error(`${providerName} returned no content`);
  return text;
}

async function callGemini(apiKey: string, prompt: string, maxOutputTokens: number, timeoutMs: number): Promise<string> {
  const res = await fetchWithTimeout(
    GEMINI_URL,
    {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-goog-api-key": apiKey },
      body: JSON.stringify({
        contents: [{ parts: [{ text: prompt }] }],
        generationConfig: { responseMimeType: "application/json", maxOutputTokens, temperature: 0.4 },
      }),
    },
    timeoutMs,
  );
  if (!res.ok) {
    const errText = await res.text().catch(() => "");
    console.error("Gemini error:", res.status, errText);
    throw new Error(`Gemini HTTP ${res.status}`);
  }
  const data = await res.json();
  const text = data?.candidates?.[0]?.content?.parts?.[0]?.text;
  if (!text) throw new Error("Gemini returned no content");
  return text;
}

// ---- Phase 1: research ------------------------------------------------------------------------

function buildResearchPrompt(examName: string): string {
  return `You are an expert on Indian competitive/entrance exams. Describe the real, actual exam pattern for: "${examName}".

Include:
- Total number of questions, total marks, and duration in minutes.
- Question types actually used (choose from: "mcq", "numerical", "descriptive").
- The real negative marking scheme, if any (marks awarded for a correct answer, marks deducted for a wrong one).
- Every subject in this exam, and for each subject every major chapter/topic actually tested, with:
  - Its approximate weightage as a percentage of that subject's questions.
  - Its approximate difficulty split (easy/medium/hard) as percentages that sum to 100.
- Ignore purely image-based/diagram-based question types — this system only generates text questions.
- Use your best real knowledge of this exam. If you are not confident about an exact number, give your best realistic estimate rather than refusing.

Respond with ONLY a single JSON object — no markdown code fences, no commentary before or after — matching exactly this shape:
{
  "examName": string,
  "totalQuestions": number,
  "totalMarks": number,
  "durationMinutes": number,
  "questionTypes": string[],
  "negativeMarking": { "enabled": boolean, "correctMarks": number, "incorrectMarks": number } | null,
  "subjects": [
    {
      "name": string,
      "chapters": [
        { "name": string, "weightagePercent": number, "easyPercent": number, "mediumPercent": number, "hardPercent": number }
      ]
    }
  ]
}`;
}

interface ExamChapter {
  name: string;
  weightagePercent: number;
  easyPercent: number;
  mediumPercent: number;
  hardPercent: number;
}
interface ExamSubject {
  name: string;
  chapters: ExamChapter[];
}
interface ExamPattern {
  examName: string;
  totalQuestions: number;
  totalMarks: number;
  durationMinutes: number;
  questionTypes: string[];
  negativeMarking: { enabled: boolean; correctMarks: number; incorrectMarks: number } | null;
  subjects: ExamSubject[];
}

function parseExamPattern(rawText: string): ExamPattern {
  const parsed = JSON.parse(extractJsonObject(rawText));
  if (!Array.isArray(parsed?.subjects) || parsed.subjects.length === 0) {
    throw new Error("Research response had no subjects");
  }
  return parsed as ExamPattern;
}

async function runResearch(examName: string, keys: ProviderKeys): Promise<ExamPattern> {
  const raw = await callWithFallback(keys, buildResearchPrompt(examName), 4000);
  return parseExamPattern(raw);
}

/** Checked BEFORE ever calling an AI provider — see supabase/sql/exam_patterns.sql. A hit here is
 *  instant, free, and can never fail with "high demand"; only an exam nobody's researched yet
 *  falls through to runResearch. Case-insensitive since the user can type any casing. */
async function fetchCachedPattern(
  admin: ReturnType<typeof createClient>,
  examName: string,
): Promise<ExamPattern | null> {
  const { data, error } = await admin
    .from("exam_patterns")
    .select("exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects")
    .ilike("exam_name", examName)
    .maybeSingle();
  if (error) {
    console.error("exam_patterns lookup failed:", error);
    return null;
  }
  if (!data) return null;
  return {
    examName: data.exam_name,
    totalQuestions: data.total_questions,
    totalMarks: data.total_marks,
    durationMinutes: data.duration_minutes,
    questionTypes: data.question_types ?? [],
    negativeMarking: data.negative_marking ?? null,
    subjects: data.subjects ?? [],
  };
}

/** Fire-and-forget cache write after a live AI research call, so the SAME exam name is never
 *  AI-researched twice. Never overwrites a hand-curated row (e.g. JEE Main's seed data) — only
 *  ever inserts, or updates a previous AI-generated cache entry for the same exam. */
async function cacheAiResearchedPattern(admin: ReturnType<typeof createClient>, exam: ExamPattern): Promise<void> {
  const { data: existing } = await admin
    .from("exam_patterns")
    .select("source")
    .ilike("exam_name", exam.examName)
    .maybeSingle();
  if (existing?.source === "curated") return;

  const { error } = await admin.from("exam_patterns").upsert(
    {
      exam_name: exam.examName,
      total_questions: exam.totalQuestions,
      total_marks: exam.totalMarks,
      duration_minutes: exam.durationMinutes,
      question_types: exam.questionTypes,
      negative_marking: exam.negativeMarking,
      subjects: exam.subjects,
      source: "ai_generated",
      updated_at: new Date().toISOString(),
    },
    { onConflict: "exam_name" },
  );
  if (error) console.error("exam_patterns cache write failed:", error);
}

// ---- Phase 2: generate -------------------------------------------------------------------------

interface ChapterConfig {
  subject: string;
  chapter: string;
  questionCount: number;
  easyCount: number;
  mediumCount: number;
  hardCount: number;
}
interface NegativeMarkingConfig {
  enabled: boolean;
  correctMarks: number;
  incorrectMarks: number;
}
interface GenerateConfig {
  totalQuestions: number;
  chapters: ChapterConfig[];
  formats: string[]; // subset of "mcq" | "numerical" | "descriptive"
  negativeMarking: NegativeMarkingConfig | null;
}

interface GeneratedQuestion {
  subject: string;
  chapter: string;
  difficulty: "easy" | "medium" | "hard";
  format: string;
  text: string;
  options?: Array<{ text: string; isCorrect: boolean }>;
  numericalAnswer?: string;
  explanation?: string;
}

function shuffle<T>(items: T[]): T[] {
  const arr = [...items];
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

/** Checked BEFORE any AI call for a chapter — see supabase/sql/exam_question_bank.sql. Every row
 *  that matches is free, instant, and never wrong the way an AI-hallucinated answer occasionally
 *  is; only whatever shortfall the bank can't cover for this chapter falls through to the AI
 *  batch loop below. Small result sets (a handful of rows per chapter), so shuffling in JS instead
 *  of pushing randomization into the query is simplest. */
interface BankRow {
  subject: string;
  chapter: string;
  difficulty: string;
  format: string;
  text: string;
  options: Array<{ text: string; isCorrect: boolean }> | null;
  numerical_answer: string | null;
  explanation: string | null;
}

async function fetchBankQuestions(
  admin: ReturnType<typeof createClient>,
  examName: string,
  chapter: ChapterConfig,
  formats: string[],
): Promise<GeneratedQuestion[]> {
  const { data, error } = await admin
    .from("exam_question_bank")
    .select("subject, chapter, difficulty, format, text, options, numerical_answer, explanation")
    .ilike("exam_name", examName)
    .eq("subject", chapter.subject)
    .eq("chapter", chapter.chapter)
    .in("format", formats);
  if (error) {
    console.error("exam_question_bank lookup failed:", error);
    return [];
  }
  const rows = (data ?? []) as unknown as BankRow[];
  return shuffle(rows).map((row) => ({
    subject: row.subject,
    chapter: row.chapter,
    difficulty: row.difficulty as "easy" | "medium" | "hard",
    format: row.format,
    text: row.text,
    options: row.options ?? undefined,
    numericalAnswer: row.numerical_answer ?? undefined,
    explanation: row.explanation ?? undefined,
  }));
}

function buildBatchPrompt(
  examName: string,
  chapter: ChapterConfig,
  batchCount: number,
  easyCount: number,
  mediumCount: number,
  hardCount: number,
  formats: string[],
): string {
  return `You are generating practice questions for "${examName}", subject "${chapter.subject}", chapter "${chapter.chapter}".

Generate exactly ${batchCount} questions that match this exam's real style and difficulty level for this chapter:
- ${easyCount} easy, ${mediumCount} medium, ${hardCount} hard.
- For each question, pick whichever format from this allowed list best suits it: ${formats.join(", ")}.
  - "mcq": exactly 4 options. Almost every mcq question should have exactly one option marked correct — but if this chapter is math/physics-heavy, you may occasionally (at most 1 in every 10 mcq questions) mark MORE THAN ONE option correct when the content genuinely supports it, matching the "which of the following statements is/are true" style used in exams like JEE Advanced. Do not force this; most mcq questions must stay single-correct.
  - "numerical": no options — the question expects a numeric final answer (put it in "numericalAnswer" as a string).
  - "descriptive": no options — a short-answer/explanation-style question (still include an "explanation" with the expected answer).
- For questions that genuinely involve mathematical notation (fractions, exponents, integrals, complex numbers, set notation, etc.), write it as LaTeX wrapped in $...$ for inline math or $$...$$ for a standalone equation — e.g. "Evaluate $\\int_0^1 x^2\\,dx$." Only use this when the subject actually calls for such notation.
- Base every question strictly on real concepts from this chapter — do not invent unrelated content.
- Do not include numbering or markdown formatting (no **bold**, no bullet lists) — only the JSON described below. LaTeX math delimiters ($...$, $$...$$) are not markdown and are fine to use where relevant.

Respond with ONLY a single JSON object — no markdown code fences, no commentary before or after — matching exactly this shape:
{"questions": [{"difficulty": "easy"|"medium"|"hard", "format": "mcq"|"numerical"|"descriptive", "text": string, "options"?: [{"text": string, "isCorrect": boolean}], "numericalAnswer"?: string, "explanation"?: string}]}`;
}

function parseBatchQuestions(rawText: string): Array<Omit<GeneratedQuestion, "subject" | "chapter">> {
  const parsed = JSON.parse(extractJsonObject(rawText));
  if (!Array.isArray(parsed?.questions) || parsed.questions.length === 0) {
    throw new Error("Batch response had no questions");
  }
  return parsed.questions;
}

/** Splits one chapter's requested count into <= MAX_QUESTIONS_PER_BATCH chunks, generating each
 *  with its own provider-fallback call. A chunk that fails after every provider is dropped rather
 *  than failing the whole chapter — the caller still gets whatever chunks succeeded.
 *
 *  The curated exam_question_bank is checked FIRST (see fetchBankQuestions) — any questions it
 *  can supply for this chapter/format cost no AI call at all. Only the shortfall the bank can't
 *  cover falls through to the AI batch loop below, with the easy/medium/hard split scaled down
 *  proportionally to match how much of the chapter is still needed. */
async function generateChapter(
  examName: string,
  chapter: ChapterConfig,
  formats: string[],
  keys: ProviderKeys,
  admin: ReturnType<typeof createClient>,
): Promise<{ questions: GeneratedQuestion[]; fullyFailed: boolean }> {
  // A chapter configured with 0 questions (e.g. the user zeroed it out in the config step) isn't
  // a failure — just nothing to generate.
  if (chapter.questionCount <= 0) {
    return { questions: [], fullyFailed: false };
  }

  const banked = (await fetchBankQuestions(admin, examName, chapter, formats)).slice(0, chapter.questionCount);
  if (banked.length >= chapter.questionCount) {
    return { questions: banked, fullyFailed: false };
  }

  const results: GeneratedQuestion[] = [...banked];
  let anySucceeded = false;
  // Scale the remaining easy/medium/hard split down to just the shortfall the bank didn't cover,
  // in the same proportion as the chapter's original mix.
  const coverageRatio = (chapter.questionCount - banked.length) / chapter.questionCount;
  let remaining = chapter.questionCount - banked.length;
  let remainingEasy = Math.round(chapter.easyCount * coverageRatio);
  let remainingHard = Math.round(chapter.hardCount * coverageRatio);
  let remainingMedium = Math.max(0, remaining - remainingEasy - remainingHard);

  while (remaining > 0) {
    const batchCount = Math.min(MAX_QUESTIONS_PER_BATCH, remaining);
    // Proportionally slice this batch's difficulty mix off the chapter's remaining totals.
    const batchEasy = Math.min(remainingEasy, Math.round((remainingEasy / remaining) * batchCount));
    const batchHard = Math.min(remainingHard, Math.round((remainingHard / remaining) * batchCount));
    const batchMedium = Math.max(0, batchCount - batchEasy - batchHard);

    try {
      const raw = await callWithFallback(
        keys,
        buildBatchPrompt(examName, chapter, batchCount, batchEasy, batchMedium, batchHard, formats),
        // Bumped from 1536 + batchCount*150 — that budget was too tight and several batches came
        // back as "max completion tokens reached before generating a valid document" (a truncated,
        // unparseable JSON body from Groq), silently dropping the whole batch even though the
        // model would have produced a valid answer given enough room.
        2048 + batchCount * 220,
      );
      const batchQuestions = parseBatchQuestions(raw);
      for (const q of batchQuestions) {
        results.push({ ...q, subject: chapter.subject, chapter: chapter.chapter });
      }
      anySucceeded = true;
    } catch (err) {
      console.error(`Chapter "${chapter.chapter}" batch of ${batchCount} failed:`, err instanceof Error ? err.message : err);
      // This chunk is dropped — the rest of the chapter (and the rest of the test) still proceeds.
    }

    remaining -= batchCount;
    remainingEasy -= batchEasy;
    remainingMedium -= batchMedium;
    remainingHard -= batchHard;
  }

  // "Fully failed" means nothing at all came back for this chapter — banked questions alone
  // (even with a partially-failed AI shortfall) still count as a partial success, not a failure.
  return { questions: results, fullyFailed: results.length === 0 && !anySucceeded };
}

// A full test can mean dozens of chapters, each its own sequential-batch AI call with up to a
// 4-provider fallback chain (worst case ~110s if only the last provider answers) — run strictly
// one-at-a-time, a large exam's total wall time can blow past both this function's own execution
// limit and the client's request timeout well before every chapter gets a turn, which is what a
// real "We're facing high demand" (0 questions came back) or an outright client-side timeout looks
// like from the Configure screen. Bounded concurrency keeps each provider's per-minute rate limit
// respected while cutting total wall time roughly proportional to CHAPTER_CONCURRENCY.
const CHAPTER_CONCURRENCY = 4;

async function mapWithConcurrency<T, R>(items: T[], concurrency: number, fn: (item: T) => Promise<R>): Promise<R[]> {
  const results = new Array<R>(items.length);
  let nextIndex = 0;
  async function worker() {
    while (true) {
      const current = nextIndex++;
      if (current >= items.length) return;
      results[current] = await fn(items[current]);
    }
  }
  await Promise.all(Array.from({ length: Math.min(concurrency, items.length) }, worker));
  return results;
}

// Supabase Edge Functions get hard-killed at their own platform execution limit — observed in
// production logs as a 503 at exactly ~150_000ms wall time, with NO response body at all (not
// even our own error JSON), which the client can only see as a bare network failure. A large full
// test's chapter fan-out can genuinely take that long, especially once a shared-organization
// provider TPM cap (see callWithFallback's Groq 429s) forces slower fallback providers for many
// batches at once. Rather than risk that hard kill, runGenerate keeps its own deadline well inside
// the platform's and, once passed, stops starting new chapters and reports the rest as failed —
// so the client always gets a real, parseable response (a partial test) instead of nothing.
const EXECUTION_BUDGET_MS = 110_000;

async function runGenerate(
  examName: string,
  config: GenerateConfig,
  keys: ProviderKeys,
  admin: ReturnType<typeof createClient>,
): Promise<{ questions: GeneratedQuestion[]; failedChapters: string[] }> {
  const formats = config.formats.length > 0 ? config.formats : ["mcq"];
  const allQuestions: GeneratedQuestion[] = [];
  const failedChapters: string[] = [];
  const deadline = Date.now() + EXECUTION_BUDGET_MS;

  const perChapter = await mapWithConcurrency(config.chapters, CHAPTER_CONCURRENCY, (chapter) => {
    if (Date.now() > deadline) {
      return Promise.resolve({ questions: [], fullyFailed: true });
    }
    return generateChapter(examName, chapter, formats, keys, admin);
  });

  config.chapters.forEach((chapter, i) => {
    const { questions, fullyFailed } = perChapter[i];
    allQuestions.push(...questions);
    if (fullyFailed) failedChapters.push(`${chapter.subject} — ${chapter.chapter}`);
  });

  return { questions: allQuestions, failedChapters };
}

// ---- HTTP handler -------------------------------------------------------------------------------

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  try {
    const keys: ProviderKeys = {
      groqKey: Deno.env.get("GROQ_API_KEY") ?? undefined,
      openRouterKey: Deno.env.get("OPENROUTER_API_KEY") ?? undefined,
      cerebrasKey: Deno.env.get("CEREBRAS_API_KEY") ?? undefined,
      geminiKey: Deno.env.get("GEMINI_API_KEY") ?? undefined,
    };
    if (!keys.groqKey && !keys.openRouterKey && !keys.cerebrasKey && !keys.geminiKey) {
      return jsonResponse({ success: false, error: "AI is not configured on the server." }, 500);
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (!supabaseUrl || !serviceRoleKey) {
      return jsonResponse({ success: false, error: "Server not configured." }, 500);
    }

    const authCheck = await resolveAuthorizedUser(req, supabaseUrl, serviceRoleKey);
    if (!authCheck.ok) {
      return jsonResponse({ success: false, error: authCheck.error }, authCheck.status);
    }

    const body = await req.json().catch(() => null);
    const phase = body?.phase;
    const examName = typeof body?.examName === "string" ? body.examName.trim() : "";
    if (!examName) {
      return jsonResponse({ success: false, error: "Please choose an exam first." }, 400);
    }

    if (phase === "research") {
      const cached = await fetchCachedPattern(authCheck.admin, examName);
      if (cached) {
        return jsonResponse({ success: true, phase: "research", exam: cached });
      }
      try {
        const exam = await runResearch(examName, keys);
        // Don't block the response on this — the user gets their result either way, and a failed
        // cache write just means this exam gets AI-researched again next time.
        cacheAiResearchedPattern(authCheck.admin, exam).catch((err) => console.error("cacheAiResearchedPattern failed:", err));
        return jsonResponse({ success: true, phase: "research", exam });
      } catch (err) {
        console.error("Full-test research failed:", err instanceof Error ? err.message : err);
        return jsonResponse({ success: false, error: "Couldn't research that exam right now. Please try again." }, 502);
      }
    }

    if (phase === "generate") {
      // Checked here (not for "research") and only recorded on a real success below — see
      // resolveAuthorizedUser's KDoc for why this used to lock free users out after a single
      // research call or a failed/timed-out attempt.
      const quota = await checkGenerateQuota(authCheck.admin, authCheck.userId, authCheck.isPremium);
      if (!quota.ok) {
        return jsonResponse({ success: false, error: quota.error }, quota.status);
      }

      const config = body?.config as GenerateConfig | undefined;
      if (!config || !Array.isArray(config.chapters) || config.chapters.length === 0) {
        return jsonResponse({ success: false, error: "Please set up at least one chapter first." }, 400);
      }
      if (config.chapters.length > MAX_CHAPTERS) {
        return jsonResponse({ success: false, error: `Please select at most ${MAX_CHAPTERS} chapters.` }, 400);
      }
      const totalRequested = config.chapters.reduce((sum, c) => sum + Math.max(0, c.questionCount), 0);
      if (totalRequested === 0) {
        return jsonResponse({ success: false, error: "Please request at least one question." }, 400);
      }
      if (totalRequested > MAX_TOTAL_QUESTIONS) {
        return jsonResponse({ success: false, error: `Please request at most ${MAX_TOTAL_QUESTIONS} questions in one test.` }, 400);
      }

      const { questions, failedChapters } = await runGenerate(examName, config, keys, authCheck.admin);
      if (questions.length === 0) {
        return jsonResponse({ success: false, error: "We're facing high demand. Please try again." }, 502);
      }

      // Counted BEFORE recordGenerateUsage inserts this generation's own row, so "Set N" reflects
      // this attempt correctly (1st successful generation of this exam -> Set 1, and so on).
      const priorCount = await countPriorGenerations(authCheck.admin, authCheck.userId, examName);
      const setNumber = priorCount + 1;
      await recordGenerateUsage(authCheck.admin, authCheck.userId, examName);

      return jsonResponse({
        success: true,
        phase: "generate",
        quizTitle: `${examName} - Set ${setNumber}`,
        questions,
        ...(failedChapters.length > 0 ? { failedChapters } : {}),
      });
    }

    return jsonResponse({ success: false, error: `Unknown phase: ${phase}` }, 400);
  } catch (error) {
    console.error("generate-full-test error:", error);
    return jsonResponse({ success: false, error: "Something went wrong generating the test." }, 500);
  }
});
