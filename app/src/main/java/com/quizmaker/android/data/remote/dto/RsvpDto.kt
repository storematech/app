package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape of the `rsvp_events` table. */
@Serializable
data class RsvpEventDto(
    val id: String,
    @SerialName("created_by") val createdBy: String? = null,
    val title: String,
    val description: String? = null,
    val slug: String,
    val location: String? = null,
    @SerialName("event_date") val eventDate: String? = null,
    val capacity: Int? = null,
    @SerialName("rsvp_deadline") val rsvpDeadline: String? = null,
    @SerialName("allow_guests") val allowGuests: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class RsvpEventInsertDto(
    @SerialName("created_by") val createdBy: String,
    val title: String,
    val description: String?,
    val slug: String,
    val location: String?,
    @SerialName("event_date") val eventDate: String?,
    val capacity: Int?,
    @SerialName("rsvp_deadline") val rsvpDeadline: String?,
    @SerialName("allow_guests") val allowGuests: Boolean,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class RsvpEventUpdateDto(
    val title: String,
    val description: String?,
    val location: String?,
    @SerialName("event_date") val eventDate: String?,
    val capacity: Int?,
    @SerialName("rsvp_deadline") val rsvpDeadline: String?,
    @SerialName("allow_guests") val allowGuests: Boolean,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class RsvpEventActiveUpdateDto(
    @SerialName("is_active") val isActive: Boolean
)

/** Row shape of the `rsvp_registrations` table — decode-only, mobile never writes registrations. */
@Serializable
data class RsvpRegistrationDto(
    val id: String,
    @SerialName("event_id") val eventId: String,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    @SerialName("guest_count") val guestCount: Int = 0,
    val attending: String = "yes",
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String
)
