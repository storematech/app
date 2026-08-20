// supabase/functions/scheduled-push/index.ts
//
// The lifecycle push-notification campaign: two sends timed off signup itself (+30min, +8h), then
// once daily at a fixed time for Day 1-10, split by whether the account has created a quiz yet
// (Day 1-5) or by how long the trial's been expired (Day 7-10), plus a single Day 6 "trial ends
// tomorrow" blast at noon. Every send is logged to `push_notification_log` (user_id +
// notification_key, unique) so re-running or overlapping cron ticks can never double-send.
//
// Needs THREE separate Supabase Dashboard cron schedules pointing at this same function, each
// POSTing a different body so this one function knows which day-window math to run:
//   1. "*/15 * * * *"        body: { "mode": "relative" }        — the +30min/+8h sends
//   2. "0 3 * * *"  (8:30am IST) body: { "mode": "daily" }       — Day 1,2,3,4,5,7,8,9,10
//   3. "30 6 * * *" (12:00pm IST) body: { "mode": "noon" }       — Day 6 only
// (IST = UTC+5:30, hence 3:00/6:30 UTC — same offset already used by scheduled-emails' 8:30am cron.)
//
// Never sent to accounts already on a paid plan (user_type in PREMIUM_USER_TYPES) — every message
// here is either an activation nudge or a trial-conversion nudge, both moot once someone's paying.

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

const DAY_MS = 24 * 60 * 60 * 1000;
const PREMIUM_USER_TYPES = ["starter", "school", "school pro"]; // mirrors scheduled-emails/index.ts

interface PushMessage {
  title: string;
  body: string;
}

// ---- Message copy — the one place to edit this campaign's text ------------------------------
// TODO marks the messages that were drafted as placeholders (exact copy wasn't specified) — edit
// freely, the scheduling logic above never needs to change when this text does.

const RELATIVE_MESSAGES: { key: string; offsetMs: number; message: PushMessage }[] = [
  {
    key: "signup_30min",
    offsetMs: 30 * 60 * 1000,
    message: {
      title: "There's More to YUNO LMS",
      body: "Your YUNO LMS can do a lot more than you think! 🚀 Create smarter quizzes, engage learners, track their progress, discover powerful teaching tools and make learning more fun — all in one place.",
    },
  },
  {
    key: "signup_8h",
    offsetMs: 8 * 60 * 60 * 1000,
    message: {
      title: "🚀 Let's Make Learning Fun!",
      body: "Turn your ideas into engaging quizzes, challenge your learners and see their performance come to life with YUNO LMS. ✨",
    },
  },
];

/** Day 1-5, 8:30am IST — split by whether the account has created at least one quiz yet. */
const DAILY_ACTIVATION_MESSAGES: Record<number, { noQuiz: PushMessage; hasQuiz: PushMessage }> = {
  1: {
    noQuiz: {
      title: "🚀 Your First Quiz Is Waiting",
      body: "Got an idea for a quiz? Turn it into an engaging quiz in just a few clicks with YUNO LMS. Create your first one today!",
    },
    hasQuiz: {
      title: "🎯 Your Quiz Is Ready — NEXT Step Pending?",
      body: "Share your quiz with learners, collect responses and see their performance. Your next step is just a click away! 🚀",
    },
  },
  // TODO: placeholder copy — exact text for Day 2-5 wasn't specified, edit freely.
  2: {
    noQuiz: {
      title: "⏳ Still Haven't Made a Quiz?",
      body: "It only takes 2 minutes to create your first quiz on YUNO LMS. Your learners are waiting — let's get started! 📝",
    },
    hasQuiz: {
      title: "📊 Check Your Quiz Results",
      body: "See how your learners are doing! Open YUNO LMS to view responses and track performance in real time. 📈",
    },
  },
  3: {
    noQuiz: {
      title: "💡 Need Quiz Ideas?",
      body: "Not sure where to start? Try the AI Quiz Generator — describe a topic and get a ready-to-share quiz in seconds. ✨",
    },
    hasQuiz: {
      title: "🔥 Keep the Momentum Going",
      body: "Great start! Create another quiz or explore Tools like Polls and Feedback Forms to engage your learners further.",
    },
  },
  4: {
    noQuiz: {
      title: "🎓 Teachers Are Already Creating on YUNO LMS",
      body: "Join them today — build your first quiz and start engaging learners in minutes.",
    },
    hasQuiz: {
      title: "👀 Who's Been Viewing Your Quiz?",
      body: "Check your Leaderboard and Responses to see who's taking part — and celebrate your top performers!",
    },
  },
  5: {
    noQuiz: {
      title: "⚡ A Few Days Left to Explore Your Free Trial",
      body: "Your free trial won't last forever. Create your first quiz now and see what YUNO LMS can do for you.",
    },
    hasQuiz: {
      title: "🌟 Your Learners Are Waiting",
      body: "Share your quiz link or QR code today and start collecting responses before your trial ends.",
    },
  },
};

