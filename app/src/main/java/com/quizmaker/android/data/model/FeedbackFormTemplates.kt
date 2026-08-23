package com.quizmaker.android.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Store
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.UUID

/** A starter recipe for [FeedbackFormEditSheet] — picking one prefills the create sheet, same idea as [defaultQuestions] but with a full, ready-to-use form. */
data class FeedbackFormTemplate(
    val icon: ImageVector,
    val label: String,
    val summary: String,
    val title: String,
    val description: String,
    val questions: List<ToolField>
)

private fun field(label: String, type: ToolFieldType, required: Boolean = false, options: List<String>? = null) =
    ToolField(id = UUID.randomUUID().toString(), label = label, type = type, required = required, options = options)

val FEEDBACK_FORM_TEMPLATES: List<FeedbackFormTemplate> = listOf(
    FeedbackFormTemplate(
        icon = Icons.Default.MenuBook,
        label = "Course Feedback",
        summary = "Rate a course and gather improvement ideas",
        title = "Course Feedback",
        description = "Help us improve this course.",
        questions = listOf(
            field("Overall rating", ToolFieldType.RATING, required = true),
            field("Was the content clear?", ToolFieldType.RADIO, options = listOf("Yes", "No")),
            field("What could we improve?", ToolFieldType.LONG_TEXT)
        )
    ),
    FeedbackFormTemplate(
        icon = Icons.Default.Person,
        label = "Teacher Evaluation",
        summary = "Gather feedback on teaching quality",
        title = "Teacher Evaluation",
        description = "Share your honest feedback about the teacher.",
        questions = listOf(
            field("Teaching effectiveness", ToolFieldType.RATING, required = true),
            field("Communication clarity", ToolFieldType.RATING, required = true),
            field("Suggestions for improvement", ToolFieldType.LONG_TEXT)
        )
    ),
    FeedbackFormTemplate(
        icon = Icons.Default.Celebration,
        label = "Event Feedback",
        summary = "Learn what worked at an event",
        title = "Event Feedback",
        description = "Tell us how the event went for you.",
        questions = listOf(
            field("How was the event overall?", ToolFieldType.RATING, required = true),
            field("What did you like most?", ToolFieldType.LONG_TEXT),
            field("Would you attend again?", ToolFieldType.RADIO, options = listOf("Yes", "No"))
        )
    ),
    FeedbackFormTemplate(
        icon = Icons.Default.QuestionAnswer,
        label = "Exam / Test Feedback",
        summary = "Check difficulty and timing after a test",
        title = "Exam Feedback",
        description = "Help us calibrate future exams.",
        questions = listOf(
            field("Difficulty level", ToolFieldType.RADIO, required = true, options = listOf("Easy", "Moderate", "Hard")),
            field("Was the time given sufficient?", ToolFieldType.RADIO, options = listOf("Yes", "No")),
            field("Comments", ToolFieldType.LONG_TEXT)
        )
    ),
    FeedbackFormTemplate(
        icon = Icons.Default.MeetingRoom,
        label = "Facility Feedback",
        summary = "Rate cleanliness and comfort",
        title = "Facility Feedback",
        description = "Help us keep the facilities in good shape.",
        questions = listOf(
            field("Cleanliness", ToolFieldType.RATING, required = true),
            field("Comfort", ToolFieldType.RATING, required = true),
            field("Suggestions", ToolFieldType.LONG_TEXT)
        )
    ),
    FeedbackFormTemplate(
        icon = Icons.Default.School,
        label = "Workshop Feedback",
        summary = "Rate content quality and the trainer",
        title = "Workshop Feedback",
        description = "Tell us how the workshop went.",
        questions = listOf(
            field("Content quality", ToolFieldType.RATING, required = true),
            field("Trainer effectiveness", ToolFieldType.RATING, required = true),
            field("Additional comments", ToolFieldType.LONG_TEXT)
        )
    ),
    FeedbackFormTemplate(
        icon = Icons.Default.Groups,
        label = "Parent-Teacher Meeting",
        summary = "Feedback on a parent-teacher meeting",
        title = "Parent-Teacher Meeting Feedback",
        description = "Share your feedback on today's meeting.",
        questions = listOf(
            field("How useful was the meeting?", ToolFieldType.RATING, required = true),
            field("Were topics covered clearly?", ToolFieldType.RADIO, options = listOf("Yes", "No")),
            field("Suggestions", ToolFieldType.LONG_TEXT)
        )
    ),
    FeedbackFormTemplate(
        icon = Icons.Default.Fastfood,
        label = "Cafeteria Feedback",
        summary = "Rate food quality and variety",
        title = "Cafeteria Feedback",
        description = "Help us improve the food on offer.",
        questions = listOf(
            field("Food quality", ToolFieldType.RATING, required = true),
            field("Variety", ToolFieldType.RATING, required = true),
            field("Suggestions", ToolFieldType.LONG_TEXT)
        )
    ),
    FeedbackFormTemplate(
        icon = Icons.Default.LaptopMac,
        label = "Online Class Feedback",
        summary = "Check audio/video quality and pace",
        title = "Online Class Feedback",
        description = "Help us improve online classes.",
        questions = listOf(
            field("Audio / video quality", ToolFieldType.RATING, required = true),
            field("Pace of teaching", ToolFieldType.RADIO, options = listOf("Too slow", "Just right", "Too fast")),
            field("Comments", ToolFieldType.LONG_TEXT)
        )
    ),
    FeedbackFormTemplate(
        icon = Icons.Default.Store,
        label = "General Suggestion Box",
        summary = "Open-ended feedback on what to improve",
        title = "Suggestion Box",
        description = "Share any suggestions you have for us.",
        questions = listOf(
            field("What's working well?", ToolFieldType.LONG_TEXT),
            field("What needs improvement?", ToolFieldType.LONG_TEXT),
            field("Overall rating", ToolFieldType.RATING, required = true)
        )
    )
)
