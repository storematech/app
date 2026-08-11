package com.quizmaker.android.core.navigation

/** Sealed set of navigable destinations. Plain string routes — simplest thing that works reliably. */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object ForgotPassword : Screen("forgot_password")
    data object CollectPhone : Screen("collect_phone")
    data object NotificationPermission : Screen("notification_permission")
    data object TrialStarted : Screen("trial_started")
    data object TrialEnded : Screen("trial_ended")

    data object AiQuiz : Screen("ai_quiz?source={source}") {
        /** [source] = "questions" when launched from the Question Bank's AI button (Add Questions mode). */
        fun createRoute(source: String? = null): String = "ai_quiz?source=${source.orEmpty()}"
    }
    data object Dashboard : Screen("dashboard")
    data object QuizList : Screen("quiz_list")
    data object Questions : Screen("questions")
    data object More : Screen("more")
    data object Profile : Screen("profile")

    data object QuizDetail : Screen("quiz_detail/{quizId}") {
        fun createRoute(quizId: String) = "quiz_detail/$quizId"
    }

    /** Shown once, right after creating a brand-new quiz — QR/share/export "flyer," not the management screen (that's QuizDetail). */
    data object QuizCreated : Screen("quiz_created/{quizId}") {
        fun createRoute(quizId: String) = "quiz_created/$quizId"
    }

    data object CreateQuiz : Screen("create_quiz?preselectedIds={preselectedIds}") {
        /** [preselectedIds] pre-checks these questions on the Questions step — used when arriving from the AI quiz flow. */
        fun createRoute(preselectedIds: List<String> = emptyList()): String =
            "create_quiz?preselectedIds=${preselectedIds.joinToString(",")}"
    }

    data object EditQuiz : Screen("edit_quiz/{quizId}") {
        fun createRoute(quizId: String) = "edit_quiz/$quizId"
    }

    data object ComingSoon : Screen("coming_soon/{title}") {
        fun createRoute(title: String) = "coming_soon/$title"
    }

    data object Leaderboard : Screen("leaderboard/{quizId}") {
        fun createRoute(quizId: String) = "leaderboard/$quizId"
    }

    data object MasterPaper : Screen("master_paper/{quizId}") {
        fun createRoute(quizId: String) = "master_paper/$quizId"
    }

    data object QuizAnalysis : Screen("quiz_analysis/{quizId}") {
        fun createRoute(quizId: String) = "quiz_analysis/$quizId"
    }

    data object QuizDetailView : Screen("quiz_detail_view/{quizId}") {
        fun createRoute(quizId: String) = "quiz_detail_view/$quizId"
    }

    data object ResponseDetail : Screen("response_detail/{responseId}") {
        fun createRoute(responseId: String) = "response_detail/$responseId"
    }

    data object Responses : Screen("responses")
    data object Revision : Screen("revision")

    data object Classes : Screen("classes")

    data object ClassDashboard : Screen("class_dashboard/{classId}") {
        fun createRoute(classId: String) = "class_dashboard/$classId"
    }

    data object ClassWeakLearners : Screen("class_weak_learners/{classId}") {
        fun createRoute(classId: String) = "class_weak_learners/$classId"
    }

    data object Tools : Screen("tools")
    data object OnboardingForms : Screen("onboarding_forms")

    data object OnboardingSubmissions : Screen("onboarding_submissions/{formId}") {
        fun createRoute(formId: String) = "onboarding_submissions/$formId"
    }

    data object FeedbackForms : Screen("feedback_forms")

    data object FeedbackSubmissions : Screen("feedback_submissions/{formId}") {
        fun createRoute(formId: String) = "feedback_submissions/$formId"
    }

    data object Polls : Screen("polls")

    data object PollResults : Screen("poll_results/{pollId}") {
        fun createRoute(pollId: String) = "poll_results/$pollId"
    }

    data object Voting : Screen("voting")

    data object VotingResults : Screen("voting_results/{campaignId}") {
        fun createRoute(campaignId: String) = "voting_results/$campaignId"
    }

    data object RsvpEvents : Screen("rsvp_events")

    data object RsvpRegistrations : Screen("rsvp_registrations/{eventId}") {
        fun createRoute(eventId: String) = "rsvp_registrations/$eventId"
    }

    data object Pricing : Screen("pricing")
    data object Faq : Screen("faq")
    data object ImportQuestions : Screen("import_questions")

    /** Opened either from in-app navigation or a https://app.yunolms.com/take-quiz/{shareId} deep link (older yunolms.com/quiz-maker.online links still work too — see NavGraph.kt). */
    data object TakeQuiz : Screen("take_quiz/{shareId}") {
        fun createRoute(shareId: String) = "take_quiz/$shareId"
    }
}