/** Day 6, 12:00pm IST — one message, everyone whose trial is about to expire. */
const NOON_TRIAL_ENDING_MESSAGE: PushMessage = {
  title: "⏰ Your Trial Ends Tomorrow!",
  body: "Your quizzes, questions and results are completely safe — nothing gets deleted. Upgrade today to keep using every YUNO LMS feature without interruption.",
};

/** Day 7-10, 8:30am IST — trial already expired, one message per day. */
const DAILY_EXPIRED_MESSAGES: Record<number, PushMessage> = {
  7: {
    title: "😔 Your Trial Has Ended",
    body: "Your free trial has expired, but your quizzes and data are safe. Upgrade now to keep creating and tracking with YUNO LMS.",
  },
  // TODO: placeholder copy — exact text for Day 8-10 wasn't specified, edit freely.
  8: {
    title: "🔓 Unlock YUNO LMS Again",
    body: "Pick up right where you left off. Upgrade now and continue creating quizzes, tracking learners and growing your classroom.",
  },
  9: {
    title: "🎁 A Reminder Before You Go",
    body: "Your quizzes and results are still waiting for you. Upgrade today to get back full access to YUNO LMS.",
  },
  10: {
    title: "⌛ Last Reminder — Don't Miss Out",
    body: "This is your final reminder. Upgrade now to keep your quizzes active and your learners engaged on YUNO LMS.",
  },
};

// ---- FCM (identical approach to notify-quiz-submission/index.ts) ------------------------------

function base64url(bytes: Uint8Array): string {
  let str = "";
  bytes.forEach((b) => (str += String.fromCharCode(b)));
  return btoa(str).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

async function importPrivateKey(pem: string): Promise<CryptoKey> {
  const pemContents = pem
    .replace(/\\n/g, "\n")
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s/g, "");
  const binaryDer = Uint8Array.from(atob(pemContents), (c) => c.charCodeAt(0));
  return crypto.subtle.importKey(
    "pkcs8",
    binaryDer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
}

async function getAccessToken(clientEmail: string, privateKeyPem: string): Promise<string> {
  const encoder = new TextEncoder();
  const now = Math.floor(Date.now() / 1000);

  const headerB64 = base64url(encoder.encode(JSON.stringify({ alg: "RS256", typ: "JWT" })));
  const claimB64 = base64url(
    encoder.encode(
      JSON.stringify({
        iss: clientEmail,
        scope: "https://www.googleapis.com/auth/firebase.messaging",
        aud: "https://oauth2.googleapis.com/token",
        iat: now,
        exp: now + 3600,
      }),
    ),
  );
  const signingInput = `${headerB64}.${claimB64}`;

  const key = await importPrivateKey(privateKeyPem);
  const signature = await crypto.subtle.sign({ name: "RSASSA-PKCS1-v1_5" }, key, encoder.encode(signingInput));
  const jwt = `${signingInput}.${base64url(new Uint8Array(signature))}`;

  const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });
  const tokenJson = await tokenResponse.json();
  if (!tokenResponse.ok) {
    throw new Error(`FCM token exchange failed: ${JSON.stringify(tokenJson)}`);
  }
  return tokenJson.access_token as string;
}

/** One access token is reused for every send in this invocation — unlike notify-quiz-submission (one send per call), this function can send to thousands of profiles per run. */
async function sendPush(accessToken: string, fcmProjectId: string, fcmToken: string, message: PushMessage): Promise<boolean> {
  const res = await fetch(`https://fcm.googleapis.com/v1/projects/${fcmProjectId}/messages:send`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({
      message: {
        token: fcmToken,
        data: {
          type: "lifecycle",
          title: message.title,
          body: message.body,
        },
        android: { priority: "high" },
      },
    }),
  });
  if (!res.ok) {
    console.error("FCM send failed:", await res.text().catch(() => ""));
  }
  return res.ok;
}

