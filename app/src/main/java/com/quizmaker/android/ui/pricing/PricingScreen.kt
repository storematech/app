package com.quizmaker.android.ui.pricing

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SaleRedEnd
import com.quizmaker.android.core.theme.SaleRedLight
import com.quizmaker.android.core.theme.SaleRedStart
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.core.theme.WarningAmber
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.TrialStatus
import com.quizmaker.android.util.formatShortDate
import kotlinx.coroutines.delay
import kotlin.time.Clock

const val SUPPORT_WHATSAPP_URL = "https://wa.me/916364893005"

/**
 * The app's single "Plans" destination — reached from More > Plans, the Dashboard trial/sale
 * banners' "View Plans" CTA, and More's premium upsell/active banners alike, so there's exactly
 * one place this content lives. Shows a license summary for an already-premium account, or a plan
 * to buy otherwise; see [PricingViewModel.load].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingScreen(
    onNavigateBack: () -> Unit,
    viewModel: PricingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.paymentSuccess) {
        if (uiState.paymentSuccess) {
            snackbarHostState.showSnackbar("Payment successful — you're now Premium!")
            viewModel.dismissPaymentSuccess()
        }
    }
    LaunchedEffect(uiState.paymentError) {
        uiState.paymentError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissPaymentError()
        }
    }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Plans", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LoadingCrossfade(isLoading = uiState.isLoading, modifier = Modifier.padding(padding)) {
            when {
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize().padding(20.dp)) {
                    ErrorBanner(message = uiState.errorMessage!!)
                }
                uiState.isPremium -> LicenseDetailsContent(
                    userType = uiState.userType,
                    licenseExpiredDate = uiState.licenseExpiredDate,
                    onContactSupport = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_WHATSAPP_URL)))
                    }
                )
                uiState.plan != null -> BuyPlanContent(
                    uiState = uiState,
                    onBuyNow = {
                        val activity = context as? Activity ?: return@BuyPlanContent
                        viewModel.startCheckout(activity)
                    }
                )
            }
        }
    }
}

@Composable
private fun BuyPlanContent(uiState: PricingUiState, onBuyNow: () -> Unit) {
    val plan = uiState.plan ?: return
    val sale = uiState.activeSale
    val isSaleDay = sale != null

    // Ticks once a second only while a 15-minute sale window is actually running, purely to force
    // recomposition so the countdown text (and the automatic revert to the regular price once it
    // lapses) stay live without the user having to leave and re-enter the screen.
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(uiState.saleActivatedUntil) {
        val until = uiState.saleActivatedUntil ?: return@LaunchedEffect
        while (Clock.System.now() < until) {
            delay(1000)
            now = Clock.System.now()
        }
        now = Clock.System.now()
    }
    val remainingSeconds = uiState.saleActivatedUntil?.let { (it - now).inWholeSeconds }
    val isSalePriceLive = isSaleDay && remainingSeconds != null && remainingSeconds > 0
    val discountedAmount = uiState.discountedAmount()

    // Brief celebration badge right when the sale discount auto-applies (see
    // PricingViewModel.load()) — fires once per activation (keyed on saleActivatedUntil, not
    // isSalePriceLive, so it never re-triggers on every tick of the countdown) then fades itself
    // out; the price swap below animates independently.
    var justUnlocked by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.saleActivatedUntil) {
        if (uiState.saleActivatedUntil != null) {
            justUnlocked = true
            delay(1800)
            justUnlocked = false
        }
    }

    val accentColor = if (isSaleDay) SaleRedStart else BrandIndigo
    val headerBg = if (isSaleDay) SaleRedLight else BrandIndigoLight

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        TrialContextHeader(uiState.trialStatus)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .elevatedSurface(shape = RoundedCornerShape(24.dp), elevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    isSaleDay -> {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Brush.horizontalGradient(listOf(SaleRedStart, SaleRedEnd)))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                (sale?.name?.ifBlank { null } ?: "Sale Day") + (sale?.discountPercent?.takeIf { it > 0 }?.let { " — $it% OFF" } ?: ""),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                    uiState.badge.isNotBlank() -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(BrandIndigo)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(uiState.badge, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                }
                Text(
                    "${plan.label} ${plan.flag}",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text("Full premium access to Yuno LMS", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(20.dp))

                AnimatedVisibility(
                    visible = justUnlocked,
                    enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.6f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                    exit = fadeOut(tween(300)) + scaleOut(targetScale = 0.85f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Brush.horizontalGradient(listOf(SaleRedStart, SaleRedEnd)))
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Discount Unlocked!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                AnimatedContent(
                    targetState = isSalePriceLive,
                    transitionSpec = {
                        (fadeIn(tween(350)) + scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                            .togetherWith(fadeOut(tween(150)))
                    },
                    label = "salePrice"
                ) { salePriceLive ->
                    if (salePriceLive && discountedAmount != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${currencySymbol(plan.currency)}${plan.amount}",
                                    color = TextSecondary,
                                    fontSize = 18.sp,
                                    textDecoration = TextDecoration.LineThrough
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${currencySymbol(plan.currency)}$discountedAmount",
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 40.sp,
                                    color = SaleRedStart
                                )
                            }
                            Text("per ${plan.interval}", color = TextSecondary, fontSize = 14.sp)
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(SaleRedStart.copy(alpha = 0.12f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "Offer ends in ${formatCountdown(remainingSeconds ?: 0)}",
                                    color = SaleRedStart,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${currencySymbol(plan.currency)}${plan.amount}",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 44.sp,
                                color = TextPrimary
                            )
                            Text("per ${plan.interval}", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                uiState.features.forEach { feature ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(feature, color = TextPrimary, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onBuyNow,
                    enabled = !uiState.isProcessingPayment,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    if (uiState.isProcessingPayment) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (isSalePriceLive && discountedAmount != null) {
                                "Buy Now — ${currencySymbol(plan.currency)}$discountedAmount"
                            } else {
                                "Buy Now"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatCountdown(totalSeconds: Long): String {
    val clamped = totalSeconds.coerceAtLeast(0)
    val minutes = clamped / 60
    val seconds = clamped % 60
    return "%d:%02d".format(minutes, seconds)
}

/** Same "you already have a license" card that used to live on its own LicenseDetails screen — now
 *  just the other branch of this same Plans screen for a premium account. */
