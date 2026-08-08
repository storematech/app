package com.quizmaker.android.ui.trial

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.common.AppLoadingScreen
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.SuccessCheckmark
import com.quizmaker.android.ui.common.elevatedSurface

/**
 * One-time congrats interstitial the moment a free account's 7-day trial begins. "Start Journey"
 * plays a brief transition (reusing [AppLoadingScreen]'s branding) before handing off to
 * [onStartJourney], which the nav graph points at the AI tab rather than Dashboard.
 */
@Composable
fun TrialStartedScreen(
    onStartJourney: () -> Unit,
    viewModel: TrialStartedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Crossfade(targetState = uiState.isNavigating, label = "trial-started-transition") { navigating ->
        if (navigating) {
            AppLoadingScreen()
        } else {
            TrialStartedContent(onStartJourney = { viewModel.startJourney(onStartJourney) })
        }
    }
}

@Composable
private fun TrialStartedContent(onStartJourney: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        SuccessCheckmark()
        Spacer(Modifier.height(28.dp))
        Text(
            "Congratulations!",
            color = SuccessGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "YOUR TRIAL PERIOD STARTED",
            textAlign = TextAlign.Center,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            color = TextPrimary
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "But we're affordable 💙 You can buy a plan to support us — or even after your trial ends, keep using Yuno LMS free with basic features.",
            textAlign = TextAlign.Center,
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(28.dp))
        FounderMessageCard()
        Spacer(Modifier.height(32.dp))
        GradientButton(text = "Start Journey", onClick = onStartJourney, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FounderMessageCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Icon(Icons.Default.FormatQuote, contentDescription = null, tint = BrandIndigo.copy(alpha = 0.4f), modifier = Modifier)
        Spacer(Modifier.height(4.dp))
        Text(
            "We built Yuno LMS because great assessment tools shouldn't be locked behind an " +
                "unaffordable price tag. Try everything free for 7 days — if you love it and can " +
                "support us, we'd be grateful. If not, you can keep creating quizzes on our free plan, always.",
            color = TextPrimary,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            fontStyle = FontStyle.Italic
        )
        Spacer(Modifier.height(10.dp))
        Text("— The Yuno LMS Team", color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}
