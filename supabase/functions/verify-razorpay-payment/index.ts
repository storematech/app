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
// Two more checks beyond that:
//   - Underpayment: create-razorpay-order's amount bounds are generous (so legitimate sale
//     discounts fit), so the amount actually paid is checked here against the real plan price for
//     that currency — fetched live from get-pricing (never duplicated as a local constant: pricing
//     changes often, see that function's own header, and a stale hardcoded copy here would either
//     falsely reject real payments after a price bump or under-charge after a price drop) —
//     discounted by whatever `sale_day` percent create-razorpay-order snapshotted into the order's
//     `notes.app_discount_percent` at the moment it was created (see fetchPlanPricingByCurrency).
//     That snapshot — not a fresh `sale_day` lookup here — is deliberate: editing/ending a sale
//     between checkout and payment must never retroactively invalidate a payment that was correct
//     for the price the customer actually saw. Without any of this, paying the minimum Razorpay
//     will accept still bought the full plan interval.
//   - Plan length: the granted interval (30 days, 365 days, ...) is also read live from
//     get-pricing's `interval` field per currency — never hardcoded — so changing a plan from
//     monthly to yearly (or anything else) in get-pricing takes effect here automatically too.
//   - Replay: this function is idempotent per `razorpay_order_id` (see
//     razorpay_payment_idempotency.sql) — resubmitting the same already-processed order/payment/
//     signature returns success without extending the license a second time.
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

/** [interval] is whatever get-pricing's PRICING.plans.*.interval currently says ("year", "month",
 *  ...) — read live per currency in fetchPlanPricingByCurrency, never inferred from currency here.
 *  Unrecognized values fail closed to the shorter 30-day grant rather than silently defaulting to
 *  a year, since under-granting is the safer direction to be wrong in. */
function planIntervalMs(interval: string): number {
  const DAY_MS = 24 * 60 * 60 * 1000;
  return interval === "year" ? 365 * DAY_MS : 30 * DAY_MS;
}

interface PlanPricing {
  amountMinor: number;
  interval: string;
}

/**
 * Live full (non-sale) price AND plan length per currency — fetched from get-pricing itself rather
 * than duplicated as local constants. Both change often (see get-pricing's own header: "edit these
 * and redeploy to change pricing everywhere"), and a hardcoded copy in this function would silently
 * drift out of sync every time — a price bump could reject real payments, a price cut could accept
 * underpayment, and a plan switched from monthly to yearly in get-pricing wouldn't actually grant a
 * year here. get-pricing is the only place either needs to be edited for it to take effect
 * everywhere, including here.
 */
async function fetchPlanPricingByCurrency(supabaseUrl: string, serviceRoleKey: string): Promise<Record<string, PlanPricing> | null> {
  try {
    const res = await fetch(`${supabaseUrl}/functions/v1/get-pricing`, {
      headers: { Authorization: `Bearer ${serviceRoleKey}`, apikey: serviceRoleKey },
    });
    if (!res.ok) {
      console.error("get-pricing fetch failed:", res.status);
      return null;
    }
    const data = await res.json();
    if (!data?.success || typeof data.plans !== "object" || data.plans === null) {
      console.error("get-pricing returned an unexpected shape:", data);
      return null;
    }
    const byCurrency: Record<string, PlanPricing> = {};
    for (const plan of Object.values(data.plans) as Array<{ currency?: unknown; amountMinor?: unknown; interval?: unknown }>) {
      if (typeof plan.currency === "string" && typeof plan.amountMinor === "number" && typeof plan.interval === "string") {
        byCurrency[plan.currency] = { amountMinor: plan.amountMinor, interval: plan.interval };
      }
    }
    return byCurrency;
  } catch (err) {
    console.error("Failed to fetch get-pricing for payment validation:", err);
    return null;
  }
}

/** Reads back the discount percent create-razorpay-order snapshotted into `notes` at order-creation
 *  time (see that function's header) — never re-derived from `sale_day` here, so nothing edited in
 *  that table after checkout can affect an already-placed order. Missing/malformed notes (e.g. an
 *  order that predates this snapshot going live) default to 0 — full price, never a reason to
 *  accept less. Clamped to [0, 100] since `notes` values are plain strings Razorpay just echoes
 *  back verbatim — nothing stops a malformed one, though only our own server ever writes it. */
