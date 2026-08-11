package com.quizmaker.android.data.model

import com.quizmaker.android.BuildConfig
import com.quizmaker.android.data.remote.dto.RsvpEventDto
import com.quizmaker.android.data.remote.dto.RsvpRegistrationDto

enum class AttendingStatus(val value: String, val label: String) {
    YES("yes", "Attending"),
    NO("no", "Not attending"),
    MAYBE("maybe", "Maybe");

    companion object {
        fun fromValue(value: String) = entries.firstOrNull { it.value == value } ?: YES
    }
}

/** A shareable event page a teacher builds to collect RSVPs — mirrors `RsvpEvent` in the web app's types/tools.ts. */
data class RsvpEvent(
    val id: String,
    val title: String,
    val description: String?,
    val slug: String,
    val location: String?,
    val eventDate: String?,
    val capacity: Int?,
    val rsvpDeadline: String?,
    val allowGuests: Boolean,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
) {
    val shareUrl: String get() = "${BuildConfig.SHARE_BASE_URL}/rsvp/$slug"
}

data class RsvpRegistration(
    val id: String,
    val eventId: String,
    val name: String,
    val email: String?,
    val phone: String?,
    val guestCount: Int,
    val attending: AttendingStatus,
    val notes: String?,
    val createdAt: String
)

data class RsvpSummary(
    val totalRegistrations: Int,
    val totalAttending: Int,
    val totalGuests: Int,
    val capacityRemaining: Int?
)

fun RsvpEventDto.toDomain(): RsvpEvent = RsvpEvent(
    id = id,
    title = title,
    description = description,
    slug = slug,
    location = location,
    eventDate = eventDate,
    capacity = capacity,
    rsvpDeadline = rsvpDeadline,
    allowGuests = allowGuests,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun RsvpRegistrationDto.toDomain(): RsvpRegistration = RsvpRegistration(
    id = id,
    eventId = eventId,
    name = name,
    email = email,
    phone = phone,
    guestCount = guestCount,
    attending = AttendingStatus.fromValue(attending),
    notes = notes,
    createdAt = createdAt
)

/** Attendee count includes the registrant themself plus their guests, counting only "yes" registrations. */
fun RsvpEvent.summarize(registrations: List<RsvpRegistration>): RsvpSummary {
    val attending = registrations.filter { it.attending == AttendingStatus.YES }
    val totalGuests = attending.sumOf { it.guestCount }
    val totalAttending = attending.size + totalGuests
    return RsvpSummary(
        totalRegistrations = registrations.size,
        totalAttending = totalAttending,
        totalGuests = totalGuests,
        capacityRemaining = capacity?.let { (it - totalAttending).coerceAtLeast(0) }
    )
}
