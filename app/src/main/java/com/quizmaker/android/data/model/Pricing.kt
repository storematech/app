package com.quizmaker.android.data.model

/** One region's premium plan, as served by the `get-pricing` Edge Function. */
data class PricingPlan(
    val label: String,
    val flag: String,
    val amount: Int,
    /** Amount in the currency's smallest unit (paise/cents) — what Razorpay's order API expects. */
    val amountMinor: Int,
    val currency: String,
    val interval: String
)

data class PricingData(
    val badge: String,
    val indiaPlan: PricingPlan,
    val globalPlan: PricingPlan,
    val features: List<String>
)
