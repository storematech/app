package com.quizmaker.android.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.aiquiz.AiQuizScreen
import com.quizmaker.android.ui.auth.ForgotPasswordScreen
import com.quizmaker.android.ui.auth.LoginScreen
import com.quizmaker.android.ui.auth.SignupScreen
import com.quizmaker.android.ui.common.ComingSoonScreen
import com.quizmaker.android.ui.dashboard.DashboardScreen
import com.quizmaker.android.ui.leaderboard.LeaderboardScreen
import com.quizmaker.android.ui.license.LicenseDetailsScreen
import com.quizmaker.android.ui.masterpaper.MasterPaperScreen
import com.quizmaker.android.ui.more.MoreScreen
import com.quizmaker.android.ui.pricing.PricingScreen
import com.quizmaker.android.ui.profile.ProfileScreen
import com.quizmaker.android.ui.questionbank.QuestionBankScreen
import com.quizmaker.android.ui.quizanalysis.QuizAnalysisScreen
import com.quizmaker.android.ui.quizcreate.CreateQuizScreen
import com.quizmaker.android.ui.quizdetail.QuizDetailScreen
import com.quizmaker.android.ui.quizdetailview.QuizDetailViewScreen
import com.quizmaker.android.ui.quizlist.QuizListScreen
import com.quizmaker.android.ui.responsedetail.ResponseDetailScreen
import com.quizmaker.android.ui.responses.ResponsesScreen
import com.quizmaker.android.ui.takequiz.TakeQuizScreen
import io.github.jan.supabase.auth.status.SessionStatus

private val authRoutes = setOf(Screen.Login.route, Screen.Signup.route, Screen.ForgotPassword.route)

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
fun QuizMakerNavGraph(navController: NavHostController = rememberNavController()) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val gate by sessionViewModel.gate.collectAsState()
    val sessionStatus by sessionViewModel.sessionStatus.collectAsState()

    if (gate == SessionGate.LOADING) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Keep navigation in sync with sign-in/sign-out that happens after first composition
    // (e.g. the user taps "Sign out" on Profile, or a login call succeeds).
    LaunchedEffect(sessionStatus) {
        val onAuthScreen = navController.currentDestination?.route in authRoutes
        when (sessionStatus) {
            is SessionStatus.Authenticated -> if (onAuthScreen) {
                navController.navigate(Screen.Dashboard.route) {
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
    val startDestination = if (gate == SessionGate.LOGGED_IN) {
        Screen.Dashboard.route
    } else {
        Screen.Login.route
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showBottomBar = bottomTabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                YunoBottomBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        val contentModifier = if (showBottomBar) Modifier.padding(innerPadding) else Modifier

        NavHost(navController = navController, startDestination = startDestination, modifier = contentModifier) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToSignup = { navController.navigate(Screen.Signup.route) },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) }
                )
            }
            composable(Screen.Signup.route) {
                SignupScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(onNavigateBack = { navController.popBackStack() })
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
                    onOpenComingSoon = { title -> navController.navigate(Screen.ComingSoon.createRoute(title)) },
                    onOpenMasterPaper = { quizId -> navController.navigate(Screen.MasterPaper.createRoute(quizId)) },
                    onOpenQuizAnalysis = { quizId -> navController.navigate(Screen.QuizAnalysis.createRoute(quizId)) },
                    onOpenQuizDetailView = { quizId -> navController.navigate(Screen.QuizDetailView.createRoute(quizId)) },
                    onEditQuiz = { quizId -> navController.navigate(Screen.EditQuiz.createRoute(quizId)) }
                )
            }
            composable(Screen.Questions.route) {
                QuestionBankScreen(
                    onOpenAi = { navController.navigate(Screen.AiQuiz.createRoute(source = "questions")) },
                    onCreateQuizFromSelection = { ids -> navController.navigate(Screen.CreateQuiz.createRoute(ids)) }
                )
            }
            composable(Screen.More.route) {
                MoreScreen(
                    onOpenProfile = { navController.navigate(Screen.Profile.route) },
                    onOpenResponses = { navController.navigate(Screen.Responses.route) },
                    onOpenPricing = { navController.navigate(Screen.Pricing.route) },
                    onOpenLicenseDetails = { navController.navigate(Screen.LicenseDetails.route) },
                    onOpenComingSoon = { title -> navController.navigate(Screen.ComingSoon.createRoute(title)) }
                )
            }
            composable(Screen.Pricing.route) {
                PricingScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPaymentSuccess = {
                        navController.navigate(Screen.LicenseDetails.route) {
                            popUpTo(Screen.Pricing.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.LicenseDetails.route) {
                LicenseDetailsScreen(onNavigateBack = { navController.popBackStack() })
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
                        navController.navigate(Screen.QuizDetail.createRoute(quizId)) {
                            popUpTo(Screen.Dashboard.route)
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
