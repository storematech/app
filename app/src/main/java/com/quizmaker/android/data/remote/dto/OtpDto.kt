package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.Serializable

/** Request/response shapes for the web app's `send-otp` / `verify-otp` Supabase Edge Functions. */
@Serializable
data class SendOtpRequest(val email: String, val quizId: String, val quizTitle: String)

@Serializable
data class VerifyOtpRequest(val email: String, val quizId: String, val otpCode: String)

@Serializable
data class OtpFunctionResponse(val success: Boolean, val error: String? = null, val message: String? = null)
