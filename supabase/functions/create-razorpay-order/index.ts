// supabase/functions/create-razorpay-order/index.ts
//
// Creates a Razorpay order server-side — the Razorpay secret key never ships to any client,
// it only ever lives here (as a server-side secret, RAZORPAY_KEY_SECRET). The client then
// opens Razorpay's Checkout SDK with this order's id; the actual charge is only confirmed
// later, server-side, by verify-razorpay-payment.
//
// Snapshots the currently-active `sale_day` discount percent (if any) into the order's `notes` —
// Razorpay stores `notes` immutably on the order and returns it back unchanged on every later
// fetch, so verify-razorpay-payment can read back "what discount applied when this order was
// created" regardless of anything edited in `sale_day` afterward (ending/changing a sale mid-
// checkout would otherwise wrongly reject an already-correct payment made at the old price — see
// that function's header for the full reasoning). `notes` is set purely from our own server-side
// sale_day lookup here, never from client input, so this can't be spoofed by a tampered request.
//
// Request body:  { "amount": number, "currency": "INR" | "USD" }   // amount in the smallest unit (paise/cents)
// Response body: { "success": true, "id": string, "amount": number, "currency": string }
//              | { "success": false, "error": string }

import { createClient } from "jsr:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const RAZORPAY_ORDERS_URL = "https://api.razorpay.com/v1/orders";

// Server-side sanity bounds, independent of whatever get-pricing currently advertises — so a
// tampered/malformed client request can't create a wildly wrong-sized order.
const MIN_AMOUNT_MINOR = 100; // smallest order Razorpay itself will accept
const MAX_AMOUNT_MINOR = 100_000_00; // hard cap so a bad request can't run away in size
const ALLOWED_CURRENCIES = new Set(["INR", "USD"]);

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

/** Largest `discount` (0-100) from a `sale_day` row whose [started_at, end_at] window covers right
 *  now — the moment the order is being created. 0 (and logged, not thrown) on any lookup failure,
 *  so a sale_day hiccup degrades to "full price" rather than blocking checkout entirely. */
async function getActiveDiscountPercentNow(supabaseUrl: string, serviceRoleKey: string): Promise<number> {
  try {
    const supabase = createClient(supabaseUrl, serviceRoleKey);
    const nowIso = new Date().toISOString();
    const { data: sales, error } = await supabase
      .from("sale_day")
      .select("started_at, end_at, discount")
      .lte("started_at", nowIso)
      .gte("end_at", nowIso);
    if (error) {
      console.error("Failed to check sale_day when creating order:", error);
      return 0;
    }
    return (sales ?? []).reduce((max: number, sale: { discount: number | null }) => Math.max(max, sale.discount ?? 0), 0);
  } catch (err) {
    console.error("Failed to check sale_day when creating order:", err);
    return 0;
  }
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
    const amount = Number(body?.amount);
    const currency = typeof body?.currency === "string" ? body.currency.toUpperCase() : "";

    if (!Number.isFinite(amount) || amount < MIN_AMOUNT_MINOR || amount > MAX_AMOUNT_MINOR) {
      return jsonResponse({ success: false, error: "Invalid amount." }, 400);
    }
    if (!ALLOWED_CURRENCIES.has(currency)) {
      return jsonResponse({ success: false, error: "Invalid currency." }, 400);
    }

    const discountPercent = await getActiveDiscountPercentNow(supabaseUrl, serviceRoleKey);

    const basicAuth = btoa(`${keyId}:${keySecret}`);
    const razorpayRes = await fetch(RAZORPAY_ORDERS_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Basic ${basicAuth}`,
      },
      body: JSON.stringify({
        amount: Math.round(amount),
        currency,
        receipt: `receipt_${Date.now()}`,
        notes: { app_discount_percent: String(discountPercent) },
      }),
    });

    if (!razorpayRes.ok) {
      const errText = await razorpayRes.text().catch(() => "");
      console.error("Razorpay order creation failed:", razorpayRes.status, errText);
      return jsonResponse({ success: false, error: "Couldn't start checkout. Please try again." }, 502);
    }

    const order = await razorpayRes.json();
    return jsonResponse({
      success: true,
      id: order.id,
      amount: order.amount,
      currency: order.currency,
    });
  } catch (error) {
    console.error("create-razorpay-order error:", error);
    return jsonResponse({ success: false, error: "Something went wrong starting checkout." }, 500);
  }
});
