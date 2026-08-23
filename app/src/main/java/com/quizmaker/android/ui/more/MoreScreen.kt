package com.quizmaker.android.ui.more

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.BuildConfig
import com.quizmaker.android.R
import com.quizmaker.android.core.prefs.AppThemeMode
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.ErrorRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.PremiumGoldEnd
import com.quizmaker.android.core.theme.PremiumGoldStart
import com.quizmaker.android.core.theme.SaleRedStart
import com.quizmaker.android.core.theme.StatAmberBg
import com.quizmaker.android.core.theme.StatAmberIcon
import com.quizmaker.android.core.theme.StatBlueBg
import com.quizmaker.android.core.theme.StatBlueIcon
import com.quizmaker.android.core.theme.StatGreenBg
import com.quizmaker.android.core.theme.StatGreenIcon
import com.quizmaker.android.core.theme.StatPurpleBg
import com.quizmaker.android.core.theme.StatPurpleIcon
import com.quizmaker.android.core.theme.StatRedBg
import com.quizmaker.android.core.theme.StatRedIcon
import com.quizmaker.android.core.theme.StatTealBg
import com.quizmaker.android.core.theme.StatTealIcon
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.common.BlurBehindDialog
import com.quizmaker.android.ui.common.DesktopBanner
import com.quizmaker.android.ui.common.PremiumActiveBanner
import com.quizmaker.android.ui.common.PremiumBanner
import com.quizmaker.android.ui.common.SaleDayMoreBanner
import com.quizmaker.android.ui.common.elevatedSurface

private enum class BottomBannerState { PREMIUM_ACTIVE, SALE_DAY, UPSELL }

@Composable
fun MoreScreen(
    onOpenProfile: () -> Unit,
    onOpenResponses: () -> Unit,
    onOpenReportedQuestions: () -> Unit,
    onOpenPricing: () -> Unit,
    onOpenFaq: () -> Unit,
    onOpenImportQuestions: () -> Unit,
    onOpenRevision: () -> Unit,
    onOpenClasses: () -> Unit,
    onOpenLearners: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: MoreViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeMode by themeViewModel.themeMode.collectAsState()
    var showThemePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // The bottom nav bar's own Scaffold (NavGraph) already reserves the system nav-bar inset, so
    // this only adds the top one — unlike Dashboard, this screen has no full-bleed banner to
    // manually paint behind the status bar, so without this the profile row rendered right
    // against it with no clearance.
    Scaffold(containerColor = AppBackground, contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top)) { padding ->
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
                MoreRow(icon = Icons.Default.Person, label = "My Profile", onClick = onOpenProfile, iconBg = BrandIndigoLight, iconTint = BrandIndigo)
                RowDivider()
                MoreRow(icon = Icons.Default.ChatBubbleOutline, label = "Responses", onClick = onOpenResponses, iconBg = StatBlueBg, iconTint = StatBlueIcon)
                RowDivider()
                MoreRow(icon = Icons.Default.Flag, label = "Reported Questions", onClick = onOpenReportedQuestions, iconBg = StatRedBg, iconTint = StatRedIcon)
                RowDivider()
                MoreRow(icon = Icons.AutoMirrored.Filled.MenuBook, label = "Revision", onClick = onOpenRevision, iconBg = StatPurpleBg, iconTint = StatPurpleIcon)
                RowDivider()
                MoreRow(icon = Icons.Default.School, label = "Classes", onClick = onOpenClasses, iconBg = StatTealBg, iconTint = StatTealIcon)
                RowDivider()
                MoreRow(icon = Icons.Default.Group, label = "Learners", onClick = onOpenLearners, iconBg = StatGreenBg, iconTint = StatGreenIcon)
                RowDivider()
                MoreRow(icon = Icons.Default.Construction, label = "Tools", onClick = onOpenTools, iconBg = StatAmberBg, iconTint = StatAmberIcon)
                RowDivider()
                MoreRow(
                    icon = Icons.Default.DarkMode,
                    label = "Theme",
                    value = themeMode.label(),
                    onClick = { showThemePicker = true }
                )
                RowDivider()
                MoreRow(icon = Icons.Default.Settings, label = "Settings", onClick = onOpenSettings)
                RowDivider()
                MoreRow(icon = Icons.Default.FileDownload, label = "Import Questions", onClick = onOpenImportQuestions, iconBg = StatBlueBg, iconTint = StatBlueIcon)
                RowDivider()
                MoreRow(
                    icon = Icons.Default.CreditCard,
                    label = "Plans",
                    badge = uiState.activeSale?.let { "SALE DAY" },
                    onClick = onOpenPricing,
                    iconTint = Color.White,
                    iconBrush = Brush.horizontalGradient(listOf(PremiumGoldStart, PremiumGoldEnd))
                )
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
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/916364893005")))
                    },
                    iconBg = StatGreenBg,
                    iconTint = StatGreenIcon
                )
                RowDivider()
                MoreRow(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    label = "FAQ And Documentation",
                    onClick = onOpenFaq,
                    iconBg = BrandIndigoLight,
                    iconTint = BrandIndigo
                )
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
            // Crossfade rather than an abrupt appear/switch — belt-and-braces so that even if this
            // ever flips (loading -> resolved, or a stale cached value -> the fresh one) it reads
            // as a smooth fade instead of a jarring "blink."
            val bannerState = when {
                uiState.isLoading -> null
                uiState.isPremium -> BottomBannerState.PREMIUM_ACTIVE
                uiState.activeSale != null -> BottomBannerState.SALE_DAY
                else -> BottomBannerState.UPSELL
            }
            Crossfade(targetState = bannerState, animationSpec = tween(220), label = "premium-banner") { state ->
                when (state) {
                    BottomBannerState.PREMIUM_ACTIVE -> PremiumActiveBanner(onClick = onOpenPricing)
                    BottomBannerState.SALE_DAY -> SaleDayMoreBanner(
                        saleName = uiState.activeSale?.name.orEmpty(),
                        discountPercent = uiState.activeSale?.discountPercent ?: 0,
                        onClick = onOpenPricing
                    )
                    BottomBannerState.UPSELL -> PremiumBanner(onClick = onOpenPricing)
                    null -> Spacer(Modifier.height(0.dp))
                }
            }

            Spacer(Modifier.height(36.dp))
            AppFooter()
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showThemePicker) {
        ThemePickerDialog(
            currentMode = themeMode,
            onSelect = { themeViewModel.setThemeMode(it) },
            onDismiss = { showThemePicker = false }
        )
    }
}

