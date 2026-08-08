package com.quizmaker.android.ui.more

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.BuildConfig
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.ErrorRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.common.DesktopBanner
import com.quizmaker.android.ui.common.PremiumActiveBanner
import com.quizmaker.android.ui.common.PremiumBanner
import com.quizmaker.android.ui.common.elevatedSurface

@Composable
fun MoreScreen(
    onOpenProfile: () -> Unit,
    onOpenResponses: () -> Unit,
    onOpenPricing: () -> Unit,
    onOpenLicenseDetails: () -> Unit,
    onOpenFaq: () -> Unit,
    onOpenImportQuestions: () -> Unit,
    onOpenComingSoon: (String) -> Unit,
    viewModel: MoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // The bottom nav bar's own Scaffold (NavGraph) already reserves the system nav-bar inset;
    // reserving it again here would leave a redundant empty strip above the tab bar.
    Scaffold(containerColor = AppBackground, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            DesktopBanner()
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .elevatedSurface(shape = RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenProfile)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(BrandIndigo),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.name.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(uiState.name.ifBlank { "Your account" }, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(uiState.email, color = TextSecondary, fontSize = 13.sp)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .elevatedSurface(shape = RoundedCornerShape(20.dp))
            ) {
                MoreRow(icon = Icons.Default.Person, label = "My Profile", onClick = onOpenProfile)
                RowDivider()
                MoreRow(icon = Icons.Default.FileDownload, label = "Import Questions", onClick = onOpenImportQuestions)
                RowDivider()
                MoreRow(icon = Icons.Default.ChatBubbleOutline, label = "Responses", onClick = onOpenResponses)
                RowDivider()
                MoreRow(icon = Icons.Default.CreditCard, label = "Plans", onClick = { onOpenComingSoon("Plans") })
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .elevatedSurface(shape = RoundedCornerShape(20.dp))
            ) {
                MoreRow(
                    icon = Icons.Default.SupportAgent,
                    label = "Get Help & Support",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/916364893006")))
                    }
                )
                RowDivider()
                MoreRow(icon = Icons.AutoMirrored.Filled.HelpOutline, label = "FAQ", onClick = onOpenFaq)
                RowDivider()
                MoreRow(
                    icon = Icons.Default.PrivacyTip,
                    label = "Privacy Policy",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://yunolms.com/privacy-policy")))
                    }
                )
                RowDivider()
                MoreRow(
                    icon = Icons.Default.Gavel,
                    label = "Terms & Conditions",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://yunolms.com/terms-and-conditions")))
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
            if (uiState.isPremium) {
                PremiumActiveBanner(onClick = onOpenLicenseDetails)
            } else {
                PremiumBanner(onClick = onOpenPricing)
            }

            Spacer(Modifier.height(36.dp))
            AppFooter()
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Zomato/Blinkit-style sign-off at the very bottom of the scroll — brand mark + tagline. */
@Composable
private fun AppFooter() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "World's Simplest Learning Management System",
            color = TextSecondary,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(BrandIndigo),
            contentAlignment = Alignment.Center
        ) {
            Text("Y", color = Color.White, fontFamily = PoppinsFamily, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text("Yuno LMS", color = TextPrimary, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Made with", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.Favorite, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text("for Educators", color = TextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text("v${BuildConfig.VERSION_NAME}", color = TextSecondary.copy(alpha = 0.6f), fontSize = 10.sp)
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = BorderGray, thickness = 1.dp)
}

@Composable
private fun MoreRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
    }
}
