// supabase/functions/notify-quiz-submission/index.ts
//
// Called by a Supabase Database Webhook on INSERT into quiz_responses (configured in the
// dashboard, not in code — see the setup notes handed to the user). Looks up the quiz's creator's
// stored FCM device token and pushes a "new submission" notification via FCM's HTTP v1 API.
//
// FCM HTTP v1 requires a service-account-signed OAuth2 access token, not a flat server key (the
// old Legacy API is disabled for Firebase projects created after June 2024) — getAccessToken()
// below hand-signs that JWT with Deno's native Web Crypto so no extra npm dependency is needed,
// matching the rest of this project's edge functions.
//
// Webhook payload: { type: "INSERT", table: "quiz_responses", record: {...} }
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
    const record = payload?.record;
    if (!record || record.completed !== true) {
      // Webhook fires on every insert, not just finished attempts — quietly no-op the rest.
      return jsonResponse({ success: true, sent: false, reason: "not a completed response" });
    }

    const supabase = createClient(supabaseUrl, serviceRoleKey);

    const { data: quiz, error: quizError } = await supabase
      .from("quizzes")
      .select("title, created_by")
      .eq("id", record.quiz_id)
      .maybeSingle();
    if (quizError || !quiz?.created_by) {
      return jsonResponse({ success: true, sent: false, reason: "quiz or creator not found" });
    }

    const { data: creatorProfile, error: profileError } = await supabase
      .from("profiles")
      .select("fcm_token")
      .eq("id", quiz.created_by)
      .maybeSingle();
    if (profileError || !creatorProfile?.fcm_token) {
      return jsonResponse({ success: true, sent: false, reason: "creator has no device token" });
    }

    const accessToken = await getAccessToken(fcmClientEmail, fcmPrivateKey);
    const submitterName = (record.user_name as string | null)?.trim() || record.user_email || "Someone";
    const score = record.score ?? 0;

    const fcmResponse = await fetch(`https://fcm.googleapis.com/v1/projects/${fcmProjectId}/messages:send`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify({
        message: {
          token: creatorProfile.fcm_token,
          data: {
            title: `New submission on ${quiz.title}`,
            body: `${submitterName} scored ${score}%`,
            response_id: String(record.id),
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
    console.error("notify-quiz-submission error:", error);
    return jsonResponse({ success: false, error: "Something went wrong." }, 500);
  }
});
