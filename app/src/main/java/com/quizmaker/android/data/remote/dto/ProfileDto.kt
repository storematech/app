package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape of the `profiles` table. */
@Serializable
data class ProfileDto(
    val id: String,
    val email: String? = null,
    val name: String? = null,
    @SerialName("business_name") val businessName: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val country: String? = null,
    @SerialName("user_count") val userCount: String? = null,
    val role: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("device_type") val deviceType: String? = null,
    val browser: String? = null
)

/** Partial update payload — only non-null fields are sent. */
@Serializable
data class ProfileUpdateDto(
    val name: String? = null,
    @SerialName("business_name") val businessName: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val country: String? = null,
    val email: String? = null,
    @SerialName("device_type") val deviceType: String? = null,
    val browser: String? = null
)

@Serializable
data class ProfileInsertDto(
    val id: String,
    val email: String? = null,
    val name: String? = null
)
