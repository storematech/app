package com.quizmaker.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.quizmaker.android.core.navigation.QuizMakerNavGraph
import com.quizmaker.android.core.theme.QuizMakerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Held so onNewIntent() (fired for the singleTask launch mode when a quiz share link is
    // tapped while the app is already running) can hand the link off to the existing NavController.
    // The initial cold-start deep link is handled automatically by rememberNavController()/NavHost.
    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuizMakerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val controller = rememberNavController()
                    navController = controller
                    QuizMakerNavGraph(navController = controller)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navController?.handleDeepLink(intent)
    }
}
