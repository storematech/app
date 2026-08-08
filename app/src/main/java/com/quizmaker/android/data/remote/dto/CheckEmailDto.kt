package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CheckEmailExistsRequest(val email: String)

@Serializable
data class CheckEmailExistsResponse(
    val success: Boolean = false,
    val exists: Boolean = false,
    val error: String? = null
)
