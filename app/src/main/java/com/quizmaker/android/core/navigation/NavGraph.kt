package com.quizmaker.android.core.navigation

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.quizmaker.android.core.analytics.AnalyticsViewModel
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoDark
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.aiquiz.AiQuizScreen
import com.quizmaker.android.ui.auth.CollectPhoneScreen
import com.quizmaker.android.ui.auth.ForgotPasswordScreen
import com.quizmaker.android.ui.auth.LoginScreen
import com.quizmaker.android.ui.classdashboard.ClassDashboardScreen
import com.quizmaker.android.ui.classlist.ClassListScreen
import com.quizmaker.android.ui.classweaklearners.ClassWeakLearnersScreen
import com.quizmaker.android.ui.tools.ToolsScreen
import com.quizmaker.android.ui.tools.feedback.FeedbackFormListScreen
import com.quizmaker.android.ui.tools.feedback.FeedbackSubmissionsScreen
import com.quizmaker.android.ui.tools.onboarding.OnboardingFormListScreen
import com.quizmaker.android.ui.tools.onboarding.OnboardingSubmissionsScreen
import com.quizmaker.android.ui.tools.poll.PollListScreen
import com.quizmaker.android.ui.tools.poll.PollResultsScreen
import com.quizmaker.android.ui.tools.rsvp.RsvpEventListScreen
import com.quizmaker.android.ui.tools.rsvp.RsvpRegistrationsScreen
import com.quizmaker.android.ui.tools.voting.VotingListScreen
import com.quizmaker.android.ui.tools.voting.VotingResultsScreen
import com.quizmaker.android.ui.common.AlertHost
import com.quizmaker.android.ui.common.AppLoadingScreen
import com.quizmaker.android.ui.common.ComingSoonScreen
import com.quizmaker.android.ui.dashboard.DashboardScreen
import com.quizmaker.android.ui.faq.FaqScreen
import com.quizmaker.android.ui.importquestions.ImportQuestionsScreen
import com.quizmaker.android.ui.leaderboard.LeaderboardScreen
import com.quizmaker.android.ui.masterpaper.MasterPaperScreen
import com.quizmaker.android.ui.more.MoreScreen
import com.quizmaker.android.ui.notifications.NotificationPermissionScreen
import com.quizmaker.android.ui.pricing.PricingScreen
import com.quizmaker.android.ui.profile.ProfileScreen
import com.quizmaker.android.ui.questionbank.QuestionBankScreen
import com.quizmaker.android.ui.quizanalysis.QuizAnalysisScreen
import com.quizmaker.android.ui.quizcreate.CreateQuizScreen
import com.quizmaker.android.ui.quizcreated.QuizCreatedScreen
import com.quizmaker.android.ui.quizdetail.QuizDetailScreen
import com.quizmaker.android.ui.quizdetailview.QuizDetailViewScreen
import com.quizmaker.android.ui.quizlist.QuizListScreen
import com.quizmaker.android.ui.revision.RevisionScreen
import com.quizmaker.android.ui.responsedetail.ResponseDetailScreen
import com.quizmaker.android.ui.responses.ResponsesScreen
import com.quizmaker.android.ui.takequiz.TakeQuizScreen
import com.quizmaker.android.ui.trial.TrialEndedScreen
import com.quizmaker.android.ui.trial.TrialStartedScreen
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch

private val authRoutes = setOf(Screen.Login.route, Screen.ForgotPassword.route)

/** Where a resolved post-auth gate lands. Only ever called with LOGGED_IN/NEEDS_PHONE/TRIAL_STARTED/
 *  TRIAL_ENDED in practice — LOADING/LOGGED_OUT are handled separately by their callers. */
private fun SessionGate.toRoute(): String = when (this) {
    SessionGate.NEEDS_PHONE -> Screen.CollectPhone.route
    SessionGate.NEEDS_NOTIFICATION_PERMISSION -> Screen.NotificationPermission.route
    SessionGate.TRIAL_STARTED -> Screen.TrialStarted.route
    SessionGate.TRIAL_ENDED -> Screen.TrialEnded.route
    SessionGate.LOGGED_IN, SessionGate.LOGGED_OUT, SessionGate.LOADING -> Screen.Dashboard.route
}

