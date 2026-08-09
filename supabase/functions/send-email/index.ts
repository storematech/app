// supabase/functions/send-email/index.ts
//
// The ONLY function that talks to Brevo — every trigger point (payment verification, the daily
// cron, the profiles-INSERT DB webhook) calls this one instead of hitting Brevo directly, so
// there's a single place that owns "how do we actually send an email," the Brevo API key only
// needs to be configured once, and adding a 5th email type later still doesn't mean a new function.
// All HTML is written inline below (no Brevo dashboard template needed/used).
//
// Two ways to invoke:
//  1. Direct call:  { "emailType": "welcome"|"trial_ending"|"license_purchased"|"weekly_summary", ...fields }
//  2. A Database Webhook on `profiles` INSERT (same payload shape notify-quiz-submission's webhook
//     already uses): { "type": "INSERT", "table": "profiles", "record": {...} } — auto-detected and
//     treated as a "welcome" email using record.email/record.name, no separate function needed.
//
// Response body: { "success": true } | { "success": false, "error": string }

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

const BRAND_COLOR = "#2563EB";
const APP_NAME = "YunoLMS";
const APP_URL = "https://app.yunolms.com";
// Not a secret — this shows up in every recipient's "From" header regardless, so it lives here as
// a plain constant rather than another Supabase secret to configure. yunolms.com is now verified
// (domain authentication, SPF/DKIM) in Brevo, so any @yunolms.com address can send from here.
const SENDER_EMAIL = "alert_no_reply@yunolms.com";
const SENDER_NAME = APP_NAME;