// ---- Eligibility + dispatch --------------------------------------------------------------------

interface EligibleProfile {
  id: string;
  fcm_token: string;
}

/** [start, end) window, both ISO strings — profiles created between [dayNumber] and [dayNumber - 1] days ago, as measured from [now]. Same windowing scheduled-emails' trial reminder already uses, generalized to any day number. */
function dayWindow(now: number, dayNumber: number): { start: string; end: string } {
  return {
    start: new Date(now - dayNumber * DAY_MS).toISOString(),
    end: new Date(now - (dayNumber - 1) * DAY_MS).toISOString(),
  };
}

type RawProfileRow = { id: string; fcm_token: string; user_type: string | null };

function toEligible(rows: RawProfileRow[] | null): EligibleProfile[] {
  return (rows ?? [])
    .filter((p) => !PREMIUM_USER_TYPES.includes((p.user_type ?? "").toLowerCase()))
    .map((p) => ({ id: p.id, fcm_token: p.fcm_token }));
}

/** Profiles created within `[start, end)` — the day-window rules (Day 1-10). */
async function fetchProfilesInWindow(supabase: SupabaseClient, start: string, end: string): Promise<EligibleProfile[]> {
  const { data, error } = await supabase
    .from("profiles")
    .select("id, fcm_token, user_type")
    .not("fcm_token", "is", null)
    .gte("created_at", start)
    .lt("created_at", end);
  if (error) {
    console.error("Failed to fetch profiles in window:", error);
    return [];
  }
  return toEligible(data as RawProfileRow[] | null);
}

/** Profiles created between [cutoff] and [dueBy] (inclusive) — the relative +30min/+8h rules. */
async function fetchProfilesDue(supabase: SupabaseClient, cutoff: string, dueBy: string): Promise<EligibleProfile[]> {
  const { data, error } = await supabase
    .from("profiles")
    .select("id, fcm_token, user_type")
    .not("fcm_token", "is", null)
    .gte("created_at", cutoff)
    .lte("created_at", dueBy);
  if (error) {
    console.error("Failed to fetch due profiles:", error);
    return [];
  }
  return toEligible(data as RawProfileRow[] | null);
}

/** Already-sent (user_id, notification_key) pairs among [profileIds] for this one key. */
async function alreadySent(supabase: SupabaseClient, profileIds: string[], key: string): Promise<Set<string>> {
  if (profileIds.length === 0) return new Set();
  const { data, error } = await supabase
    .from("push_notification_log")
    .select("user_id")
    .eq("notification_key", key)
    .in("user_id", profileIds);
  if (error) {
    console.error("Failed to check push_notification_log:", error);
    return new Set();
  }
  return new Set((data ?? []).map((row) => row.user_id as string));
}

async function logSent(supabase: SupabaseClient, userId: string, key: string): Promise<void> {
  const { error } = await supabase.from("push_notification_log").insert({ user_id: userId, notification_key: key });
  if (error) console.error(`Failed to log push_notification_log for ${key}:`, error);
}

async function sendToEligible(
  supabase: SupabaseClient,
  accessToken: string,
  fcmProjectId: string,
  key: string,
  message: PushMessage,
  profiles: EligibleProfile[],
): Promise<number> {
  const sentAlready = await alreadySent(supabase, profiles.map((p) => p.id), key);
  let sent = 0;
  for (const profile of profiles) {
    if (sentAlready.has(profile.id)) continue;
    const ok = await sendPush(accessToken, fcmProjectId, profile.fcm_token, message);
    if (ok) {
      await logSent(supabase, profile.id, key);
      sent++;
    }
  }
  return sent;
}

/** Splits [profiles] by whether each has created at least one quiz. */
async function partitionByQuizCreated(
  supabase: SupabaseClient,
  profiles: EligibleProfile[],
): Promise<{ withQuiz: EligibleProfile[]; withoutQuiz: EligibleProfile[] }> {
  if (profiles.length === 0) return { withQuiz: [], withoutQuiz: [] };
  const { data, error } = await supabase
    .from("quizzes")
    .select("created_by")
    .in("created_by", profiles.map((p) => p.id));
  if (error) {
    console.error("Failed to check quiz ownership:", error);
    return { withQuiz: [], withoutQuiz: profiles };
  }
  const withQuizIds = new Set((data ?? []).map((row) => row.created_by as string));
  return {
    withQuiz: profiles.filter((p) => withQuizIds.has(p.id)),
    withoutQuiz: profiles.filter((p) => !withQuizIds.has(p.id)),
  };
}

