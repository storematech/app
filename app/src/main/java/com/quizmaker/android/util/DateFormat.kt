package com.quizmaker.android.util

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

private val MONTH_ABBREVIATIONS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/**
 * Formats an ISO-8601 timestamp as "Aug 4, 2026". Accepts both `timestamptz` values
 * ("2026-08-04T00:00:00Z") and plain `date` columns ("2026-08-04", no time component).
 */
fun formatShortDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val normalized = if (iso.contains("T")) iso else "${iso}T00:00:00Z"
        formatShortDate(Instant.parse(normalized))
    } catch (e: Exception) {
        ""
    }
}

fun formatShortDate(instant: Instant?): String {
    if (instant == null) return ""
    return try {
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${MONTH_ABBREVIATIONS[dateTime.month.number - 1]} ${dateTime.day}, ${dateTime.year}"
    } catch (e: Exception) {
        ""
    }
}

/** Formats an ISO-8601 timestamp as "Aug 4, 2026, 3:45 PM". */
fun formatDateTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "-"
    return try {
        formatDateTime(Instant.parse(iso))
    } catch (e: Exception) {
        "-"
    }
}

fun formatDateTime(instant: Instant?): String {
    if (instant == null) return "-"
    return try {
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val hour24 = dateTime.hour
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val amPm = if (hour24 < 12) "AM" else "PM"
        val minute = dateTime.minute.toString().padStart(2, '0')
        "${MONTH_ABBREVIATIONS[dateTime.month.number - 1]} ${dateTime.day}, ${dateTime.year}, $hour12:$minute $amPm"
    } catch (e: Exception) {
        "-"
    }
}