function escapeHtml(value: string): string {
  return value.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

/** Shared header/footer wrapper — inline styles only, since most email clients strip <style> blocks. */
function layout(title: string, bodyHtml: string): string {
  return `
<div style="font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;background:#F5F5FB;padding:24px 0;">
  <div style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #E5E7EB;">
    <div style="background:${BRAND_COLOR};padding:24px 28px;">
      <span style="color:#ffffff;font-size:20px;font-weight:700;">${APP_NAME}</span>
    </div>
    <div style="padding:28px;color:#111827;font-size:14px;line-height:22px;">
      <h1 style="font-size:19px;margin:0 0 12px;color:#111827;">${title}</h1>
      ${bodyHtml}
    </div>
    <div style="padding:18px 28px;background:#F5F5FB;color:#6B7280;font-size:12px;">
      Sent by ${APP_NAME} &middot; <a href="${APP_URL}" style="color:${BRAND_COLOR};">${APP_URL}</a>
    </div>
  </div>
</div>`;
}

function button(label: string, href: string): string {
  return `<a href="${href}" style="display:inline-block;margin-top:16px;background:${BRAND_COLOR};color:#ffffff;text-decoration:none;padding:12px 22px;border-radius:10px;font-weight:600;font-size:14px;">${label}</a>`;
}

// ---- Per-type content builders ---------------------------------------------------------------

function welcomeEmail(name: string) {
  const subject = `Welcome to ${APP_NAME}!`;
  const html = layout(
    `Welcome aboard, ${escapeHtml(name || "there")} 👋`,
    `<p>Your ${APP_NAME} account is ready. Create your first quiz with AI in seconds, share it with a link or QR code, and watch results roll in live.</p>
     ${button(`Open ${APP_NAME}`, APP_URL)}`,
  );
  return { subject, html };
}

function trialEndingEmail(name: string, daysLeft: number) {
  const subject = daysLeft <= 1 ? `Your ${APP_NAME} trial ends tomorrow` : `Your ${APP_NAME} trial ends in ${daysLeft} days`;
  const html = layout(
    "Your trial is ending soon",
    `<p>Hi ${escapeHtml(name || "there")}, your free trial ${daysLeft <= 1 ? "ends tomorrow" : `ends in ${daysLeft} days`}.</p>
     <p><strong>Your quizzes, questions, and results are completely safe</strong> — nothing gets deleted. After your trial you can keep using ${APP_NAME} free with basic features, or upgrade any time to keep premium features.</p>
     ${button("Check Your Status", APP_URL)}`,
  );
  return { subject, html };
}

function licensePurchasedEmail(name: string, planLabel: string, expiresAt: string | undefined) {
  const expiryLabel = expiresAt
    ? new Date(expiresAt).toLocaleDateString("en-US", { year: "numeric", month: "long", day: "numeric" })
    : null;
  const subject = `You're premium! ${APP_NAME} license activated`;
  const html = layout(
    "Payment received 🎉",
    `<p>Hi ${escapeHtml(name || "there")}, your <strong>${escapeHtml(planLabel)}</strong> plan is now active.</p>
     ${expiryLabel ? `<p>Your license is valid until <strong>${expiryLabel}</strong>. Enjoy unlimited quizzes and premium support.</p>` : ""}
     ${button("View License", APP_URL)}`,
  );
  return { subject, html };
}

interface WeeklyQuizStat {
  title: string;
  participantCount: number;
  topScorer: { name: string; score: number } | null;
  lowScorer: { name: string; score: number } | null;
  questions: { text: string; correctPercent: number; totalAttempts: number }[];
}

function weeklySummaryEmail(name: string, weekLabel: string, quizzes: WeeklyQuizStat[]) {
  const subject = `Your ${APP_NAME} weekly summary — ${weekLabel}`;
  const totalParticipants = quizzes.reduce((sum, q) => sum + q.participantCount, 0);

  const quizBlocks = quizzes
    .map((q) => {
      const questionRows = q.questions
        .slice(0, 5)
        .map(
          (question) =>
            `<tr>
               <td style="padding:6px 0;border-bottom:1px solid #F0F0F5;font-size:13px;color:#374151;">${escapeHtml(question.text)}</td>
               <td style="padding:6px 0;border-bottom:1px solid #F0F0F5;font-size:13px;color:${question.correctPercent < 50 ? "#EF4444" : "#16A34A"};text-align:right;white-space:nowrap;">${question.correctPercent}% correct</td>
             </tr>`,
        )
        .join("");

      return `
      <div style="margin-top:18px;padding:16px;border:1px solid #E5E7EB;border-radius:12px;">
        <div style="font-weight:700;font-size:15px;color:#111827;">${escapeHtml(q.title)}</div>
        <div style="color:#6B7280;font-size:12px;margin-top:2px;">${q.participantCount} submission${q.participantCount === 1 ? "" : "s"} this week</div>
        ${q.topScorer ? `<div style="margin-top:10px;font-size:13px;">🏆 Top score: <strong>${escapeHtml(q.topScorer.name)}</strong> — ${q.topScorer.score}%</div>` : ""}
        ${q.lowScorer ? `<div style="margin-top:4px;font-size:13px;">⚠️ Lowest score: <strong>${escapeHtml(q.lowScorer.name)}</strong> — ${q.lowScorer.score}%</div>` : ""}
        ${questionRows ? `<table style="width:100%;border-collapse:collapse;margin-top:12px;">${questionRows}</table>` : ""}
      </div>`;
    })
    .join("");

  const html = layout(
    `Weekly Summary — ${weekLabel}`,
    `<p>Hi ${escapeHtml(name || "there")}, here's what happened across your quizzes this week: <strong>${totalParticipants}</strong> total submission${totalParticipants === 1 ? "" : "s"}.</p>
     ${quizBlocks || "<p>No submissions this week.</p>"}
     ${button("Open Dashboard", APP_URL)}`,
  );
  return { subject, html };
}

// ---- Brevo --------------------------------------------------------------------------------

async function sendViaBrevo(to: { email: string; name?: string }, subject: string, htmlContent: string) {
  const apiKey = Deno.env.get("BREVO_API_KEY");
  if (!apiKey) {
    throw new Error("Brevo is not configured (BREVO_API_KEY).");
  }

  const res = await fetch("https://api.brevo.com/v3/smtp/email", {
    method: "POST",
    headers: {
      "api-key": apiKey,
      "Content-Type": "application/json",
      "Accept": "application/json",
    },
    body: JSON.stringify({
      sender: { email: SENDER_EMAIL, name: SENDER_NAME },
      to: [{ email: to.email, name: to.name || undefined }],
      subject,
      htmlContent,
    }),
  });

  if (!res.ok) {
    const errorBody = await res.text();
    throw new Error(`Brevo send failed (${res.status}): ${errorBody}`);
  }
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  try {
    const body = await req.json().catch(() => null);
    if (!body) return jsonResponse({ success: false, error: "Invalid request body." }, 400);

    // A native Supabase Database Webhook on `profiles` INSERT — same shape notify-quiz-submission
    // already relies on for quiz_responses — is treated as an implicit "welcome" trigger.
    const isProfileInsertWebhook = body.type === "INSERT" && body.table === "profiles" && body.record;
    const emailType = isProfileInsertWebhook ? "welcome" : body.emailType;

    let to: { email: string; name?: string } | null = null;
    let subject = "";
    let html = "";

    if (emailType === "welcome") {
      const email = isProfileInsertWebhook ? body.record.email : body.email;
      const name = isProfileInsertWebhook ? body.record.name : body.name;
      if (!email) return jsonResponse({ success: true, sent: false, reason: "no email on record" });
      to = { email, name };
      ({ subject, html } = welcomeEmail(name ?? ""));
    } else if (emailType === "trial_ending") {
      if (!body.email) return jsonResponse({ success: false, error: "email required" }, 400);
      to = { email: body.email, name: body.name };
      ({ subject, html } = trialEndingEmail(body.name ?? "", Number(body.daysLeft ?? 1)));
    } else if (emailType === "license_purchased") {
      if (!body.email) return jsonResponse({ success: false, error: "email required" }, 400);
      to = { email: body.email, name: body.name };
      ({ subject, html } = licensePurchasedEmail(body.name ?? "", body.planLabel ?? "Premium", body.expiresAt));
    } else if (emailType === "weekly_summary") {
      if (!body.email) return jsonResponse({ success: false, error: "email required" }, 400);
      to = { email: body.email, name: body.name };
      ({ subject, html } = weeklySummaryEmail(body.name ?? "", body.weekLabel ?? "", body.quizzes ?? []));
    } else {
      return jsonResponse({ success: false, error: `Unknown emailType: ${emailType}` }, 400);
    }

    await sendViaBrevo(to!, subject, html);
    return jsonResponse({ success: true });
  } catch (error) {
    console.error("send-email error:", error);
    return jsonResponse({ success: false, error: error instanceof Error ? error.message : "Something went wrong." }, 500);
  }
});
