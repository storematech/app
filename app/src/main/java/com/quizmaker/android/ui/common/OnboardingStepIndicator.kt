package com.quizmaker.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.quizmaker.android.core.theme.BrandIndigo

/**
 * A row of [totalSteps] dots, the first [currentStep] filled solid — shown across the brand-new-
 * account onboarding sequence (Create Account -> Phone -> Notifications -> Get Started) so a new
 * user can see the sequence is short and bounded, rather than an unknown-length chain of screens.
 * [activeColor]/[inactiveColor] default to the brand color but are overridable so a screen with a
 * colored hero background (e.g. CollectPhoneScreen) can pass a white-tinted pair instead.
 */
@Composable
fun OnboardingStepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = BrandIndigo,
    inactiveColor: Color = BrandIndigo.copy(alpha = 0.2f)
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (index < currentStep) activeColor else inactiveColor)
            )
        }
    }
}