@Composable
private fun LicenseDetailsContent(userType: String?, licenseExpiredDate: String?, onContactSupport: () -> Unit) {
    val expiryLabel = formatShortDate(licenseExpiredDate)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .elevatedSurface(shape = RoundedCornerShape(24.dp), elevation = 6.dp)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(SuccessGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Congratulations 🎉",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = SuccessGreen
            )
            Spacer(Modifier.height(6.dp))
            Text(
                userType?.takeIf { it.isNotBlank() }?.let { "Premium Membership Activated ($it)" } ?: "Premium Membership Activated",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = TextPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Enjoy all premium features without any limits, with support.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (expiryLabel.isNotBlank()) {
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SuccessGreen)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text("License Valid Until: $expiryLabel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LicenseFeatureRow("Unlimited Quizzes")
                LicenseFeatureRow("Premium Support 24/7")
                LicenseFeatureRow("Advanced Features")
            }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.clickable(onClick = onContactSupport),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Need Help? Contact Support", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun LicenseFeatureRow(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SuccessGreen.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

/** Repeats the same trial-context messaging shown on the trial-started/trial-ended screens, so
 *  "why am I here" is never a mystery when a user lands on Plans from one of those. */
@Composable
private fun TrialContextHeader(trialStatus: TrialStatus) {
    if (trialStatus is TrialStatus.Premium) return

    val (icon, tint, headline) = when (trialStatus) {
        is TrialStatus.Active -> Triple(
            Icons.Default.Schedule,
            BrandIndigo,
            if (trialStatus.daysLeft <= 1) "Last day of your free trial" else "${trialStatus.daysLeft} days left in your free trial"
        )
        is TrialStatus.Extended -> Triple(
            Icons.Default.AutoAwesome,
            SuccessGreen,
            if (trialStatus.daysLeft <= 1) "Last day of your extended trial" else "Congratulations! ${trialStatus.daysLeft} days left in your extended trial"
        )
        else -> Triple(Icons.Default.LockClock, WarningAmber, "Your trial has ended")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .elevatedSurface(shape = RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(headline, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "But we're affordable 💙 You can buy a plan to support us — or even after your trial ends, keep using Yuno LMS free with basic features.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "— A message from our founder: great assessment tools shouldn't be locked behind an unaffordable price tag.",
            color = TextSecondary,
            fontSize = 11.sp,
            fontStyle = FontStyle.Italic
        )
    }
}

private fun currencySymbol(currencyCode: String): String = when (currencyCode.uppercase()) {
    "INR" -> "₹"
    "USD" -> "$"
    else -> "$currencyCode "
}
