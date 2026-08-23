package com.quizmaker.android.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

/** A starter recipe for [PollEditSheet] — picking one prefills the create sheet, same idea as [defaultOptions] but with a full, ready-to-use poll. */
data class PollTemplate(
    val icon: ImageVector,
    val label: String,
    val summary: String,
    val question: String,
    val description: String,
    val options: List<String>,
    val allowMultiple: Boolean = false
)

val POLL_TEMPLATES: List<PollTemplate> = listOf(
    PollTemplate(
        icon = Icons.Default.MenuBook,
        label = "Next Topic Vote",
        summary = "Let learners choose the next topic",
        question = "Which topic should we cover next?",
        description = "Pick the topic you'd like to learn next.",
        options = listOf("Algebra", "Geometry", "Statistics", "Trigonometry")
    ),
    PollTemplate(
        icon = Icons.Default.CalendarMonth,
        label = "Class Timing Preference",
        summary = "Find the best time for extra classes",
        question = "What time works best for extra classes?",
        description = "Help us schedule extra classes at a convenient time.",
        options = listOf("Morning", "Afternoon", "Evening", "Weekend")
    ),
    PollTemplate(
        icon = Icons.Default.DirectionsBus,
        label = "Field Trip Destination",
        summary = "Vote on where to go for a field trip",
        question = "Where should we go for the field trip?",
        description = "Cast your vote for the class field trip.",
        options = listOf("Science Museum", "Zoo", "Historical Fort", "Amusement Park")
    ),
    PollTemplate(
        icon = Icons.Default.Quiz,
        label = "Exam Date Preference",
        summary = "Pick the best date for an upcoming exam",
        question = "Which date works best for the exam?",
        description = "Vote for your preferred exam date.",
        options = listOf("Monday", "Wednesday", "Friday", "Next Week")
    ),
    PollTemplate(
        icon = Icons.Default.Celebration,
        label = "Event Theme Vote",
        summary = "Vote on the theme for the annual event",
        question = "What theme should we pick for the annual event?",
        description = "Vote for your favorite theme.",
        options = listOf("Retro", "Space", "Sports", "Cultural Heritage")
    ),
    PollTemplate(
        icon = Icons.Default.School,
        label = "Homework Frequency",
        summary = "Ask how often homework should be assigned",
        question = "How often should we assign homework?",
        description = "Share your preference on homework frequency.",
        options = listOf("Daily", "Weekly", "Biweekly", "Rarely")
    ),
    PollTemplate(
        icon = Icons.Default.Fastfood,
        label = "Lunch Menu Vote",
        summary = "Vote on new cafeteria menu items",
        question = "What should be added to the cafeteria menu?",
        description = "Help us pick new items for the menu.",
        options = listOf("Pasta", "Sandwiches", "Salad Bar", "Fresh Juice")
    ),
    PollTemplate(
        icon = Icons.Default.SportsSoccer,
        label = "Sports Day Activity",
        summary = "Vote on a new Sports Day activity",
        question = "Which activity should we add to Sports Day?",
        description = "Vote for an activity to add this year.",
        options = listOf("Tug of War", "Relay Race", "Basketball", "Sack Race")
    ),
    PollTemplate(
        icon = Icons.Default.Star,
        label = "Best Teaching Method",
        summary = "Learn which teaching style learners prefer",
        question = "Which teaching method do you prefer?",
        description = "Your feedback helps us teach better.",
        options = listOf("Lecture", "Group Discussion", "Practical / Hands-on", "Video Lessons")
    ),
    PollTemplate(
        icon = Icons.Default.Groups,
        label = "Club Interest Poll",
        summary = "Gauge interest in different clubs",
        question = "Which club would you like to join?",
        description = "Vote for the club you're most interested in.",
        options = listOf("Debate Club", "Art Club", "Coding Club", "Music Club")
    )
)
