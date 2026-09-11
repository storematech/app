package com.quizmaker.android.ui.appintro

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.absoluteValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AiGradientEnd
import com.quizmaker.android.core.theme.AiGradientMid
import com.quizmaker.android.core.theme.AiGradientStart
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.StatAmberBg
import com.quizmaker.android.core.theme.StatAmberIcon
import com.quizmaker.android.core.theme.StatPurpleBg
import com.quizmaker.android.core.theme.StatPurpleIcon
import com.quizmaker.android.core.theme.StatTealBg
import com.quizmaker.android.core.theme.StatTealIcon
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.OnboardingStepIndicator
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class AppIntroSlide(
    val icon: ImageVector,
    val accent: Color,
    val accentBg: Color,
    val title: String,
    val description: String
)

// Not a top-level val: accentBg/BrandIndigoLight below are theme-aware `@Composable get()` tokens
// (they read LocalDarkTheme.current), so this list has to be built from inside a @Composable body
// rather than once at class-init time — otherwise it could never react to a theme switch anyway.
@Composable
private fun appIntroSlides(): List<AppIntroSlide> = listOf(
    AppIntroSlide(
        icon = Icons.Default.QrCodeScanner,
        accent = StatPurpleIcon,
        accentBg = StatPurpleBg,
        title = "Scan any paper, create a quiz",
        description = "Snap a photo of any paper or worksheet with Scanner, and turn it into a ready-to-share quiz in seconds."
    ),
    AppIntroSlide(
        icon = Icons.AutoMirrored.Filled.MenuBook,
        accent = BrandIndigo,
        accentBg = BrandIndigoLight,
        title = "Lakhs of questions, ready to go",
        description = "Full Test comes pre-loaded with lakhs of curated questions across every major exam — pick one and start."
    ),
    AppIntroSlide(
        icon = Icons.Default.Description,
        accent = StatAmberIcon,
        accentBg = StatAmberBg,
        title = "More than just quizzes",
        description = "Generate a printable, offline exam question paper for any topic — ready to hand out on paper."
    ),
    AppIntroSlide(
        icon = Icons.Default.School,
        accent = StatTealIcon,
        accentBg = StatTealBg,
        title = "Classes, feedback & onboarding",
        description = "Group learners into classes, collect feedback, and run onboarding forms — all in one place."
    )
)

/**
 * One-time, full-screen "what Yuno LMS can do" experience shown to every brand-new account — see
 * SessionViewModel.resolvePostAuthGate() for exactly when (after phone collection and notification
 * permission, before the trial/Dashboard landing) and AppIntroPrefs for the once-per-account+device
 * bookkeeping. Two phases: an animated, wordless-until-now "welcome" beat (see
 * [AppIntroWelcomeScreen]) that sets a premium tone, then the swipeable feature-slide carousel (see
 * [AppIntroSlides]). "Skip" and reaching the last slide both dismiss it for good.
 */
@Composable
fun AppIntroScreen(
    onFinished: () -> Unit,
    viewModel: AppIntroViewModel = hiltViewModel()
) {
    var showWelcome by remember { mutableStateOf(true) }
    if (showWelcome) {
        AppIntroWelcomeScreen(onContinue = { showWelcome = false })
    } else {
        AppIntroSlides(onFinished = onFinished, viewModel = viewModel)
    }
}

/**
 * A short, animated beat before the feature slides — bold text builds line by line over the same
 * blurred-gradient-blob backdrop FullTestScreen uses for its "premium AI product" surface, then
 * auto-advances into [AppIntroSlides]. Tapping anywhere skips ahead immediately for anyone who
 * doesn't want to wait out the animation.
 */
