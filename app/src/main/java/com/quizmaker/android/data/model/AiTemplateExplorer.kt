package com.quizmaker.android.data.model

/** One labeled group of topic starters shown in the "browse all templates" sheet opened from the
 *  AI Quick tab's "View All" button — distinct from the random 10-of-50 [AI_PROMPT_TEMPLATES]
 *  carousel: this is the full, organized catalog, grouped the way a student/teacher would browse
 *  by subject rather than see a shuffled sample. */
data class AiTemplateGroup(val title: String, val topics: List<String>)

private val HISTORY_TOPICS: List<String> = listOf(
    "World War I",
    "World War II",
    "Ancient Indian History",
    "Medieval Indian History",
    "Modern Indian History",
    "Asian History",
    "American History"
) + listOf(
    "India", "United States", "United Kingdom", "France", "Germany", "Italy", "Spain", "Portugal",
    "Russia", "China", "Japan", "South Korea", "North Korea", "Mongolia", "Vietnam", "Thailand",
    "Philippines", "Indonesia", "Malaysia", "Singapore", "Pakistan", "Bangladesh", "Sri Lanka",
    "Nepal", "Bhutan", "Afghanistan", "Iran", "Iraq", "Saudi Arabia", "Turkey", "Israel", "Egypt",
    "Greece", "Poland", "Netherlands", "Belgium", "Switzerland", "Austria", "Sweden", "Norway",
    "Denmark", "Finland", "Ireland", "Ukraine", "Brazil", "Argentina", "Mexico", "Canada",
    "Australia", "South Africa"
).map { "$it History" }

/** Full catalog shown in [com.quizmaker.android.ui.aiquiz.AiTemplatesExplorerSheet], one section
 *  per subject area. Every group also gets a trailing "Custom" entry rendered by the sheet itself
 *  (not stored here) so users can type their own topic instead of picking one of these. */
val AI_TEMPLATE_GROUPS: List<AiTemplateGroup> = listOf(
    AiTemplateGroup("Class Wise", (1..10).map { "Class $it" }),
    AiTemplateGroup("History", HISTORY_TOPICS),
    AiTemplateGroup(
        "Sciences",
        listOf(
            "Physics - Light",
            "Physics - Motion and Force",
            "Physics - Electricity and Circuits",
            "Physics - Magnetism",
            "Chemistry - Acids, Bases and Salts",
            "Chemistry - Periodic Table",
            "Chemistry - Chemical Reactions",
            "Biology - Life Processes",
            "Biology - Human Anatomy",
            "Biology - Genetics and Heredity",
            "Biology - Ecosystems and Environment",
            "Solar System and Space"
        )
    ),
    AiTemplateGroup(
        "Health Care",
        listOf(
            "Human Anatomy",
            "Nutrition and Diet",
            "First Aid Basics",
            "Common Diseases and Prevention",
            "Mental Health Awareness",
            "Personal Hygiene"
        )
    ),
    AiTemplateGroup(
        "Personal Development",
        listOf(
            "Time Management",
            "Communication Skills",
            "Leadership Skills",
            "Emotional Intelligence",
            "Goal Setting",
            "Critical Thinking"
        )
    ),
    AiTemplateGroup(
        "English Learner",
        listOf(
            "English Grammar Basics",
            "Vocabulary Building",
            "Spoken English Practice",
            "Business English",
            "IELTS Preparation",
            "Reading Comprehension"
        )
    )
)
