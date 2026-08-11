package com.quizmaker.android.data.model

/** Broad subject grouping — drives which icon/color theme a template's card uses (see AiQuizScreen). */
enum class AiPromptCategory { HISTORY, SCIENCE, MATH, LANGUAGE, EXAM, SOCIAL, GENERAL, TECH }

/** One "trending" quiz-topic starter shown as a tappable card on the AI Create Quiz screen. */
data class AiPromptTemplate(val label: String, val prompt: String, val category: AiPromptCategory)

/**
 * 50 short quiz-topic starters. Only a random 10 are shown at a time (see
 * AiQuizViewModel.reshuffleTemplates()) — a fresh 10 every time the AI screen is opened, so the
 * carousel doesn't show the same picks on every visit.
 */
val AI_PROMPT_TEMPLATES: List<AiPromptTemplate> = listOf(
    "Indian History" to AiPromptCategory.HISTORY,
    "American History" to AiPromptCategory.HISTORY,
    "World War II" to AiPromptCategory.HISTORY,
    "Ancient Indian History" to AiPromptCategory.HISTORY,
    "Medieval Indian History" to AiPromptCategory.HISTORY,
    "Modern Indian History" to AiPromptCategory.HISTORY,
    "Indian Freedom Struggle" to AiPromptCategory.HISTORY,
    "NEET Biology" to AiPromptCategory.EXAM,
    "NEET Physics" to AiPromptCategory.EXAM,
    "NEET Chemistry" to AiPromptCategory.EXAM,
    "Railway RRB NTPC" to AiPromptCategory.EXAM,
    "Railway Group D" to AiPromptCategory.EXAM,
    "SSC CGL General Awareness" to AiPromptCategory.EXAM,
    "Banking Awareness" to AiPromptCategory.EXAM,
    "UPSC Prelims GK" to AiPromptCategory.EXAM,
    "English Class 5" to AiPromptCategory.LANGUAGE,
    "English Class 6" to AiPromptCategory.LANGUAGE,
    "English Class 7" to AiPromptCategory.LANGUAGE,
    "English Class 8" to AiPromptCategory.LANGUAGE,
    "English Grammar Basics" to AiPromptCategory.LANGUAGE,
    "Hindi Grammar Basics" to AiPromptCategory.LANGUAGE,
    "Science Class 6" to AiPromptCategory.SCIENCE,
    "Science Class 7" to AiPromptCategory.SCIENCE,
    "Science Class 8" to AiPromptCategory.SCIENCE,
    "Science Class 9" to AiPromptCategory.SCIENCE,
    "Science Class 10" to AiPromptCategory.SCIENCE,
    "Maths Class 5" to AiPromptCategory.MATH,
    "Maths Class 6" to AiPromptCategory.MATH,
    "Maths Class 7" to AiPromptCategory.MATH,
    "Maths Class 8" to AiPromptCategory.MATH,
    "Maths Class 10 - Trigonometry" to AiPromptCategory.MATH,
    "Physics Class 10 - Light" to AiPromptCategory.SCIENCE,
    "Chemistry Class 10 - Acids and Bases" to AiPromptCategory.SCIENCE,
    "Biology Class 10 - Life Processes" to AiPromptCategory.SCIENCE,
    "Social Studies Class 6" to AiPromptCategory.SOCIAL,
    "Geography Class 8" to AiPromptCategory.SOCIAL,
    "Civics Class 9" to AiPromptCategory.SOCIAL,
    "Indian Constitution" to AiPromptCategory.SOCIAL,
    "Indian Geography" to AiPromptCategory.SOCIAL,
    "World Geography" to AiPromptCategory.SOCIAL,
    "World Capitals" to AiPromptCategory.SOCIAL,
    "General Knowledge" to AiPromptCategory.GENERAL,
    "Current Affairs" to AiPromptCategory.GENERAL,
    "Computer Basics" to AiPromptCategory.TECH,
    "Environmental Science" to AiPromptCategory.SCIENCE,
    "Human Biology" to AiPromptCategory.SCIENCE,
    "Human Anatomy" to AiPromptCategory.SCIENCE,
    "Solar System" to AiPromptCategory.SCIENCE,
    "Basic Chemistry Reactions" to AiPromptCategory.SCIENCE,
    "Cricket General Knowledge" to AiPromptCategory.GENERAL
).map { (topic, category) -> AiPromptTemplate(label = topic, prompt = "Create a quiz on $topic", category = category) }