async function runRelative(supabase: SupabaseClient, accessToken: string, fcmProjectId: string, now: number): Promise<Record<string, number>> {
  const results: Record<string, number> = {};
  for (const rule of RELATIVE_MESSAGES) {
    // Bounded to the last 2 days — comfortably wider than the 8h offset — so this doesn't rescan
    // the entire historical profiles table on every 15-minute tick; anything older is either
    // already logged or simply not relevant to a 30min/8h rule.
    const cutoff = new Date(now - 2 * DAY_MS).toISOString();
    const dueBy = new Date(now - rule.offsetMs).toISOString();
    const profiles = await fetchProfilesDue(supabase, cutoff, dueBy);
    results[rule.key] = await sendToEligible(supabase, accessToken, fcmProjectId, rule.key, rule.message, profiles);
  }
  return results;
}

async function runDaily(supabase: SupabaseClient, accessToken: string, fcmProjectId: string, now: number): Promise<Record<string, number>> {
  const results: Record<string, number> = {};

  for (const [dayStr, messages] of Object.entries(DAILY_ACTIVATION_MESSAGES)) {
    const day = Number(dayStr);
    const { start, end } = dayWindow(now, day);
    const profiles = await fetchProfilesInWindow(supabase, start, end);
    const { withQuiz, withoutQuiz } = await partitionByQuizCreated(supabase, profiles);
    results[`day${day}_no_quiz`] = await sendToEligible(supabase, accessToken, fcmProjectId, `day${day}_no_quiz`, messages.noQuiz, withoutQuiz);
    results[`day${day}_has_quiz`] = await sendToEligible(supabase, accessToken, fcmProjectId, `day${day}_has_quiz`, messages.hasQuiz, withQuiz);
  }

  for (const [dayStr, message] of Object.entries(DAILY_EXPIRED_MESSAGES)) {
    const day = Number(dayStr);
    const key = `day${day}_expired`;
    const { start, end } = dayWindow(now, day);
    const profiles = await fetchProfilesInWindow(supabase, start, end);
    results[key] = await sendToEligible(supabase, accessToken, fcmProjectId, key, message, profiles);
  }

  return results;
}

async function runNoon(supabase: SupabaseClient, accessToken: string, fcmProjectId: string, now: number): Promise<Record<string, number>> {
  const key = "day6_trial_ending";
  const { start, end } = dayWindow(now, 6);
  const profiles = await fetchProfilesInWindow(supabase, start, end);
  return { [key]: await sendToEligible(supabase, accessToken, fcmProjectId, key, NOON_TRIAL_ENDING_MESSAGE, profiles) };
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  try {
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    const fcmClientEmail = Deno.env.get("FCM_SERVICE_ACCOUNT_EMAIL");
    const fcmPrivateKey = Deno.env.get("FCM_SERVICE_ACCOUNT_PRIVATE_KEY");
    const fcmProjectId = Deno.env.get("FCM_PROJECT_ID");
    if (!supabaseUrl || !serviceRoleKey || !fcmClientEmail || !fcmPrivateKey || !fcmProjectId) {
      return jsonResponse({ success: false, error: "Server not configured." }, 500);
    }

    const body = await req.json().catch(() => ({}));
    const mode = body?.mode;
    if (mode !== "relative" && mode !== "daily" && mode !== "noon") {
      return jsonResponse({ success: false, error: "mode must be 'relative' | 'daily' | 'noon'." }, 400);
    }

    const supabase = createClient(supabaseUrl, serviceRoleKey);
    const accessToken = await getAccessToken(fcmClientEmail, fcmPrivateKey);
    const now = Date.now();

    const results =
      mode === "relative" ? await runRelative(supabase, accessToken, fcmProjectId, now) :
      mode === "daily" ? await runDaily(supabase, accessToken, fcmProjectId, now) :
      await runNoon(supabase, accessToken, fcmProjectId, now);

    return jsonResponse({ success: true, mode, sent: results });
  } catch (error) {
    console.error("scheduled-push error:", error);
    return jsonResponse({ success: false, error: "Something went wrong." }, 500);
  }
});
