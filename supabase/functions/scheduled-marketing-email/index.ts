// supabase/functions/scheduled-marketing-email/index.ts
//
// Runs on an hourly schedule (Supabase Dashboard > Edge Functions > scheduled-marketing-email >
// Cron, e.g. "0 * * * *" for the top of every hour). Each run sends exactly one cold-outreach
// proposal email to the oldest still-'pending' row in marketing_prospects (see
// supabase/sql/marketing_prospects.sql) — one email per hour, never more, so this stays well
// inside Brevo's sending limits and reads as a slow, deliberate campaign rather than a blast.
//
// Never talks to Brevo directly — delegates the actual send to send-email
// (emailType: "marketing_proposal"), same as scheduled-emails does for trial/weekly emails, so
// send-email stays the only function that owns Brevo API access. On success the prospect row is
// flipped to status = 'sent'; on failure it's flipped to 'failed' with the error recorded, so it's
// never retried automatically (reset it back to 'pending' in the table to retry manually) and
// failures stay visible without checking function logs.
//
// Response body: { "success": true, "sent": boolean, "reason"?: string, "prospect"?: { id, email } }
//              | { "success": false, "error": string, "prospect"?: { id, email } }

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

    const admin = createClient(supabaseUrl, serviceRoleKey);

    const { data: prospect, error: fetchError } = await admin
      .from("marketing_prospects")
      .select("id, name, email")
      .eq("status", "pending")
      .order("created_at", { ascending: true })
      .limit(1)
      .maybeSingle();

    if (fetchError) {
      console.error("marketing_prospects fetch failed:", fetchError);
      return jsonResponse({ success: false, error: fetchError.message }, 500);
    }
    if (!prospect) {
      return jsonResponse({ success: true, sent: false, reason: "no pending prospects" });
    }

    const sendRes = await fetch(`${supabaseUrl}/functions/v1/send-email`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${serviceRoleKey}`,
        apikey: serviceRoleKey,
      },
      body: JSON.stringify({ emailType: "marketing_proposal", email: prospect.email, name: prospect.name }),
    });
    const sendResult = await sendRes.json().catch(() => ({ success: false, error: "Invalid response from send-email" }));

    if (sendRes.ok && sendResult.success) {
      await admin
        .from("marketing_prospects")
        .update({ status: "sent", sent_at: new Date().toISOString(), error_message: null })
        .eq("id", prospect.id);
      return jsonResponse({ success: true, sent: true, prospect: { id: prospect.id, email: prospect.email } });
    }

    const errorMessage = sendResult?.error ?? `send-email returned ${sendRes.status}`;
    await admin
      .from("marketing_prospects")
      .update({ status: "failed", error_message: errorMessage })
      .eq("id", prospect.id);
    return jsonResponse(
      { success: false, error: errorMessage, prospect: { id: prospect.id, email: prospect.email } },
      502,
    );
  } catch (error) {
    console.error("scheduled-marketing-email error:", error);
    return jsonResponse({ success: false, error: error instanceof Error ? error.message : "Something went wrong." }, 500);
  }
});
