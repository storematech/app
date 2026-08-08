// supabase/functions/verify-razorpay-payment/index.ts
//
// Verifies a completed Razorpay checkout server-side — HMAC-SHA256(order_id|payment_id) signed
// with RAZORPAY_KEY_SECRET, which never leaves this function — and only then activates the
// premium plan on the user's `profiles` row (user_type + license_expired_date), using the
// service-role key to bypass RLS for that one write.
//
// The charged amount/currency used to decide the plan is re-fetched from Razorpay's Orders API,
// never taken from the request body: the signature only proves order_id/payment_id are linked,
// not what the client *claims* was paid, so trusting a client-supplied amount here would let a
// tampered request grant a plan it didn't pay for.
//
// Request body:  {
//   "razorpay_order_id": string,
//   "razorpay_payment_id": string,
//   "razorpay_signature": string,
//   "user_id": string,
//   "country"?: string
// }
// Response body: { "success": true } | { "success": false, "error": string }

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

async function hmacSha256Hex(secret: string, message: string): Promise<string> {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signatureBytes = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(message));
  return Array.from(new Uint8Array(signatureBytes))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

// The single self-serve plan is annual for INR (₹799/year) and monthly for USD ($4/month) —
// mirrors get-pricing's PRICING.plans. If more plans/currencies are added later, this needs
// to look the interval up from the order/plan instead of inferring it from currency alone.
function planIntervalMs(currency: string): number {
  const DAY_MS = 24 * 60 * 60 * 1000;
  return currency === "INR" ? 365 * DAY_MS : 30 * DAY_MS;
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  try {
    const keyId = Deno.env.get("RAZORPAY_KEY_ID");
    const keySecret = Deno.env.get("RAZORPAY_KEY_SECRET");
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (!keyId || !keySecret || !supabaseUrl || !serviceRoleKey) {
      return jsonResponse({ success: false, error: "Payments are not configured on the server." }, 500);
    }

    const body = await req.json().catch(() => null);
    const orderId = typeof body?.razorpay_order_id === "string" ? body.razorpay_order_id : "";
    const paymentId = typeof body?.razorpay_payment_id === "string" ? body.razorpay_payment_id : "";
    const signature = typeof body?.razorpay_signature === "string" ? body.razorpay_signature : "";
    const userId = typeof body?.user_id === "string" ? body.user_id : "";

    if (!orderId || !paymentId || !signature || !userId) {
      return jsonResponse({ success: false, error: "Missing payment details." }, 400);
    }

    const expectedSignature = await hmacSha256Hex(keySecret, `${orderId}|${paymentId}`);
    if (expectedSignature !== signature) {
      console.error("Razorpay signature mismatch for order", orderId);
      return jsonResponse({ success: false, error: "Payment could not be verified." }, 400);
    }

    const orderRes = await fetch(`https://api.razorpay.com/v1/orders/${orderId}`, {
      headers: { Authorization: `Basic ${btoa(`${keyId}:${keySecret}`)}` },
    });
    if (!orderRes.ok) {
      console.error("Couldn't re-fetch Razorpay order", orderId, orderRes.status);
      return jsonResponse({ success: false, error: "Payment could not be verified." }, 502);
    }
    const order = await orderRes.json();
    if (order.status !== "paid") {
      return jsonResponse({ success: false, error: "Payment has not completed yet." }, 400);
    }

    const amountMinor = Number(order.amount);
    const currency = String(order.currency ?? "");

    const supabase = createClient(supabaseUrl, serviceRoleKey);

    const { data: existingProfile } = await supabase
      .from("profiles")
      .select("license_expired_date")
      .eq("id", userId)
      .single();

    const now = Date.now();
    const currentExpiry = existingProfile?.license_expired_date
      ? new Date(existingProfile.license_expired_date).getTime()
      : now;
    // Extends from the later of "now" or the existing expiry, so renewing before expiry
    // stacks on top of remaining time instead of discarding it.
    const newExpiry = new Date(Math.max(now, currentExpiry) + planIntervalMs(currency)).toISOString();

    const { error: updateError } = await supabase
      .from("profiles")
      .update({ user_type: "starter", license_expired_date: newExpiry })
      .eq("id", userId);

    if (updateError) {
      console.error("Failed to activate premium for user", userId, updateError);
      return jsonResponse({ success: false, error: "Payment verified, but activation failed. Contact support." }, 500);
    }

    const { error: paymentLogError } = await supabase.from("payments").insert({
      user_id: userId,
      amount: amountMinor,
      currency,
      payment_status: "success",
    });
    if (paymentLogError) {
      console.error("Failed to log successful payment:", paymentLogError);
    }

    return jsonResponse({ success: true });
  } catch (error) {
    console.error("verify-razorpay-payment error:", error);
    return jsonResponse({ success: false, error: "Something went wrong verifying the payment." }, 500);
  }
});
