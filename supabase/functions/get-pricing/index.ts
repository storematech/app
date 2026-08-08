// supabase/functions/get-pricing/index.ts
//
// Serves the current premium pricing so it lives in exactly one place — change the numbers
// below and redeploy this function to update pricing across web + Android with no app release.
// Public/read-only: no auth required, no payment processing happens here (that's a separate,
// not-yet-built step — see verify-razorpay-payment on the web side for the eventual pattern).
//
// Response body: {
//   "success": true,
//   "badge": string,
//   "plans": {
//     "india":  { "label": string, "flag": string, "amount": number, "currency": "INR", "amountMinor": number, "interval": "year" },
//     "global": { "label": string, "flag": string, "amount": number, "currency": "USD", "amountMinor": number, "interval": "month" }
//   },
//   "features": string[]
// }

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

// The only numbers that matter — edit these and redeploy to change pricing everywhere.
const PRICING = {
  badge: "Limited Special Offer",
  plans: {
    india: {
      label: "India Premium",
      flag: "🇮🇳",
      amount: 799,
      currency: "INR",
      amountMinor: 79900, // smallest currency unit (paise), for the future payment gateway integration
      interval: "year",
    },
    global: {
      label: "Global Premium",
      flag: "🌍",
      amount: 4,
      currency: "USD",
      amountMinor: 400, // smallest currency unit (cents)
      interval: "month",
    },
  },
  features: [
    "Unlimited Quizzes",
    "Unlimited Question Bank",
    "Unlimited Responses",
    "Premium Support",
    "Mobile Friendly",
  ],
};

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

Deno.serve((req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }
  return jsonResponse({ success: true, ...PRICING });
});