private fun AppThemeMode.label(): String = when (this) {
    AppThemeMode.LIGHT -> "Light"
    AppThemeMode.DARK -> "Dark"
    AppThemeMode.SYSTEM -> "System default"
}

@Composable
private fun ThemePickerDialog(currentMode: AppThemeMode, onSelect: (AppThemeMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = { BlurBehindDialog(); Text("Theme", color = TextPrimary, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                AppThemeMode.entries.forEach { mode ->
                    ThemeModeRow(label = mode.label(), selected = currentMode == mode, onClick = { onSelect(mode) })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = BrandIndigo, fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
private fun ThemeModeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = BrandIndigo, unselectedColor = TextSecondary)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, color = TextPrimary, fontSize = 15.sp)
    }
}

/** Sign-off at the very bottom of the scroll — tagline + social links, kept plain/monochrome
 *  (no brand mark or colored logo) the way larger apps' footers tend to. */
@Composable
private fun AppFooter() {
    val context = LocalContext.current
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
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SocialIconButton(
                painter = painterResource(R.drawable.ic_instagram),
                contentDescription = "Instagram",
                onClick = { openUrl(context, "https://www.instagram.com/yunolms/") }
            )
            SocialIconButton(
                painter = painterResource(R.drawable.ic_youtube),
                contentDescription = "YouTube",
                onClick = { openUrl(context, "https://www.youtube.com/@YunoLMS") }
            )
            SocialIconButton(
                painter = painterResource(R.drawable.ic_whatsapp),
                contentDescription = "WhatsApp",
                onClick = { openUrl(context, "https://wa.me/916364893005") }
            )
            SocialIconButton(
                icon = Icons.Default.Public,
                contentDescription = "Website",
                onClick = { openInChrome(context, "https://yunolms.com") }
            )
        }
        Spacer(Modifier.height(18.dp))

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
private fun SocialIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    icon: ImageVector? = null
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(BorderGray)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (painter != null) {
            Icon(painter = painter, contentDescription = contentDescription, tint = TextSecondary, modifier = Modifier.size(19.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = contentDescription, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

/** Forces Chrome specifically (per request) for the website link, falling back to whatever
 *  handles it if Chrome isn't installed. */
private fun openInChrome(context: android.content.Context, url: String) {
    val chromeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { setPackage("com.android.chrome") }
    try {
        context.startActivity(chromeIntent)
    } catch (e: ActivityNotFoundException) {
        openUrl(context, url)
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = BorderGray, thickness = 1.dp)
}

/**
 * [iconBg]/[iconTint] default to the same neutral pairing every row used before this became
 * colorful — pass one of the Stat*Bg/Stat*Icon pairs (or [iconBrush] for a gradient chip, e.g.
 * Plans' gold) to give a row its own accent, same "colored icon chip" language as Dashboard's
 * StatTile.
 */
@Composable
private fun MoreRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    badge: String? = null,
    value: String? = null,
    iconBg: Color = BorderGray,
    iconTint: Color = TextSecondary,
    iconBrush: Brush? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .then(if (iconBrush != null) Modifier.background(iconBrush) else Modifier.background(iconBg)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(value, color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
        }
        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SaleRedStart)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(badge, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
            Spacer(Modifier.width(8.dp))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
    }
}
