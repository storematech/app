// supabase/functions/check-email-exists/index.ts
//
// Tells the client whether an account already exists for an email, so the auth screen can show
// "enter your password" (sign in) vs "create a password" (sign up) without making the user pick
// one manually. Uses the service-role key to bypass RLS for this one lookup — the anon key
// intentionally can't query `profiles` by arbitrary email itself.
//
// Request body:  { "email": string }
// Response body: { "success": true, "exists": boolean } | { "success": false, "error": string }

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

    const body = await req.json().catch(() => null);
    const email = typeof body?.email === "string" ? body.email.trim().toLowerCase() : "";
    if (!email || !email.includes("@")) {
      return jsonResponse({ success: false, error: "Enter a valid email." }, 400);
    }

    const supabase = createClient(supabaseUrl, serviceRoleKey);
    const { data, error } = await supabase
      .from("profiles")
      .select("id")
      .ilike("email", email)
      .limit(1)
      .maybeSingle();

    if (error) {
      console.error("check-email-exists lookup failed:", error);
      return jsonResponse({ success: false, error: "Couldn't check that email right now." }, 500);
    }

    return jsonResponse({ success: true, exists: data != null });
  } catch (error) {
    console.error("check-email-exists error:", error);
    return jsonResponse({ success: false, error: "Something went wrong." }, 500);
  }
});