function readSnapshottedDiscountPercent(order: { notes?: Record<string, unknown> }): number {
  const raw = order.notes?.app_discount_percent;
  const parsed = typeof raw === "string" ? Number(raw) : NaN;
  if (!Number.isFinite(parsed)) return 0;
  return Math.min(100, Math.max(0, Math.round(parsed)));
}

/** Minimum amount (smallest currency unit) that must have been paid for [currency] to earn a plan,
 *  honoring the discount snapshotted on the order itself. Truncates the same way the Android
 *  client's own discount math does (PricingViewModel.discountedAmountMinor). */
function getExpectedMinAmount(
  planPricingByCurrency: Record<string, PlanPricing>,
  currency: string,
  discountPercent: number,
): number | null {
  const fullPrice = planPricingByCurrency[currency]?.amountMinor;
  if (fullPrice === undefined) return null; // unrecognized currency — caller rejects
  return Math.floor((fullPrice * (100 - discountPercent)) / 100);
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

    const planPricingByCurrency = await fetchPlanPricingByCurrency(supabaseUrl, serviceRoleKey);
    if (!planPricingByCurrency) {
      return jsonResponse({ success: false, error: "Payment could not be verified." }, 502);
    }
    const discountPercent = readSnapshottedDiscountPercent(order);
    const expectedMinAmount = getExpectedMinAmount(planPricingByCurrency, currency, discountPercent);
    if (expectedMinAmount === null || amountMinor < expectedMinAmount) {
      console.error(`Underpayment on order ${orderId}: paid ${amountMinor} ${currency}, expected at least ${expectedMinAmount}`);
      return jsonResponse({ success: false, error: "Payment amount does not match the plan price." }, 400);
    }

    // Idempotency claim: razorpay_order_id is the primary key, so only the first request for a
    // given order can insert successfully — a resubmitted (already-processed) order/payment/
    // signature hits the unique violation below and is treated as already-granted, not re-granted.
    const { error: claimError } = await supabase
      .from("processed_razorpay_payments")
      .insert({ razorpay_order_id: orderId, razorpay_payment_id: paymentId, user_id: userId });
    if (claimError) {
      if (claimError.code === "23505") {
        return jsonResponse({ success: true });
      }
      console.error("Failed to claim Razorpay order as processed:", claimError);
      return jsonResponse({ success: false, error: "Something went wrong verifying the payment." }, 500);
    }

    const { data: existingProfile } = await supabase
      .from("profiles")
      .select("email, name, license_expired_date")
      .eq("id", userId)
      .single();

    const now = Date.now();
    const currentExpiry = existingProfile?.license_expired_date
      ? new Date(existingProfile.license_expired_date).getTime()
      : now;
    // Extends from the later of "now" or the existing expiry, so renewing before expiry
    // stacks on top of remaining time instead of discarding it. planPricingByCurrency[currency] is
    // guaranteed present here — the underpayment check above already returned early otherwise.
    const interval = planPricingByCurrency[currency]?.interval ?? "month";
    const newExpiry = new Date(Math.max(now, currentExpiry) + planIntervalMs(interval)).toISOString();

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

    // Best-effort — a Brevo/email hiccup should never fail an already-verified, already-activated
    // payment. send-email owns all Brevo access; this function never talks to Brevo directly.
    if (existingProfile?.email) {
      try {
        await fetch(`${supabaseUrl}/functions/v1/send-email`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${serviceRoleKey}`,
            apikey: serviceRoleKey,
          },
          body: JSON.stringify({
            emailType: "license_purchased",
            email: existingProfile.email,
            name: existingProfile.name,
            planLabel: currency === "INR" ? "India Premium" : "Global Premium",
            expiresAt: newExpiry,
          }),
        });
      } catch (emailError) {
        console.error("Failed to send license-purchased email:", emailError);
      }
    }

    return jsonResponse({ success: true });
  } catch (error) {
    console.error("verify-razorpay-payment error:", error);
    return jsonResponse({ success: false, error: "Something went wrong verifying the payment." }, 500);
  }
});
