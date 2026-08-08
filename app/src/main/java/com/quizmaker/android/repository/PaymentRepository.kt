package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.remote.dto.CreateRazorpayOrderRequest
import com.quizmaker.android.data.remote.dto.CreateRazorpayOrderResponse
import com.quizmaker.android.data.remote.dto.PaymentRecordInsertDto
import com.quizmaker.android.data.remote.dto.VerifyRazorpayPaymentRequest
import com.quizmaker.android.data.remote.dto.VerifyRazorpayPaymentResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drives the Razorpay checkout's server-side legs — order creation and payment verification —
 * via the same `create-razorpay-order` / `verify-razorpay-payment` Edge Functions the web app's
 * TrialAndPricing.tsx uses, so premium-activation logic lives in one place shared by both
 * clients. The Razorpay secret key never touches this repository (or the app at all) — it only
 * ever lives server-side in those functions.
 */
@Singleton
class PaymentRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    data class RazorpayOrder(val id: String, val amount: Int, val currency: String)

    suspend fun createOrder(amountMinor: Int, currency: String): AppResult<RazorpayOrder> = safeCall {
        val response = supabase.functions.invoke("create-razorpay-order") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(CreateRazorpayOrderRequest(amount = amountMinor, currency = currency)))
        }
        val result = json.decodeFromString<CreateRazorpayOrderResponse>(response.bodyAsText())
        val orderId = result.id
        if (orderId.isNullOrBlank()) {
            error(result.error ?: "Couldn't start checkout. Please try again.")
        }
        RazorpayOrder(id = orderId, amount = result.amount ?: amountMinor, currency = result.currency ?: currency)
    }

    suspend fun verifyPayment(
        razorpayOrderId: String,
        razorpayPaymentId: String,
        razorpaySignature: String,
        userId: String,
        country: String?
    ): AppResult<Unit> = safeCall {
        val response = supabase.functions.invoke("verify-razorpay-payment") {
            contentType(ContentType.Application.Json)
            setBody(
                json.encodeToString(
                    VerifyRazorpayPaymentRequest(
                        razorpayOrderId = razorpayOrderId,
                        razorpayPaymentId = razorpayPaymentId,
                        razorpaySignature = razorpaySignature,
                        userId = userId,
                        country = country
                    )
                )
            )
        }
        val result = json.decodeFromString<VerifyRazorpayPaymentResponse>(response.bodyAsText())
        if (!result.success) {
            error(result.error ?: "Payment verification failed. If you were charged, contact support.")
        }
    }

    /** Best-effort log of a cancelled/failed attempt — mirrors the web app's direct client insert. */
    suspend fun recordAttempt(userId: String, amountMinor: Int, currency: String, status: String, errorMessage: String?) {
        runCatching {
            supabase.from("payments").insert(
                PaymentRecordInsertDto(
                    userId = userId,
                    amount = amountMinor,
                    currency = currency,
                    paymentStatus = status,
                    errorMessage = errorMessage
                )
            )
        }
    }
}