@Composable
private fun AppIntroWelcomeScreen(onContinue: () -> Unit) {
    val line1 = remember { Animatable(0f) }
    val line2 = remember { Animatable(0f) }
    val line3Alpha = remember { Animatable(0f) }
    val line3Scale = remember { Animatable(0.82f) }

    LaunchedEffect(Unit) {
        line1.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
        delay(450)
        line2.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
        delay(550)
        coroutineScope {
            launch { line3Alpha.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
            launch { line3Scale.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
        }
        delay(1500)
        onContinue()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onContinue)
    ) {
        WelcomeAuraBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Take a deep breath.",
                textAlign = TextAlign.Center,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = TextPrimary,
                modifier = Modifier.alpha(line1.value).offset(y = ((1f - line1.value) * 12).dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "We're introducing you to",
                textAlign = TextAlign.Center,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = TextPrimary,
                modifier = Modifier.alpha(line2.value).offset(y = ((1f - line2.value) * 12).dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "YunoLMS",
                textAlign = TextAlign.Center,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 42.sp,
                style = TextStyle(brush = Brush.horizontalGradient(listOf(AiGradientStart, AiGradientMid, AiGradientEnd))),
                modifier = Modifier
                    .alpha(line3Alpha.value)
                    .scale(line3Scale.value)
            )
        }
    }
}

/** Same 4-corner blurred-blob backdrop as FullTestScreen's PremiumAuraBackground, duplicated here
 *  (rather than exported cross-screen) since this is the only other place that wants it — kept in
 *  sync by eye if that palette ever changes. */
@Composable
private fun WelcomeAuraBackground() {
    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        WelcomeAuraBlob(color = Color(0xFF4C8DF6), modifier = Modifier.align(Alignment.TopStart).offset(x = (-70).dp, y = (-70).dp))
        WelcomeAuraBlob(color = Color(0xFF9C6ADE), modifier = Modifier.align(Alignment.TopEnd).offset(x = 70.dp, y = (-70).dp))
        WelcomeAuraBlob(color = Color(0xFFF76CA6), modifier = Modifier.align(Alignment.BottomStart).offset(x = (-70).dp, y = 70.dp))
        WelcomeAuraBlob(color = Color(0xFFFDB750), modifier = Modifier.align(Alignment.BottomEnd).offset(x = 70.dp, y = 70.dp))
    }
}

@Composable
private fun WelcomeAuraBlob(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(240.dp)
            .blur(90.dp)
            .background(
                Brush.radialGradient(colors = listOf(color.copy(alpha = 0.4f), Color.Transparent)),
                shape = CircleShape
            )
    )
}

@Composable
private fun AppIntroSlides(
    onFinished: () -> Unit,
    viewModel: AppIntroViewModel
) {
    val slides = appIntroSlides()
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    fun finish(skipped: Boolean) {
        viewModel.markIntroShown(skipped = skipped, reachedSlide = pagerState.currentPage + 1, totalSlides = slides.size)
        onFinished()
    }

    // Ties the whole screen to the current slide's color — the glow wash below and the step dots
    // both cross-fade to it as you swipe, so the carousel doesn't feel like a flat white form.
    val currentAccent by animateColorAsState(
        targetValue = slides[pagerState.currentPage.coerceIn(slides.indices)].accent,
        animationSpec = tween(450),
        label = "introAccent"
    )

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .align(Alignment.TopCenter)
                .blur(120.dp)
                .background(
                    Brush.radialGradient(colors = listOf(currentAccent.copy(alpha = 0.24f), Color.Transparent))
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.End) {
                Text(
                    "Skip",
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { finish(skipped = true) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                AppIntroSlideContent(slides[page], pageOffset)
            }

            OnboardingStepIndicator(
                currentStep = pagerState.currentPage + 1,
                totalSteps = slides.size,
                activeColor = currentAccent,
                inactiveColor = TextSecondary.copy(alpha = 0.22f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(24.dp))

            val isLastSlide = pagerState.currentPage == slides.lastIndex
            GradientButton(
                text = if (isLastSlide) "Get Started" else "Next",
                onClick = {
                    if (isLastSlide) {
                        finish(skipped = false)
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** [pageOffset] is 0 for the fully-centered page, ±1 for a fully-adjacent one — drives a subtle
 *  scale/fade so the carousel has depth as you drag between slides instead of a hard cut. */
@Composable
private fun AppIntroSlideContent(slide: AppIntroSlide, pageOffset: Float) {
    val focus = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
    val scale = lerp(0.88f, 1f, focus)
    val fade = lerp(0.4f, 1f, focus)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(scaleX = scale, scaleY = scale, alpha = fade)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Soft blurred glow sitting behind the icon tile — same "premium aura" language as the
            // welcome beat's WelcomeAuraBlob, just quieter and tied to this slide's own accent.
            Box(
                modifier = Modifier
                    .size(156.dp)
                    .blur(56.dp)
                    .background(
                        Brush.radialGradient(colors = listOf(slide.accent.copy(alpha = 0.38f), Color.Transparent)),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Brush.linearGradient(colors = listOf(slide.accent.copy(alpha = 0.24f), slide.accentBg)))
                    .border(1.dp, slide.accent.copy(alpha = 0.3f), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(slide.icon, contentDescription = null, tint = slide.accent, modifier = Modifier.size(48.dp))
            }
        }
        Spacer(Modifier.height(36.dp))
        Text(
            slide.title,
            textAlign = TextAlign.Center,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 25.sp,
            lineHeight = 31.sp,
            color = TextPrimary
        )
        Spacer(Modifier.height(14.dp))
        Text(
            slide.description,
            textAlign = TextAlign.Center,
            color = TextSecondary,
            fontSize = 14.5.sp,
            lineHeight = 21.sp
        )
    }
}
