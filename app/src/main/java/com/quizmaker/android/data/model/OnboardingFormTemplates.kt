package com.quizmaker.android.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.UUID

/** A starter recipe for [OnboardingFormEditSheet] — picking one prefills the create sheet, same idea as [defaultFields] but with a full, ready-to-use form instead of the generic 3-field starter. */
data class OnboardingFormTemplate(
    val icon: ImageVector,
    val label: String,
    val summary: String,
    val title: String,
    val description: String,
    val welcomeMessage: String,
    val fields: List<ToolField>
)

private fun field(label: String, type: ToolFieldType, required: Boolean = false, options: List<String>? = null) =
    ToolField(id = UUID.randomUUID().toString(), label = label, type = type, required = required, options = options)

val ONBOARDING_FORM_TEMPLATES: List<OnboardingFormTemplate> = listOf(
    OnboardingFormTemplate(
        icon = Icons.Default.PersonAdd,
        label = "New Student Registration",
        summary = "Collect details from students joining your class",
        title = "New Student Registration",
        description = "Fill this out to register as a new student.",
        welcomeMessage = "Welcome aboard! We'll be in touch shortly.",
        fields = listOf(
            field("Full name", ToolFieldType.SHORT_TEXT, required = true),
            field("Email address", ToolFieldType.EMAIL, required = true),
            field("Phone number", ToolFieldType.PHONE, required = true),
            field("Date of birth", ToolFieldType.DATE),
            field("Grade / Class", ToolFieldType.SHORT_TEXT, required = true),
            field("Parent / Guardian name", ToolFieldType.SHORT_TEXT)
        )
    ),
    OnboardingFormTemplate(
        icon = Icons.Default.SupervisorAccount,
        label = "Parent Onboarding",
        summary = "Collect parent/guardian contact details",
        title = "Parent Onboarding",
        description = "Help us stay in touch with you about your child's progress.",
        welcomeMessage = "Thanks! We'll reach out through your preferred contact method.",
        fields = listOf(
            field("Parent / Guardian name", ToolFieldType.SHORT_TEXT, required = true),
            field("Email address", ToolFieldType.EMAIL, required = true),
            field("Phone number", ToolFieldType.PHONE, required = true),
            field("Child's name", ToolFieldType.SHORT_TEXT, required = true),
            field("Preferred contact method", ToolFieldType.RADIO, options = listOf("Email", "Phone", "WhatsApp"))
        )
    ),
    OnboardingFormTemplate(
        icon = Icons.Default.Work,
        label = "Teacher / Staff Onboarding",
        summary = "Collect details from new teachers or staff",
        title = "Teacher / Staff Onboarding",
        description = "Tell us a bit about yourself before you get started.",
        welcomeMessage = "Welcome to the team!",
        fields = listOf(
            field("Full name", ToolFieldType.SHORT_TEXT, required = true),
            field("Email address", ToolFieldType.EMAIL, required = true),
            field("Phone number", ToolFieldType.PHONE, required = true),
            field("Subject taught", ToolFieldType.SHORT_TEXT, required = true),
            field("Years of experience", ToolFieldType.NUMBER)
        )
    ),
    OnboardingFormTemplate(
        icon = Icons.Default.Book,
        label = "Course Enrollment",
        summary = "Sign learners up for a course or batch",
        title = "Course Enrollment",
        description = "Enroll in a course and pick your preferred batch.",
        welcomeMessage = "You're enrolled! Batch details will follow by email.",
        fields = listOf(
            field("Full name", ToolFieldType.SHORT_TEXT, required = true),
            field("Email address", ToolFieldType.EMAIL, required = true),
            field("Phone number", ToolFieldType.PHONE),
            field("Course interested in", ToolFieldType.SELECT, required = true, options = listOf("Beginner", "Intermediate", "Advanced")),
            field("Preferred batch timing", ToolFieldType.RADIO, options = listOf("Morning", "Afternoon", "Evening"))
        )
    ),
    OnboardingFormTemplate(
        icon = Icons.Default.Event,
        label = "Event Registration",
        summary = "Register attendees for an event",
        title = "Event Registration",
        description = "Reserve your spot at the event.",
        welcomeMessage = "You're registered! See you there.",
        fields = listOf(
            field("Full name", ToolFieldType.SHORT_TEXT, required = true),
            field("Email address", ToolFieldType.EMAIL, required = true),
            field("Phone number", ToolFieldType.PHONE),
            field("Number of guests", ToolFieldType.NUMBER),
            field("Dietary restrictions", ToolFieldType.LONG_TEXT)
        )
    ),
    OnboardingFormTemplate(
        icon = Icons.Default.School,
        label = "Workshop Sign-up",
        summary = "Gauge experience level before a workshop",
        title = "Workshop Sign-up",
        description = "Sign up for the upcoming workshop.",
        welcomeMessage = "You're all set! Bring a laptop if you have one.",
        fields = listOf(
            field("Full name", ToolFieldType.SHORT_TEXT, required = true),
            field("Email address", ToolFieldType.EMAIL, required = true),
            field("Phone number", ToolFieldType.PHONE),
            field("Prior experience level", ToolFieldType.RADIO, options = listOf("Beginner", "Intermediate", "Advanced")),
            field("Do you have a laptop?", ToolFieldType.RADIO, options = listOf("Yes", "No"))
        )
    ),
    OnboardingFormTemplate(
        icon = Icons.Default.Diversity3,
        label = "Volunteer Application",
        summary = "Collect availability and skills from volunteers",
        title = "Volunteer Application",
        description = "Thanks for offering to help — tell us more below.",
        welcomeMessage = "Thank you for volunteering! We'll be in touch.",
        fields = listOf(
            field("Full name", ToolFieldType.SHORT_TEXT, required = true),
            field("Email address", ToolFieldType.EMAIL, required = true),
            field("Phone number", ToolFieldType.PHONE, required = true),
            field("Availability", ToolFieldType.CHECKBOX, options = listOf("Weekdays", "Weekends", "Evenings")),
            field("Relevant skills", ToolFieldType.LONG_TEXT)
        )
    ),
    OnboardingFormTemplate(
        icon = Icons.Default.Groups,
        label = "Alumni Registration",
        summary = "Reconnect with former students",
        title = "Alumni Registration",
        description = "Reconnect with us and stay in the loop.",
        welcomeMessage = "Great to have you back in touch!",
        fields = listOf(
            field("Full name", ToolFieldType.SHORT_TEXT, required = true),
            field("Email address", ToolFieldType.EMAIL, required = true),
            field("Phone number", ToolFieldType.PHONE),
            field("Graduation year", ToolFieldType.NUMBER, required = true),
            field("Current occupation", ToolFieldType.SHORT_TEXT)
        )
    ),
    OnboardingFormTemplate(
        icon = Icons.Default.CalendarMonth,
        label = "Summer Camp Registration",
        summary = "Collect child and emergency details for camp",
        title = "Summer Camp Registration",
        description = "Register your child for summer camp.",
        welcomeMessage = "Registration received! Packing list to follow.",
        fields = listOf(
            field("Child's full name", ToolFieldType.SHORT_TEXT, required = true),
            field("Parent email address", ToolFieldType.EMAIL, required = true),
            field("Parent phone number", ToolFieldType.PHONE, required = true),
            field("Child's age", ToolFieldType.NUMBER, required = true),
            field("Emergency contact", ToolFieldType.SHORT_TEXT, required = true),
            field("Medical conditions / allergies", ToolFieldType.LONG_TEXT)
        )
    ),
    OnboardingFormTemplate(
        icon = Icons.Default.SportsHandball,
        label = "Library Membership",
        summary = "Sign learners up for library access",
        title = "Library Membership",
        description = "Sign up for a library membership.",
        welcomeMessage = "Your membership is active — happy reading!",
        fields = listOf(
            field("Full name", ToolFieldType.SHORT_TEXT, required = true),
            field("Email address", ToolFieldType.EMAIL, required = true),
            field("Phone number", ToolFieldType.PHONE),
            field("Preferred genre", ToolFieldType.SELECT, options = listOf("Fiction", "Non-fiction", "Science", "Comics")),
            field("Membership type", ToolFieldType.RADIO, options = listOf("Student", "Faculty", "Guest"))
        )
    )
)
