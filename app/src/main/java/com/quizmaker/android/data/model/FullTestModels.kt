package com.quizmaker.android.data.model

/** Which section of the exam browser a suggestion belongs to — see FULL_TEST_EXAM_SUGGESTIONS. */
enum class ExamCategory { EXAM_PREP, JOB_PREP }

/** One tappable exam suggestion on the Full Test entry screen — tapping pre-fills the exam-name
 *  field, mirroring how AiQuizScreen's own trending-template chips pre-fill its prompt field. */
data class ExamSuggestion(val name: String, val category: ExamCategory)

/** 50+ real Indian competitive/entrance and government-job exams, split into the two categories
 *  the Full Test entry screen shows as separate horizontally-scrollable rows. */
val FULL_TEST_EXAM_SUGGESTIONS = listOf(
    // Exam Prep — school/engineering/medical/law entrance exams
    ExamSuggestion("JEE Main", ExamCategory.EXAM_PREP),
    ExamSuggestion("JEE Advanced", ExamCategory.EXAM_PREP),
    ExamSuggestion("NEET UG", ExamCategory.EXAM_PREP),
    ExamSuggestion("NEET PG", ExamCategory.EXAM_PREP),
    ExamSuggestion("BITSAT", ExamCategory.EXAM_PREP),
    ExamSuggestion("MHT-CET", ExamCategory.EXAM_PREP),
    ExamSuggestion("WBJEE", ExamCategory.EXAM_PREP),
    ExamSuggestion("KCET", ExamCategory.EXAM_PREP),
    ExamSuggestion("VITEEE", ExamCategory.EXAM_PREP),
    ExamSuggestion("SRMJEEE", ExamCategory.EXAM_PREP),
    ExamSuggestion("COMEDK UGET", ExamCategory.EXAM_PREP),
    ExamSuggestion("CUET UG", ExamCategory.EXAM_PREP),
    ExamSuggestion("CLAT", ExamCategory.EXAM_PREP),
    ExamSuggestion("AILET", ExamCategory.EXAM_PREP),
    ExamSuggestion("NDA", ExamCategory.EXAM_PREP),
    ExamSuggestion("CDS", ExamCategory.EXAM_PREP),
    ExamSuggestion("AFCAT", ExamCategory.EXAM_PREP),
    ExamSuggestion("CAT", ExamCategory.EXAM_PREP),
    ExamSuggestion("XAT", ExamCategory.EXAM_PREP),
    ExamSuggestion("SNAP", ExamCategory.EXAM_PREP),
    ExamSuggestion("GATE", ExamCategory.EXAM_PREP),
    ExamSuggestion("CTET", ExamCategory.EXAM_PREP),
    ExamSuggestion("UGC NET", ExamCategory.EXAM_PREP),
    ExamSuggestion("CSIR NET", ExamCategory.EXAM_PREP),
    ExamSuggestion("Olympiad — Maths", ExamCategory.EXAM_PREP),
    ExamSuggestion("Olympiad — Science", ExamCategory.EXAM_PREP),
    ExamSuggestion("NTSE", ExamCategory.EXAM_PREP),
    ExamSuggestion("NCERT Class 10 Board", ExamCategory.EXAM_PREP),
    ExamSuggestion("NCERT Class 12 Board", ExamCategory.EXAM_PREP),

    // Job Prep — central/state government and banking recruitment exams
    ExamSuggestion("UPSC CSE (IAS)", ExamCategory.JOB_PREP),
    ExamSuggestion("UPSC EPFO", ExamCategory.JOB_PREP),
    ExamSuggestion("SSC-CGL", ExamCategory.JOB_PREP),
    ExamSuggestion("SSC-CHSL", ExamCategory.JOB_PREP),
    ExamSuggestion("SSC-MTS", ExamCategory.JOB_PREP),
    ExamSuggestion("SSC-GD Constable", ExamCategory.JOB_PREP),
    ExamSuggestion("SSC-JE", ExamCategory.JOB_PREP),
    ExamSuggestion("SSC Stenographer", ExamCategory.JOB_PREP),
    ExamSuggestion("RRB NTPC", ExamCategory.JOB_PREP),
    ExamSuggestion("RRB Group D", ExamCategory.JOB_PREP),
    ExamSuggestion("RRB ALP", ExamCategory.JOB_PREP),
    ExamSuggestion("RRB JE", ExamCategory.JOB_PREP),
    ExamSuggestion("IBPS PO", ExamCategory.JOB_PREP),
    ExamSuggestion("IBPS Clerk", ExamCategory.JOB_PREP),
    ExamSuggestion("IBPS RRB", ExamCategory.JOB_PREP),
    ExamSuggestion("SBI PO", ExamCategory.JOB_PREP),
    ExamSuggestion("SBI Clerk", ExamCategory.JOB_PREP),
    ExamSuggestion("RBI Grade B", ExamCategory.JOB_PREP),
    ExamSuggestion("RBI Assistant", ExamCategory.JOB_PREP),
    ExamSuggestion("LIC AAO", ExamCategory.JOB_PREP),
    ExamSuggestion("LIC ADO", ExamCategory.JOB_PREP),
    ExamSuggestion("Indian Army GD", ExamCategory.JOB_PREP),
    ExamSuggestion("Indian Navy SSR", ExamCategory.JOB_PREP),
    ExamSuggestion("Indian Air Force X/Y", ExamCategory.JOB_PREP),
    ExamSuggestion("Delhi Police Constable", ExamCategory.JOB_PREP),
    ExamSuggestion("State PSC (General)", ExamCategory.JOB_PREP),
    ExamSuggestion("Patwari / Revenue Clerk", ExamCategory.JOB_PREP)
)

/** Question formats the AI can be asked to use — see FullTestConfig.formats. */
enum class QuestionFormat(val label: String, val apiValue: String) {
    MCQ("MCQ", "mcq"),
    NUMERICAL("Numerical", "numerical"),
    DESCRIPTIVE("Descriptive", "descriptive")
}

data class FullTestNegativeMarking(val enabled: Boolean, val correctMarks: Double, val incorrectMarks: Double)

data class FullTestChapter(
    val name: String,
    val weightagePercent: Int,
    val easyPercent: Int,
    val mediumPercent: Int,
    val hardPercent: Int
)

data class FullTestSubject(val name: String, val chapters: List<FullTestChapter>)

/** The AI's researched structure for a named exam — see FullTestRepository.researchExam(). */
data class FullTestExamPattern(
    val examName: String,
    val totalQuestions: Int,
    val totalMarks: Int,
    val durationMinutes: Int,
    val questionTypes: List<String>,
    val negativeMarking: FullTestNegativeMarking?,
    val subjects: List<FullTestSubject>
)

/** One row of the editable per-chapter table on the Configure step — seeded from
 *  FullTestExamPattern, then freely adjustable by the user (question count, difficulty split)
 *  before generating. [id] is just "subject::chapter", stable for LazyColumn keys. */
data class FullTestChapterConfig(
    val subject: String,
    val chapter: String,
    val questionCount: Int,
    val easyCount: Int,
    val mediumCount: Int,
    val hardCount: Int
) {
    val id: String get() = "$subject::$chapter"
}

/** One generated question — see FullTestRepository.generateFullTest(). Mirrors Question's shape
 *  closely enough to convert 1:1 once the user confirms which ones to keep (see
 *  FullTestViewModel.toQuestion()). */
data class FullTestGeneratedQuestion(
    val subject: String,
    val chapter: String,
    val difficulty: String,
    val format: String,
    val text: String,
    val options: List<Pair<String, Boolean>>,
    val numericalAnswer: String?,
    val explanation: String?
)
