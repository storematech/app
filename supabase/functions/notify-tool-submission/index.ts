// supabase/functions/notify-tool-submission/index.ts
//
// Same push-notification pattern as notify-quiz-submission/index.ts, generalized across all five
// "Tools" (More → Tools) response tables — poll votes, voting ballots, RSVP registrations,
// feedback submissions, and onboarding submissions. Each of those five tables needs its own
// Supabase Database Webhook (configured in the dashboard, not in code) on INSERT, all five
// pointing at THIS one function's URL — the `table` field the webhook payload already includes is
// what routes each call to the right message copy below, so one function covers all five instead
// of five near-identical files.
//
// Webhook payload: { type: "INSERT", table: "poll_votes" | "voting_ballots" | "rsvp_registrations"
//                     | "feedback_submissions" | "onboarding_submissions", record: {...} }
// Response body:   { "success": true, "sent": boolean, "reason"?: string } | { "success": false, "error": string }

import { createClient } from "jsr:@supabase/supabase-js@2";

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

/** Exchanges the service account key for a short-lived (1hr) FCM-scoped OAuth2 access token. */
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

// deno-lint-ignore no-explicit-any
type Row = Record<string, any>;

function submitterLabel(name: string | null | undefined, email: string | null | undefined): string {
  return name?.trim() || email || "Someone";
}

/** One entry per response table this function is wired up to via a Database Webhook. */
interface ToolConfig {
  /** Table the response row's foreign key points at — where `created_by` and the title live. */
  parentTable: string;
  /** FK column on the response row (e.g. `poll_id`) pointing at [parentTable]'s `id`. */
  parentIdColumn: string;
  /** Column on [parentTable] to use as the human-readable name in the notification. */
  parentTitleColumn: string;
  buildMessage: (record: Row, parentTitle: string) => { title: string; body: string };
}

const TOOL_CONFIGS: Record<string, ToolConfig> = {
  poll_votes: {
    parentTable: "polls",
    parentIdColumn: "poll_id",
    parentTitleColumn: "question",
    buildMessage: (record, parentTitle) => ({
      title: `New vote on ${parentTitle}`,
      body: `${submitterLabel(record.voter_name, record.voter_email)} voted`,
    }),
  },
  voting_ballots: {
    parentTable: "voting_campaigns",
    parentIdColumn: "campaign_id",
    parentTitleColumn: "title",
    buildMessage: (record, parentTitle) => ({
      title: `New ballot on ${parentTitle}`,
      body: `${submitterLabel(record.voter_name, record.voter_email)} cast a vote`,
    }),
  },
  rsvp_registrations: {
    parentTable: "rsvp_events",
    parentIdColumn: "event_id",
    parentTitleColumn: "title",
    buildMessage: (record, parentTitle) => {
      const attending = { yes: "Yes", no: "No", maybe: "Maybe" }[record.attending as string] ?? record.attending;
      return {
        title: `New RSVP for ${parentTitle}`,
        body: `${submitterLabel(record.name, record.email)} responded: ${attending}`,
      };
    },
  },
  feedback_submissions: {
    parentTable: "feedback_forms",
    parentIdColumn: "form_id",
    parentTitleColumn: "title",
    buildMessage: (record, parentTitle) => ({
      title: `New feedback on ${parentTitle}`,
      body: `${submitterLabel(record.learner_name, record.learner_email)} submitted feedback`,
    }),
  },
  onboarding_submissions: {
    parentTable: "onboarding_forms",
    parentIdColumn: "form_id",
    parentTitleColumn: "title",
    buildMessage: (record, parentTitle) => ({
      title: `New submission on ${parentTitle}`,
      body: `${submitterLabel(record.name, record.email)} completed the form`,
    }),
  },
};

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

    const payload = await req.json().catch(() => null);
    const table = payload?.table as string | undefined;
    const record = payload?.record as Row | undefined;
    const config = table ? TOOL_CONFIGS[table] : undefined;
    if (!record || !config) {
      return jsonResponse({ success: true, sent: false, reason: "unrecognized table" });
    }

    const supabase = createClient(supabaseUrl, serviceRoleKey);

    const parentId = record[config.parentIdColumn];
    const { data: parent, error: parentError } = await supabase
      .from(config.parentTable)
      .select(`created_by, ${config.parentTitleColumn}`)
      .eq("id", parentId)
      .maybeSingle();
    if (parentError || !parent?.created_by) {
      return jsonResponse({ success: true, sent: false, reason: `${config.parentTable} or creator not found` });
    }

    const { data: creatorProfile, error: profileError } = await supabase
      .from("profiles")
      .select("fcm_token")
      .eq("id", parent.created_by)
      .maybeSingle();
    if (profileError || !creatorProfile?.fcm_token) {
      return jsonResponse({ success: true, sent: false, reason: "creator has no device token" });
    }

    const accessToken = await getAccessToken(fcmClientEmail, fcmPrivateKey);
    const parentTitle = (parent[config.parentTitleColumn] as string | null)?.trim() || "your tool";
    const message = config.buildMessage(record, parentTitle);

    const fcmResponse = await fetch(`https://fcm.googleapis.com/v1/projects/${fcmProjectId}/messages:send`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify({
        message: {
          token: creatorProfile.fcm_token,
          // No `response_id` here (unlike notify-quiz-submission) — the client's tap handler
          // deep-links that id straight into the quiz response detail screen (see MainActivity's
          // EXTRA_RESPONSE_ID/pendingResponseId), which a poll/ballot/registration/submission id
          // doesn't match. Omitting it just opens the app normally on tap instead of a broken deep link.
          data: {
            // Routes to its own Android notification channel client-side (see QuizFcmService.kt) —
            // kept distinct from plain quiz-submission pushes so it doesn't show up mislabeled
            // under a channel described as being about quiz submissions.
            type: "tool_submission",
            title: message.title,
            body: message.body,
          },
          android: { priority: "high" },
        },
      }),
    });

    if (!fcmResponse.ok) {
      const errorBody = await fcmResponse.text();
      console.error("FCM send failed:", errorBody);
      return jsonResponse({ success: false, error: "Push send failed." }, 502);
    }

    return jsonResponse({ success: true, sent: true });
  } catch (error) {
    console.error("notify-tool-submission error:", error);
    return jsonResponse({ success: false, error: "Something went wrong." }, 500);
  }
});
