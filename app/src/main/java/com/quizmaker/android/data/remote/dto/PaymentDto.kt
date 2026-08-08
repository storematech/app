package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateRazorpayOrderRequest(
    val amount: Int,
    val currency: String
)

/** Razorpay order object shape, as relayed back by the `create-razorpay-order` Edge Function. */
@Serializable
data class CreateRazorpayOrderResponse(
    val id: String? = null,
    val amount: Int? = null,
    val currency: String? = null,
    val error: String? = null
)

@Serializable
data class VerifyRazorpayPaymentRequest(
    @SerialName("razorpay_order_id") val razorpayOrderId: String,
    @SerialName("razorpay_payment_id") val razorpayPaymentId: String,
    @SerialName("razorpay_signature") val razorpaySignature: String,
    @SerialName("user_id") val userId: String,
    val country: String?
)

@Serializable
data class VerifyRazorpayPaymentResponse(
    val success: Boolean = false,
    val error: String? = null
)

/** Row shape of the `payments` table — mirrors the web app's client-side insert on cancel/fail. */
@Serializable
data class PaymentRecordInsertDto(
    @SerialName("user_id") val userId: String,
    val amount: Int,
    val currency: String,
    @SerialName("payment_status") val paymentStatus: String,
    @SerialName("error_message") val errorMessage: String? = null
)
