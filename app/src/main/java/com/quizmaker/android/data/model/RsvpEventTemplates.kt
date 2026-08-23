package com.quizmaker.android.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A starter recipe for [RsvpEventEditSheet] — picking one prefills the create sheet. No `eventDate`
 * is included since it's inherently specific to when the real event happens — the user picks that
 * themselves via the sheet's date/time picker after applying a template.
 */
data class RsvpEventTemplate(
    val icon: ImageVector,
    val label: String,
    val summary: String,
    val title: String,
    val description: String,
    val location: String,
    val allowGuests: Boolean
)

val RSVP_EVENT_TEMPLATES: List<RsvpEventTemplate> = listOf(
    RsvpEventTemplate(
        icon = Icons.Default.MilitaryTech,
        label = "Annual Sports Day",
        summary = "Registrations for Sports Day",
        title = "Annual Sports Day",
        description = "Join us for a day of games and athletics.",
        location = "School Sports Ground",
        allowGuests = true
    ),
    RsvpEventTemplate(
        icon = Icons.Default.Groups,
        label = "Parent-Teacher Meeting",
        summary = "Book a slot for a PTM",
        title = "Parent-Teacher Meeting",
        description = "Discuss your child's progress with their teachers.",
        location = "School Campus",
        allowGuests = false
    ),
    RsvpEventTemplate(
        icon = Icons.Default.Celebration,
        label = "Annual Day Celebration",
        summary = "Registrations for Annual Day",
        title = "Annual Day Celebration",
        description = "An evening of performances and celebration.",
        location = "School Auditorium",
        allowGuests = true
    ),
    RsvpEventTemplate(
        icon = Icons.Default.Science,
        label = "Science Exhibition",
        summary = "Registrations for a science exhibition",
        title = "Science Exhibition",
        description = "Explore student science projects and experiments.",
        location = "School Hall",
        allowGuests = true
    ),
    RsvpEventTemplate(
        icon = Icons.Default.EmojiEvents,
        label = "Graduation Ceremony",
        summary = "RSVP for a graduation ceremony",
        title = "Graduation Ceremony",
        description = "Celebrate our graduating class with us.",
        location = "Main Auditorium",
        allowGuests = true
    ),
    RsvpEventTemplate(
        icon = Icons.Default.School,
        label = "Workshop / Seminar",
        summary = "RSVP for a workshop or seminar",
        title = "Workshop",
        description = "Register to attend this workshop.",
        location = "Conference Room",
        allowGuests = false
    ),
    RsvpEventTemplate(
        icon = Icons.Default.MeetingRoom,
        label = "Open House",
        summary = "RSVP for an open house visit",
        title = "Open House",
        description = "Tour our campus and meet our teachers.",
        location = "School Campus",
        allowGuests = true
    ),
    RsvpEventTemplate(
        icon = Icons.Default.Park,
        label = "Alumni Meet",
        summary = "RSVP for an alumni gathering",
        title = "Alumni Meet",
        description = "Reconnect with old classmates and teachers.",
        location = "School Grounds",
        allowGuests = true
    ),
    RsvpEventTemplate(
        icon = Icons.Default.TheaterComedy,
        label = "Cultural Fest",
        summary = "Registrations for a cultural fest",
        title = "Cultural Fest",
        description = "A celebration of music, dance, and art.",
        location = "School Auditorium",
        allowGuests = true
    ),
    RsvpEventTemplate(
        icon = Icons.Default.CalendarMonth,
        label = "Orientation Day",
        summary = "RSVP for a new-student orientation",
        title = "Orientation Day",
        description = "Welcoming new students and families to campus.",
        location = "Main Hall",
        allowGuests = true
    )
)