// [route] is the destination's route *pattern* (used to detect the active tab); [navigateRoute]
// is what actually gets passed to navigate() — for AiQuiz that must be a resolved route since its
// pattern carries an unfilled {source} placeholder.
private data class BottomTab(
    val route: String,
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector,
    val navigateRoute: String = route
)

private val bottomTabs = listOf(
    BottomTab(Screen.AiQuiz.route, "AI", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, navigateRoute = Screen.AiQuiz.createRoute()),
    BottomTab(Screen.Dashboard.route, "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    BottomTab(Screen.QuizList.route, "Quizzes", Icons.AutoMirrored.Filled.Article, Icons.AutoMirrored.Outlined.Article),
    BottomTab(Screen.Questions.route, "Questions", Icons.AutoMirrored.Filled.Help, Icons.AutoMirrored.Filled.HelpOutline),
    BottomTab(Screen.More.route, "More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
)

@Composable
fun QuizMakerNavGraph(
    navController: NavHostController = rememberNavController(),
    pendingResponseId: String? = null,
    onConsumedPendingResponse: () -> Unit = {}
) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val analyticsViewModel: AnalyticsViewModel = hiltViewModel()
    val gate by sessionViewModel.gate.collectAsState()
    val sessionStatus by sessionViewModel.sessionStatus.collectAsState()

    if (gate == SessionGate.LOADING) {
        AppLoadingScreen()
        return
    }

    // A quiz-submission notification tap while the app was backgrounded/killed — only once we're
    // actually past every onboarding gate, so it can't jump ahead of phone collection / trial screens.
    LaunchedEffect(pendingResponseId, gate) {
        if (pendingResponseId != null && gate == SessionGate.LOGGED_IN) {
            navController.navigate(Screen.ResponseDetail.createRoute(pendingResponseId))
            onConsumedPendingResponse()
        }
    }

    // Keep navigation in sync with sign-in/sign-out that happens after first composition
    // (e.g. the user taps "Sign out" on Profile, or a login call succeeds).
    LaunchedEffect(sessionStatus) {
        val onAuthScreen = navController.currentDestination?.route in authRoutes
        when (sessionStatus) {
            is SessionStatus.Authenticated -> if (onAuthScreen) {
                // A fresh sign-in/sign-up — same gate check the cold-start gate below does, so a
                // brand-new or pre-existing account never reaches Dashboard without first passing
                // through phone collection / trial-started / trial-ended as appropriate.
                navController.navigate(sessionViewModel.resolvePostAuthGate().toRoute()) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is SessionStatus.NotAuthenticated -> if (!onAuthScreen) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            else -> Unit
        }
    }

    // gate was decided atomically (in the same coroutine step as the sessionStatus read below),
    // so this can't disagree with sessionStatus the way two independently-updating StateFlows could.
    val startDestination = if (gate == SessionGate.LOGGED_OUT) Screen.Login.route else gate.toRoute()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showBottomBar = bottomTabs.any { it.route == currentRoute }
    val isDashboard = currentRoute == Screen.Dashboard.route

    // One spot covers every screen's screen_view instead of each screen self-reporting — the raw
    // route pattern (e.g. "quiz_detail/{quizId}") is used as the screen name rather than a filled-in
    // route, so Firebase groups all instances of the same screen together regardless of which quiz/
    // response/etc. was open.
    LaunchedEffect(currentRoute) {
        currentRoute?.let { analyticsViewModel.logScreenView(it) }
    }

    // Dashboard manages its own status bar icon color dynamically (see DashboardScreen.kt — it
    // tracks its own banner's scroll position). Every other screen gets a single, centrally-owned
    // rule here instead: white icons over a solid brand-blue strip painted behind the status bar.
    // Previously this was set ad hoc per-screen (Login/CollectPhone each toggled it locally and
    // restored whatever the previous screen had left it as on dispose), which could drift out of
    // sync depending on navigation history — e.g. leaving a screen that never set it at all left
    // the status bar in whatever state an earlier screen happened to set.
    val view = LocalView.current
    val insetsController = remember(view) {
        (view.context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view) }
    }
    LaunchedEffect(isDashboard) {
        if (!isDashboard) insetsController?.isAppearanceLightStatusBars = false
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                YunoBottomBar(navController = navController, currentRoute = currentRoute)
            }
        },
        // Deliberately excludes the top inset: reserving it here would push every screen's
        // content below the status bar before it even renders, so a screen like Dashboard that
        // wants its own content to bleed behind the status bar never could, no matter what it
        // does internally. Screens with their own TopAppBar+Scaffold already reserve the top
        // inset correctly on their own; this only needs to keep content clear of the bottom
        // nav bar / system gesture bar.
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        val contentModifier = if (showBottomBar) Modifier.padding(innerPadding) else Modifier

        Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = startDestination, modifier = contentModifier) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) }
                )
            }
            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.CollectPhone.route) {
                val scope = rememberCoroutineScope()
                CollectPhoneScreen(
                    onSaved = {
                        // Re-run the same post-auth gate check used at cold start, rather than
                        // jumping straight to Dashboard — a brand-new account's trial starts the
                        // moment the phone number is saved, so this is what actually routes to
                        // TrialStarted immediately instead of only on the next app launch.
                        scope.launch {
                            navController.navigate(sessionViewModel.resolvePostAuthGate().toRoute()) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
            composable(Screen.NotificationPermission.route) {
                val scope = rememberCoroutineScope()
                NotificationPermissionScreen(
                    onContinue = {
                        // Same "re-run the gate check" pattern as CollectPhone — lands on
                        // TrialStarted/TrialEnded/Dashboard, whichever actually applies next.
                        scope.launch {
                            navController.navigate(sessionViewModel.resolvePostAuthGate().toRoute()) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
            composable(Screen.TrialStarted.route) {
                TrialStartedScreen(
                    onStartJourney = {
                        navController.navigate(Screen.AiQuiz.createRoute()) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.TrialEnded.route) {
                TrialEndedScreen(
                    onViewPlans = { navController.navigate(Screen.Pricing.route) },
                    onDismiss = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.AiQuiz.route,
                arguments = listOf(navArgument("source") { type = NavType.StringType; defaultValue = "" })
            ) {
                AiQuizScreen(
                    onNavigateToCreateQuiz = { ids ->
                        navController.navigate(Screen.CreateQuiz.createRoute(ids))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onOpenResponse = { responseId -> navController.navigate(Screen.ResponseDetail.createRoute(responseId)) },
                    onOpenQuizzes = {
                        navController.navigate(Screen.QuizList.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenQuestions = {
                        navController.navigate(Screen.Questions.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenResponses = { navController.navigate(Screen.Responses.route) },
                    onOpenPricing = { navController.navigate(Screen.Pricing.route) }
                )
            }
            composable(Screen.Responses.route) {
                ResponsesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenResponse = { responseId -> navController.navigate(Screen.ResponseDetail.createRoute(responseId)) }
                )
            }
            composable(Screen.QuizList.route) {
                QuizListScreen(
                    onOpenQuiz = { quizId -> navController.navigate(Screen.QuizDetailView.createRoute(quizId)) },
                    onCreateQuiz = { navController.navigate(Screen.CreateQuiz.createRoute()) },
                    onOpenAi = { navController.navigate(Screen.AiQuiz.createRoute(source = "quiz_list")) },
                    onViewLeaderboard = { quizId -> navController.navigate(Screen.Leaderboard.createRoute(quizId)) },
                    onOpenMasterPaper = { quizId -> navController.navigate(Screen.MasterPaper.createRoute(quizId)) },
                    onOpenQuizAnalysis = { quizId -> navController.navigate(Screen.QuizAnalysis.createRoute(quizId)) },
                    onOpenQuizDetailView = { quizId -> navController.navigate(Screen.QuizDetailView.createRoute(quizId)) },
                    onEditQuiz = { quizId -> navController.navigate(Screen.EditQuiz.createRoute(quizId)) },
                    onOpenPricing = { navController.navigate(Screen.Pricing.route) }
                )
            }
            composable(Screen.Questions.route) {
                QuestionBankScreen(
                    onOpenAi = { navController.navigate(Screen.AiQuiz.createRoute(source = "questions")) },
                    onCreateQuizFromSelection = { ids -> navController.navigate(Screen.CreateQuiz.createRoute(ids)) },
                    onOpenPricing = { navController.navigate(Screen.Pricing.route) }
                )
            }
            composable(Screen.More.route) {
                MoreScreen(
                    onOpenProfile = { navController.navigate(Screen.Profile.route) },
                    onOpenResponses = { navController.navigate(Screen.Responses.route) },
                    onOpenPricing = { navController.navigate(Screen.Pricing.route) },
                    onOpenFaq = { navController.navigate(Screen.Faq.route) },
                    onOpenImportQuestions = { navController.navigate(Screen.ImportQuestions.route) },
                    onOpenRevision = { navController.navigate(Screen.Revision.route) },
                    onOpenClasses = { navController.navigate(Screen.Classes.route) },
                    onOpenTools = { navController.navigate(Screen.Tools.route) }
                )
            }
            composable(Screen.Revision.route) {
                RevisionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onCreateRetest = { ids -> navController.navigate(Screen.CreateQuiz.createRoute(ids)) }
                )
            }
            composable(Screen.Classes.route) {
                ClassListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenClass = { classId -> navController.navigate(Screen.ClassDashboard.createRoute(classId)) }
                )
            }
            composable(Screen.ClassDashboard.route) {
                ClassDashboardScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenWeakLearners = { classId -> navController.navigate(Screen.ClassWeakLearners.createRoute(classId)) }
                )
            }
            composable(Screen.ClassWeakLearners.route) {
                ClassWeakLearnersScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Tools.route) {
                ToolsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenOnboardingForms = { navController.navigate(Screen.OnboardingForms.route) },
                    onOpenFeedbackForms = { navController.navigate(Screen.FeedbackForms.route) },
                    onOpenPolls = { navController.navigate(Screen.Polls.route) },
                    onOpenVoting = { navController.navigate(Screen.Voting.route) },
                    onOpenRsvpEvents = { navController.navigate(Screen.RsvpEvents.route) },
                    onOpenComingSoon = { title -> navController.navigate(Screen.ComingSoon.createRoute(title)) }
                )
            }
            composable(Screen.OnboardingForms.route) {
                OnboardingFormListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onViewSubmissions = { formId -> navController.navigate(Screen.OnboardingSubmissions.createRoute(formId)) }
                )
            }
            composable(Screen.OnboardingSubmissions.route) {
                OnboardingSubmissionsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.FeedbackForms.route) {
                FeedbackFormListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onViewSubmissions = { formId -> navController.navigate(Screen.FeedbackSubmissions.createRoute(formId)) }
                )
            }
            composable(Screen.FeedbackSubmissions.route) {
                FeedbackSubmissionsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Polls.route) {
                PollListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onViewResults = { pollId -> navController.navigate(Screen.PollResults.createRoute(pollId)) }
                )
            }
            composable(Screen.PollResults.route) {
                PollResultsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Voting.route) {
                VotingListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onViewResults = { campaignId -> navController.navigate(Screen.VotingResults.createRoute(campaignId)) }
                )
            }
            composable(Screen.VotingResults.route) {
                VotingResultsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.RsvpEvents.route) {
                RsvpEventListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onViewRegistrations = { eventId -> navController.navigate(Screen.RsvpRegistrations.createRoute(eventId)) }
                )
            }
            composable(Screen.RsvpRegistrations.route) {
                RsvpRegistrationsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Faq.route) {
                FaqScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.ImportQuestions.route) {
                ImportQuestionsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Pricing.route) {
                PricingScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.ComingSoon.route,
                arguments = listOf(navArgument("title") { type = NavType.StringType })
            ) { backStackEntry ->
                val title = backStackEntry.arguments?.getString("title") ?: "Coming soon"
                ComingSoonScreen(title = title, onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.CreateQuiz.route,
                arguments = listOf(navArgument("preselectedIds") { type = NavType.StringType; defaultValue = "" })
            ) {
                CreateQuizScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onQuizCreated = { quizId ->
                        navController.navigate(Screen.QuizCreated.createRoute(quizId)) {
                            popUpTo(Screen.Dashboard.route)
                        }
                    }
                )
            }
            composable(
                route = Screen.QuizCreated.route,
                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
            ) {
                QuizCreatedScreen(
                    onDone = {
                        // Explicitly the Quizzes tab (not just "back", which used to land on
                        // Dashboard) — same pattern the bottom nav bar itself uses.
                        navController.navigate(Screen.QuizList.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(
                route = Screen.EditQuiz.route,
                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
            ) {
                CreateQuizScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onQuizCreated = { navController.popBackStack() }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.QuizDetail.route,
                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
            ) {
                QuizDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onViewLeaderboard = { quizId -> navController.navigate(Screen.Leaderboard.createRoute(quizId)) },
                    onEditQuiz = { quizId -> navController.navigate(Screen.EditQuiz.createRoute(quizId)) }
                )
            }
            composable(
                route = Screen.Leaderboard.route,
                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
            ) {
                LeaderboardScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.MasterPaper.route,
                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
            ) {
                MasterPaperScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.QuizAnalysis.route,
                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
            ) {
                QuizAnalysisScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.QuizDetailView.route,
                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
            ) {
                QuizDetailViewScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onOpenResponse = { responseId -> navController.navigate(Screen.ResponseDetail.createRoute(responseId)) }
                )
            }
            composable(
                route = Screen.ResponseDetail.route,
                arguments = listOf(navArgument("responseId") { type = NavType.StringType })
            ) {
                ResponseDetailScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.TakeQuiz.route,
                arguments = listOf(navArgument("shareId") { type = NavType.StringType }),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "https://app.yunolms.com/take-quiz/{shareId}" },
                    navDeepLink { uriPattern = "https://yunolms.com/take-quiz/{shareId}" },
                    navDeepLink { uriPattern = "https://quiz-maker.online/take-quiz/{shareId}" },
                    navDeepLink { uriPattern = "quizmaker://take-quiz/{shareId}" }
                )
            ) {
                TakeQuizScreen(
                    onViewLeaderboard = { quizId ->
                        navController.navigate(Screen.Leaderboard.createRoute(quizId))
                    },
                    onFinished = {
                        // Reached either from a deep link (no back stack in our app) or from within
                        // the app (Quiz Detail's preview). Popping falls back to Dashboard either way.
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route)
                        }
                    }
                )
            }
        }

        // Paints the status bar area itself brand-blue on every screen except Dashboard (which
        // paints its own banner full-bleed behind the status bar instead) — drawn on top of the
        // NavHost content since most screens' own TopAppBar/background don't reach up that far.
        if (!isDashboard) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(BrandIndigoDark)
            )
        }

        AlertHost(modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun YunoBottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar(
        modifier = Modifier.shadow(elevation = 8.dp),
        containerColor = SurfaceWhite,
        tonalElevation = 0.dp
    ) {
        bottomTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.navigateRoute) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(if (selected) tab.filledIcon else tab.outlinedIcon, contentDescription = tab.label)
                },
                label = { Text(tab.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandIndigo,
                    selectedTextColor = BrandIndigo,
                    indicatorColor = BrandIndigoLight,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}
