package com.quizmaker.android.ui.common

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoDark
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.PremiumGoldEnd
import com.quizmaker.android.core.theme.PremiumGoldStart
import com.quizmaker.android.core.theme.SaleRedEnd
import com.quizmaker.android.core.theme.SaleRedStart
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.core.theme.WarningAmber

/** Promotional banner pointing at the Desktop app, shared by Dashboard and More. */
@Composable
fun DesktopBanner() {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BrandIndigoLight)
            .clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://yunolms.com")))
            }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Computer, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Yuno LMS is available on Desktop",
                color = BrandIndigoDark,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Unlock advanced features at yunolms.com",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open yunolms.com", tint = BrandIndigo, modifier = Modifier.size(20.dp))
    }
}

/**
 * Compact "Add Questions with AI" CTA for the Question Bank screen — same light-indigo,
 * medium-height card style as [DesktopBanner] (not the big saturated Dashboard hero banner).
 */
@Composable
fun AiQuestionsBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BrandIndigoLight)
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Add Questions with AI",
                color = BrandIndigoDark,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Generate questions from a topic, PDF, or photos",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(20.dp))
    }
}

/**
 * Informational highlight for the Quizzes screen — same light-indigo, medium-height card style
 * as [DesktopBanner]. Purely a feature callout (not clickable), same as Dashboard's welcome
 * feature icons.
 */
@Composable
fun QuizFeaturesBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BrandIndigoLight)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuizFeatureItem(icon = Icons.Default.EmojiEvents, label = "Leaderboard", modifier = Modifier.weight(1f))
        QuizFeatureItem(icon = Icons.Default.BarChart, label = "Analysis", modifier = Modifier.weight(1f))
        QuizFeatureItem(icon = Icons.Default.Print, label = "Print", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun QuizFeatureItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = BrandIndigoDark, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

/** Compact "Get Premium" upsell banner — deliberately smaller than [DesktopBanner]. */
@Composable
fun PremiumBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(PremiumGoldStart, PremiumGoldEnd)))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Get Premium", color = Color.White, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(1.dp))
            Text("Unlock advanced reports and more", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
    }
}

/** Compact sale-day variant of [PremiumBanner] — same slot/shape, red instead of gold, shown on
 *  More whenever a `sale_day` window is live for a non-premium account. */
@Composable
fun SaleDayMoreBanner(saleName: String, discountPercent: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(SaleRedStart, SaleRedEnd)))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                saleName.ifBlank { "Sale Day" } + if (discountPercent > 0) " — $discountPercent% OFF" else "",
                color = Color.White,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(1.dp))
            Text("Grab a premium license at a special price", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
    }
}

/**
 * Replaces the Dashboard's usual welcome banner while a `sale_day` row's window is live.
 * Full-bleed to the screen edges and behind the status bar (Zomato/Blinkit-style header) —
 * the background paints edge-to-edge while [Modifier.windowInsetsPadding] pushes the actual
 * content below the status bar icons. Flat top / rounded bottom so it reads as a header, not a card.
 * A large, faint price-tag watermark plus a couple of sparkle accents stand in for a "luxury sale"
 * motif instead of literal device icons, which read as "cross-platform" rather than "sale."
 */
@Composable
fun SaleDayBanner(saleName: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(Brush.horizontalGradient(listOf(SaleRedStart, SaleRedEnd)))
            .clickable(onClick = onClick)
    ) {
        Icon(
            Icons.Default.LocalOffer,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.12f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 28.dp, y = 6.dp)
                .size(150.dp)
                .rotate(-24f)
        )
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-36).dp, y = 54.dp)
                .size(16.dp)
        )
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-18).dp, y = (-64).dp)
                .size(11.dp)
        )

        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    saleName.ifBlank { "Premium License Sale" },
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Premium license sale is live — grab your license at a special price",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Get Licenses", color = SaleRedStart, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SaleRedStart, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * Dashboard hero banner for a free account mid-trial — same slot/shape as [SaleDayBanner] and the
 * plain Welcome banner, just with a days-left readout and a "View Plans" CTA instead of the
 * static feature icons.
 */
@Composable
fun TrialActiveBanner(daysLeft: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(Brush.horizontalGradient(listOf(BrandIndigoDark, BrandIndigo)))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Text("Welcome to Yuno LMS", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(
                if (daysLeft <= 1) "Last day of your free trial" else "$daysLeft days left in your free trial",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("View Plans", color = BrandIndigo, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * Dashboard hero banner for an account whose trial was manually extended by an admin
 * (`profiles.user_type = 'trial_extend'` — see [com.quizmaker.android.util.TrialStatus.Extended]).
 * Same slot/shape as [TrialActiveBanner]/[SaleDayBanner], but green→gold rather than indigo so the
 * extension reads as a bonus being announced, not routine trial-countdown messaging.
 */
@Composable
fun TrialExtendedBanner(daysLeft: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(Brush.horizontalGradient(listOf(SuccessGreen, PremiumGoldEnd)))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Congratulations! Your trial is extended", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (daysLeft <= 1) {
                    "Last day of your extended trial — buy a license to keep every feature unlocked"
                } else {
                    "$daysLeft days left — buy a license to keep every feature unlocked"
                },
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("View Plans", color = SuccessGreen, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Dashboard hero banner once a free account's trial window has lapsed — same slot as [TrialActiveBanner]. */
@Composable
fun TrialEndedBanner(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(WarningAmber)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LockClock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Your trial has ended", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "You can still use Yuno LMS free with basic features — upgrade anytime.",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("View Plans", color = WarningAmber, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Shown instead of [PremiumBanner] once the account has an active paid plan. */
@Composable
fun PremiumActiveBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SuccessGreen)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("You're a Premium customer", color = Color.White, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(1.dp))
            Text("Tap to view your license details", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
    }
}
