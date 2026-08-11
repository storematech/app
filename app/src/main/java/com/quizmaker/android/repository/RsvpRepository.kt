package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.RsvpEvent
import com.quizmaker.android.data.model.RsvpRegistration
import com.quizmaker.android.data.model.toDomain
import com.quizmaker.android.data.remote.dto.RsvpEventActiveUpdateDto
import com.quizmaker.android.data.remote.dto.RsvpEventDto
import com.quizmaker.android.data.remote.dto.RsvpEventInsertDto
import com.quizmaker.android.data.remote.dto.RsvpEventUpdateDto
import com.quizmaker.android.data.remote.dto.RsvpRegistrationDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Minimal row shape for the narrow `event_id`-only select in getRegistrationCounts(). */
@Serializable
private data class EventIdRow(@SerialName("event_id") val eventId: String)

/**
 * Reads/writes `rsvp_events`/`rsvp_registrations` — the fifth of the "Tools" (More → Tools),
 * teacher-facing event management only. Registering via the share link stays a web-only flow.
 * The simplest of the six tools' repositories — no dynamic jsonb field array, just flat columns.
 */
@Singleton
class RsvpRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getEvents(userId: String): AppResult<List<RsvpEvent>> = safeCall {
        supabase.from("rsvp_events")
            .select {
                filter { eq("created_by", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<RsvpEventDto>()
            .map { it.toDomain() }
    }

    suspend fun getEvent(eventId: String): AppResult<RsvpEvent> = safeCall {
        supabase.from("rsvp_events")
            .select { filter { eq("id", eventId) } }
            .decodeSingle<RsvpEventDto>()
            .toDomain()
    }

    suspend fun getRegistrationCounts(eventIds: List<String>): AppResult<Map<String, Int>> = safeCall {
        if (eventIds.isEmpty()) return@safeCall emptyMap()
        supabase.from("rsvp_registrations")
            .select(Columns.raw("event_id")) { filter { isIn("event_id", eventIds) } }
            .decodeList<EventIdRow>()
            .groupingBy { it.eventId }
            .eachCount()
    }

    suspend fun createEvent(
        userId: String,
        title: String,
        description: String?,
        location: String?,
        eventDate: String?,
        capacity: Int?,
        rsvpDeadline: String?,
        allowGuests: Boolean,
        isActive: Boolean
    ): AppResult<RsvpEvent> = safeCall {
        supabase.from("rsvp_events")
            .insert(
                RsvpEventInsertDto(
                    createdBy = userId,
                    title = title,
                    description = description,
                    slug = generateSlug(title),
                    location = location,
                    eventDate = eventDate,
                    capacity = capacity,
                    rsvpDeadline = rsvpDeadline,
                    allowGuests = allowGuests,
                    isActive = isActive
                )
            ) { select() }
            .decodeSingle<RsvpEventDto>()
            .toDomain()
    }

    suspend fun updateEvent(
        eventId: String,
        title: String,
        description: String?,
        location: String?,
        eventDate: String?,
        capacity: Int?,
        rsvpDeadline: String?,
        allowGuests: Boolean,
        isActive: Boolean
    ): AppResult<RsvpEvent> = safeCall {
        supabase.from("rsvp_events")
            .update(
                RsvpEventUpdateDto(
                    title = title,
                    description = description,
                    location = location,
                    eventDate = eventDate,
                    capacity = capacity,
                    rsvpDeadline = rsvpDeadline,
                    allowGuests = allowGuests,
                    isActive = isActive
                )
            ) {
                filter { eq("id", eventId) }
                select()
            }
            .decodeSingle<RsvpEventDto>()
            .toDomain()
    }

    suspend fun setActive(eventId: String, isActive: Boolean): AppResult<Unit> = safeCall {
        supabase.from("rsvp_events").update(RsvpEventActiveUpdateDto(isActive = isActive)) {
            filter { eq("id", eventId) }
        }
        Unit
    }

    suspend fun deleteEvent(eventId: String): AppResult<Unit> = safeCall {
        supabase.from("rsvp_events").delete { filter { eq("id", eventId) } }
        Unit
    }

    suspend fun getRegistrations(eventId: String): AppResult<List<RsvpRegistration>> = safeCall {
        supabase.from("rsvp_registrations")
            .select {
                filter { eq("event_id", eventId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<RsvpRegistrationDto>()
            .map { it.toDomain() }
    }

    /** `slug` is unique-constrained — a short random suffix avoids a pre-check round trip for the common case. */
    private fun generateSlug(title: String): String {
        val base = title.lowercase().trim()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "event" }
        return "$base-${UUID.randomUUID().toString().take(6)}"
    }
}
