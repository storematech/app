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
    val browser: String? = null,
    @SerialName("user_type") val userType: String? = null,
    @SerialName("license_expired_date") val licenseExpiredDate: String? = null,
    @SerialName("business_logo") val businessLogo: String? = null,
    val address: String? = null
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
    val browser: String? = null,
    val address: String? = null
)

/** Letterhead logo — its own DTO since it's written by a dedicated upload flow, never bundled into the general profile-fields update. */
@Serializable
data class ProfileLogoUpdateDto(
    @SerialName("business_logo") val businessLogo: String?
)

@Serializable
data class ProfileInsertDto(
    val id: String,
    val email: String? = null,
    val name: String? = null
)

/** Onboarding's one-time phone collection — only ever touches these two columns, never name/business_name. */
@Serializable
data class ProfilePhoneUpdateDto(
    @SerialName("phone_number") val phoneNumber: String,
    val country: String
)

/** Push notification device token — only ever touches this one column. */
@Serializable
data class ProfileFcmTokenUpdateDto(
    @SerialName("fcm_token") val fcmToken: String
)
