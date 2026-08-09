// supabase/functions/scheduled-emails/index.ts
//
// Runs on a daily schedule (Supabase Dashboard > Edge Functions > scheduled-emails > Cron, e.g.
// "0 3 * * *" for 3am UTC daily). Does two things in one run, so a single cron job covers both
// timing needs instead of two separate scheduled functions:
//  1. Every day: emails anyone whose free trial ends within the next 24 hours.
//  2. Sundays only (UTC): emails every quiz creator a weekly summary of the past 7 days' activity —
//     quiz-wise submission count, top/lowest scorer, and per-question correct-rate breakdown.
// Never talks to Brevo directly — both paths delegate the actual send to send-email, which is the
// only function that owns Brevo API access.
//
// Response body: { "success": true, "trialRemindersSent": number, "weeklySummariesSent": number }

import { createClient, SupabaseClient } from "jsr:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

const TRIAL_DAYS = 7; // mirrors util/TrialStatus.kt (Android) / the web app's trial rule
const PREMIUM_USER_TYPES = ["starter", "school", "school pro"]; // mirrors Profile.isPremium (Android)
const DAY_MS = 24 * 60 * 60 * 1000;

async function callSendEmail(supabaseUrl: string, serviceRoleKey: string, payload: Record<string, unknown>): Promise<boolean> {
  const res = await fetch(`${supabaseUrl}/functions/v1/send-email`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${serviceRoleKey}`,
      apikey: serviceRoleKey,
    },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    console.error("send-email call failed:", await res.text());
    return false;
  }
  return true;
}

/** Anyone whose trial ends within the next 24h — a daily cron naturally catches each user ~once. */
async function sendTrialEndingReminders(supabase: SupabaseClient, supabaseUrl: string, serviceRoleKey: string): Promise<number> {
  const now = Date.now();
  const windowStart = new Date(now - TRIAL_DAYS * DAY_MS).toISOString();
  const windowEnd = new Date(now - (TRIAL_DAYS - 1) * DAY_MS).toISOString();

  const { data: profiles, error } = await supabase
    .from("profiles")
    .select("email, name, user_type, created_at")
    .gte("created_at", windowStart)
    .lt("created_at", windowEnd);

  if (error) {
    console.error("Failed to query trial-ending profiles:", error);
    return 0;
  }

  let sent = 0;
  for (const profile of profiles ?? []) {
    if (PREMIUM_USER_TYPES.includes((profile.user_type ?? "").toLowerCase())) continue;
    if (!profile.email) continue;
    const ok = await callSendEmail(supabaseUrl, serviceRoleKey, {
      emailType: "trial_ending",
      email: profile.email,
      name: profile.name,
      daysLeft: 1,
    });
    if (ok) sent++;
  }
  return sent;
}

interface ResponseRow {
  id: string;
  quiz_id: string;
  user_name: string | null;
  user_email: string;
  score: number | null;
  completed_at: string | null;
}

/** Every quiz creator with at least one submission in the past 7 days gets a per-quiz breakdown. */
async function sendWeeklySummaries(supabase: SupabaseClient, supabaseUrl: string, serviceRoleKey: string): Promise<number> {
  const now = new Date();
  const weekAgo = new Date(now.getTime() - 7 * DAY_MS);
  const weekLabel = `${weekAgo.toLocaleDateString("en-US", { month: "short", day: "numeric" })} – ${now.toLocaleDateString("en-US", { month: "short", day: "numeric" })}`;

  const { data: quizzes, error: quizzesError } = await supabase.from("quizzes").select("id, title, created_by");
  if (quizzesError || !quizzes?.length) {
    if (quizzesError) console.error("Failed to load quizzes:", quizzesError);
    return 0;
  }

  const quizIds = quizzes.map((q) => q.id);
  const { data: responses, error: responsesError } = await supabase
    .from("quiz_responses")
    .select("id, quiz_id, user_name, user_email, score, completed_at")
    .in("quiz_id", quizIds)
    .eq("completed", true)
    .gte("completed_at", weekAgo.toISOString());
  if (responsesError) {
    console.error("Failed to load quiz_responses:", responsesError);
    return 0;
  }
  const responseRows = (responses ?? []) as ResponseRow[];

  const responseIds = responseRows.map((r) => r.id);
  const { data: answerDetails } = responseIds.length
    ? await supabase.from("quiz_answer_details").select("response_id, question_text, is_correct").in("response_id", responseIds)
    : { data: [] as { response_id: string; question_text: string | null; is_correct: boolean | null }[] };

  const responsesByQuiz = new Map<string, ResponseRow[]>();
  const responseIdToQuiz = new Map<string, string>();
  for (const r of responseRows) {
    responsesByQuiz.set(r.quiz_id, [...(responsesByQuiz.get(r.quiz_id) ?? []), r]);
    responseIdToQuiz.set(r.id, r.quiz_id);
  }

  // question_text -> { correct, total }, scoped per quiz.
  const questionStatsByQuiz = new Map<string, Map<string, { correct: number; total: number }>>();
  for (const detail of answerDetails ?? []) {
    const quizId = responseIdToQuiz.get(detail.response_id);
    if (!quizId || !detail.question_text) continue;
    const quizMap = questionStatsByQuiz.get(quizId) ?? new Map();
    const stat = quizMap.get(detail.question_text) ?? { correct: 0, total: 0 };
    stat.total += 1;
    if (detail.is_correct) stat.correct += 1;
    quizMap.set(detail.question_text, stat);
    questionStatsByQuiz.set(quizId, quizMap);
  }

  const quizzesByCreator = new Map<string, typeof quizzes>();
  for (const q of quizzes) {
    if (!q.created_by) continue;
    quizzesByCreator.set(q.created_by, [...(quizzesByCreator.get(q.created_by) ?? []), q]);
  }

  let sent = 0;
  for (const [creatorId, creatorQuizzes] of quizzesByCreator) {
    const quizStats = creatorQuizzes
      .map((quiz) => {
        const quizResponses = responsesByQuiz.get(quiz.id) ?? [];
        if (quizResponses.length === 0) return null;

        const sorted = [...quizResponses].sort((a, b) => (b.score ?? 0) - (a.score ?? 0));
        const top = sorted[0];
        const low = sorted[sorted.length - 1];
        const questionMap = questionStatsByQuiz.get(quiz.id) ?? new Map();
        const questions = Array.from(questionMap.entries()).map(([text, stat]) => ({
          text,
          correctPercent: stat.total > 0 ? Math.round((stat.correct / stat.total) * 100) : 0,
          totalAttempts: stat.total,
        }));

        return {
          title: quiz.title as string,
          participantCount: quizResponses.length,
          topScorer: { name: top.user_name || top.user_email, score: top.score ?? 0 },
          lowScorer: low.id !== top.id ? { name: low.user_name || low.user_email, score: low.score ?? 0 } : null,
          questions,
        };
      })
      .filter((q): q is NonNullable<typeof q> => q !== null);

    if (quizStats.length === 0) continue; // nothing happened this week for this creator

    const { data: creatorProfile } = await supabase.from("profiles").select("email, name").eq("id", creatorId).maybeSingle();
    if (!creatorProfile?.email) continue;

    const ok = await callSendEmail(supabaseUrl, serviceRoleKey, {
      emailType: "weekly_summary",
      email: creatorProfile.email,
      name: creatorProfile.name,
      weekLabel,
      quizzes: quizStats,
    });
    if (ok) sent++;
  }
  return sent;
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (!supabaseUrl || !serviceRoleKey) {
      return jsonResponse({ success: false, error: "Server not configured." }, 500);
    }

    const supabase = createClient(supabaseUrl, serviceRoleKey);

    const trialRemindersSent = await sendTrialEndingReminders(supabase, supabaseUrl, serviceRoleKey);

    // UTC Sunday — adjust here if your users are concentrated in a different timezone.
    const isSunday = new Date().getUTCDay() === 0;
    const weeklySummariesSent = isSunday ? await sendWeeklySummaries(supabase, supabaseUrl, serviceRoleKey) : 0;

    return jsonResponse({ success: true, trialRemindersSent, weeklySummariesSent });
  } catch (error) {
    console.error("scheduled-emails error:", error);
    return jsonResponse({ success: false, error: "Something went wrong." }, 500);
  }
});
