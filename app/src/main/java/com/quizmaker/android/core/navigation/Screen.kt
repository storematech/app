package com.quizmaker.android.core.navigation

/** Sealed set of navigable destinations. Plain string routes — simplest thing that works reliably. */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Signup : Screen("signup")
    data object ForgotPassword : Screen("forgot_password")

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

    /** Opened either from in-app navigation or a https://quiz-maker.online/take-quiz/{shareId} deep link. */
    data object TakeQuiz : Screen("take_quiz/{shareId}") {
        fun createRoute(shareId: String) = "take_quiz/$shareId"
    }
}
