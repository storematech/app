package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.PricingData
import com.quizmaker.android.data.model.PricingPlan
import com.quizmaker.android.data.remote.dto.PricingResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches premium pricing from the `get-pricing` Edge Function so the numbers can change
 * (regional pricing, promos) with a function redeploy — no app release needed. Read-only:
 * this repository never initiates a charge (see that function's header for the current state
 * of the payment flow).
 */
@Singleton
class PricingRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getPricing(): AppResult<PricingData> = safeCall {
        val response = supabase.functions.invoke("get-pricing")
        val result = json.decodeFromString<PricingResponse>(response.bodyAsText())
        val plans = result.plans
        if (!result.success || plans == null) {
            error(result.error ?: "Couldn't load pricing. Please try again.")
        }
        PricingData(
            badge = result.badge.orEmpty(),
            indiaPlan = PricingPlan(
                label = plans.india.label,
                flag = plans.india.flag,
                amount = plans.india.amount,
                amountMinor = plans.india.amountMinor,
                currency = plans.india.currency,
                interval = plans.india.interval
            ),
            globalPlan = PricingPlan(
                label = plans.global.label,
                flag = plans.global.flag,
                amount = plans.global.amount,
                amountMinor = plans.global.amountMinor,
                currency = plans.global.currency,
                interval = plans.global.interval
            ),
            features = result.features.orEmpty()
        )
    }
}
