package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.Serializable

/** One region's plan, as served by the `get-pricing` Edge Function. */
@Serializable
data class PricingPlanDto(
    val label: String,
    val flag: String,
    val amount: Int,
    val currency: String,
    val amountMinor: Int,
    val interval: String
)

@Serializable
data class PricingPlansDto(
    val india: PricingPlanDto,
    val global: PricingPlanDto
)

/** Response shape of the `get-pricing` Edge Function. */
@Serializable
data class PricingResponse(
    val success: Boolean = false,
    val badge: String? = null,
    val plans: PricingPlansDto? = null,
    val features: List<String>? = null,
    val error: String? = null
)
