package com.quizmaker.android

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.quizmaker.android.core.navigation.QuizMakerNavGraph
import com.quizmaker.android.core.payment.RazorpayResult
import com.quizmaker.android.core.payment.RazorpayResultBus
import com.quizmaker.android.core.theme.QuizMakerTheme
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    // Held so onNewIntent() (fired for the singleTask launch mode when a quiz share link is
    // tapped while the app is already running) can hand the link off to the existing NavController.
    // The initial cold-start deep link is handled automatically by rememberNavController()/NavHost.
    private var navController: NavHostController? = null

    // Razorpay's Checkout SDK calls back to whichever Activity launched it — this app is
    // single-Activity, so MainActivity implements that interface and just forwards the result
    // into a shared bus that PricingViewModel (wherever the checkout was actually started) reacts to.
    @Inject
    lateinit var razorpayResultBus: RazorpayResultBus

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate() — applies Theme.QuizMaker.Splash's branded splash
        // (blue background + the "Y" mark) and then switches to Theme.QuizMaker once ready.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Default enableEdgeToEdge() draws a translucent white/dark scrim behind the status bar
        // for contrast, which washes out full-bleed colored headers like Dashboard's banner into
        // a muddy gray strip. Screens that go under the status bar (Dashboard) provide their own
        // contrast via their background color + light/dark icon toggling, so no scrim is needed.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        // Belt-and-braces: on API 29+ the system can still paint its own translucent contrast
        // scrim behind a transparent status/nav bar regardless of the style above, which is
        // exactly what was washing Dashboard's colored banner out to white/gray on some devices.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
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

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val paymentId = razorpayPaymentId ?: paymentData?.paymentId
        if (paymentId.isNullOrBlank()) {
            onPaymentError(0, "Razorpay reported success without a payment id.", paymentData)
            return
        }
        lifecycleScope.launch {
            razorpayResultBus.emit(
                RazorpayResult.Success(orderId = paymentData?.orderId, paymentId = paymentId, signature = paymentData?.signature)
            )
        }
    }

    override fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) {
        lifecycleScope.launch {
            razorpayResultBus.emit(RazorpayResult.Failure(code = code, description = description))
        }
    }
}
