package com.quizmaker.android.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.util.ReportDesign
import com.quizmaker.android.util.ReportTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDesignScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportDesignViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCustomPicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val (bytes, contentType) = withContext(Dispatchers.IO) {
                val type = context.contentResolver.getType(uri) ?: "image/jpeg"
                val data = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                data to type
            }
            if (bytes != null) {
                viewModel.uploadLogo(bytes, contentType)
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    LaunchedEffect(uiState.brandingSaveSuccess) {
        if (uiState.brandingSaveSuccess) {
            snackbarHostState.showSnackbar("Business details saved")
        }
    }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Report Design", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LoadingCrossfade(isLoading = uiState.isLoading, modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text("Sample Report Preview", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("A live look at your reports with the template and color below.", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                SampleReportPreview(template = uiState.template, accentColorHex = uiState.colorHex)

                Spacer(Modifier.height(28.dp))

                Text("Report Template", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Applies to every PDF export — Leaderboard, Responses, Master Paper, and more.", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                        TemplatePreviewCard(
                            template = ReportTemplate.MODERN,
                            isSelected = uiState.template == ReportTemplate.MODERN,
                            accentColorHex = uiState.colorHex,
                            onClick = { viewModel.onTemplateSelected(ReportTemplate.MODERN) },
                            modifier = Modifier.weight(1f)
                        )
                        TemplatePreviewCard(
                            template = ReportTemplate.CLASSIC,
                            isSelected = uiState.template == ReportTemplate.CLASSIC,
                            accentColorHex = uiState.colorHex,
                            onClick = { viewModel.onTemplateSelected(ReportTemplate.CLASSIC) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                        TemplatePreviewCard(
                            template = ReportTemplate.BOLD,
                            isSelected = uiState.template == ReportTemplate.BOLD,
                            accentColorHex = uiState.colorHex,
                            onClick = { viewModel.onTemplateSelected(ReportTemplate.BOLD) },
                            modifier = Modifier.weight(1f)
                        )
                        TemplatePreviewCard(
                            template = ReportTemplate.MINIMAL,
                            isSelected = uiState.template == ReportTemplate.MINIMAL,
                            accentColorHex = uiState.colorHex,
                            onClick = { viewModel.onTemplateSelected(ReportTemplate.MINIMAL) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                Text("Accent Color", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Spacer(Modifier.height(14.dp))
                ColorGrid(
                    selectedHex = uiState.colorHex,
                    onSelect = viewModel::onColorSelected,
                    onCustomClick = { showCustomPicker = true }
                )

                Spacer(Modifier.height(28.dp))

                Text("Business Branding", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Shown as the letterhead at the top of every PDF export.", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BorderGray, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.businessLogoUrl != null) {
                            AsyncImage(
                                model = uiState.businessLogoUrl,
                                contentDescription = "Business logo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(6.dp)
                            )
                        } else {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = TextSecondary)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        OutlinedButton(
                            onClick = { logoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            enabled = !uiState.isUploadingLogo
                        ) {
                            if (uiState.isUploadingLogo) {
                                CircularProgressIndicator(modifier = Modifier.height(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text(if (uiState.businessLogoUrl != null) "Change logo" else "Upload logo")
                            }
                        }
                        if (uiState.businessLogoUrl != null) {
                            TextButton(onClick = viewModel::removeLogo, enabled = !uiState.isUploadingLogo) {
                                Text("Remove")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = uiState.businessName,
                    onValueChange = viewModel::onBusinessNameChange,
                    label = { Text("Business Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.address,
                    onValueChange = viewModel::onAddressChange,
                    label = { Text("Address") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(90.dp)
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = viewModel::saveBranding,
                    enabled = !uiState.isSavingBranding,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (uiState.isSavingBranding) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Save Business Details")
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showCustomPicker) {
        ColorPickerDialog(
            initialHex = uiState.colorHex,
            onConfirm = { hex ->
                viewModel.onColorSelected(hex)
                showCustomPicker = false
            },
            onDismiss = { showCustomPicker = false }
        )
    }
}

private val ReportTemplate.displayName: String
    get() = when (this) {
        ReportTemplate.MODERN -> "Modern"
        ReportTemplate.CLASSIC -> "Classic"
        ReportTemplate.BOLD -> "Bold"
        ReportTemplate.MINIMAL -> "Minimal"
    }

@Composable
private fun TemplatePreviewCard(
    template: ReportTemplate,
    isSelected: Boolean,
    accentColorHex: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = remember(accentColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(accentColorHex)) }.getOrDefault(Color(ReportDesign.DEFAULT_ACCENT_COLOR))
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceWhite)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else BorderGray,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
        ) {
            drawMockReport(template, accentColor)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(template.displayName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
        }
        if (isSelected) {
            Text("Default", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Miniature stand-in for a real exported table: a header + two rows, styled the same way the
 *  actual *PdfExporters style theirs — see LeaderboardPdfExporter.kt for the real-size version. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMockReport(template: ReportTemplate, accent: Color) {
    val headerHeight = size.height * 0.3f
    val barColor = Color(0xFF9CA3AF)
    val neutralLine = Color(0xFFE5E7EB)
    val headerFilled = template == ReportTemplate.MODERN || template == ReportTemplate.BOLD
    val bodyFilled = template == ReportTemplate.BOLD

    when {
        headerFilled -> drawRect(color = accent, size = Size(size.width, headerHeight))
        template == ReportTemplate.CLASSIC -> drawLine(color = accent, start = Offset(0f, headerHeight), end = Offset(size.width, headerHeight), strokeWidth = 3f)
        else -> drawLine(color = neutralLine, start = Offset(0f, headerHeight), end = Offset(size.width, headerHeight), strokeWidth = 2f) // MINIMAL
    }
    val headerTextColor = if (headerFilled) Color.White.copy(alpha = 0.9f) else Color(0xFF111827)
    drawRoundRect(
        color = headerTextColor,
        topLeft = Offset(8f, headerHeight / 2f - 3f),
        size = Size(size.width * 0.32f, 6f),
        cornerRadius = CornerRadius(3f)
    )
    drawRoundRect(
        color = headerTextColor,
        topLeft = Offset(size.width * 0.6f, headerHeight / 2f - 3f),
        size = Size(size.width * 0.28f, 6f),
        cornerRadius = CornerRadius(3f)
    )

    val rowHeight = (size.height - headerHeight) / 2f
    var rowTop = headerHeight
    repeat(2) { index ->
        if (bodyFilled) {
            drawRect(color = accent, topLeft = Offset(0f, rowTop), size = Size(size.width, rowHeight))
            if (index % 2 == 1) drawRect(color = Color.Black.copy(alpha = 0.12f), topLeft = Offset(0f, rowTop), size = Size(size.width, rowHeight))
        } else if (template == ReportTemplate.MODERN && index % 2 == 1) {
            drawRect(color = accent.copy(alpha = 0.08f), topLeft = Offset(0f, rowTop), size = Size(size.width, rowHeight))
        }
        val rowBarColor = if (bodyFilled) Color.White.copy(alpha = 0.85f) else barColor
        drawRoundRect(
            color = rowBarColor,
            topLeft = Offset(8f, rowTop + rowHeight / 2f - 2.5f),
            size = Size(size.width * 0.22f, 5f),
            cornerRadius = CornerRadius(2.5f)
        )
        drawRoundRect(
            color = rowBarColor,
            topLeft = Offset(size.width * 0.6f, rowTop + rowHeight / 2f - 2.5f),
            size = Size(size.width * 0.2f, 5f),
            cornerRadius = CornerRadius(2.5f)
        )
        rowTop += rowHeight
    }
}

private data class SampleRow(val rank: String, val name: String, val score: String, val time: String)

private val SAMPLE_ROWS = listOf(
    SampleRow("1", "Aisha Khan", "96%", "3:42"),
    SampleRow("2", "Rohan Mehta", "91%", "4:10"),
    SampleRow("3", "Priya Singh", "88%", "4:55"),
    SampleRow("4", "Karan Patel", "82%", "5:20")
)

/**
 * A full-size, realistic mock of a Leaderboard export — same title/header/row rules as
 * [drawMockReport] and the real *PdfExporters, just built with actual Compose text instead of
 * Canvas bars, so the user can read real sample data instead of squinting at a tiny thumbnail.
 * Recomposes live as the template/color selection below it changes.
 */
@Composable
private fun SampleReportPreview(template: ReportTemplate, accentColorHex: String) {
    val accent = remember(accentColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(accentColorHex)) }.getOrDefault(Color(ReportDesign.DEFAULT_ACCENT_COLOR))
    }
    val headerFilled = template == ReportTemplate.MODERN || template == ReportTemplate.BOLD
    val bodyFilled = template == ReportTemplate.BOLD
    val headerTextColor = if (headerFilled) Color.White else Color(0xFF111827)
    val bodyTextColor = if (bodyFilled) Color.White else Color(0xFF111827)
    val modernAltColor = remember(accent) { accent.copy(alpha = 0.08f) }
    val boldAltColor = remember(accent) { lerp(accent, Color.Black, 0.15f) }
    val dividerColor = if (template == ReportTemplate.MINIMAL) BorderGray else accent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Leaderboard — Sample Quiz",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (headerFilled) accent else Color(0xFF111827)
            )
            Spacer(Modifier.height(3.dp))
            Text("Participants: 24   Avg score: 82%   Generated: Aug 19, 2026", color = Color(0xFF6B7280), fontSize = 10.sp)
        }
        HorizontalDivider(color = dividerColor, thickness = if (template == ReportTemplate.BOLD) 2.dp else 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (headerFilled) Modifier.background(accent) else Modifier)
                .padding(vertical = 9.dp, horizontal = 14.dp)
        ) {
            Text("RANK", modifier = Modifier.weight(0.18f), color = headerTextColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Text("NAME", modifier = Modifier.weight(0.42f), color = headerTextColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Text("SCORE", modifier = Modifier.weight(0.2f), color = headerTextColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            Text("TIME", modifier = Modifier.weight(0.2f), color = headerTextColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
        when (template) {
            ReportTemplate.CLASSIC -> HorizontalDivider(color = accent, thickness = 2.dp)
            ReportTemplate.MINIMAL -> HorizontalDivider(color = BorderGray, thickness = 1.dp)
            else -> Unit
        }

        SAMPLE_ROWS.forEachIndexed { index, row ->
            val rowBg = when {
                bodyFilled -> if (index % 2 == 1) boldAltColor else accent
                template == ReportTemplate.MODERN && index % 2 == 1 -> modernAltColor
                else -> Color.Transparent
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBg)
                    .padding(vertical = 9.dp, horizontal = 14.dp)
            ) {
                Text(row.rank, modifier = Modifier.weight(0.18f), color = bodyTextColor, fontSize = 11.sp)
                Text(row.name, modifier = Modifier.weight(0.42f), color = bodyTextColor, fontSize = 11.sp)
                Text(row.score, modifier = Modifier.weight(0.2f), color = bodyTextColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(row.time, modifier = Modifier.weight(0.2f), color = bodyTextColor, fontSize = 11.sp)
            }
            if (index != SAMPLE_ROWS.lastIndex) {
                HorizontalDivider(color = if (bodyFilled) Color.White.copy(alpha = 0.3f) else BorderGray, thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun ColorGrid(selectedHex: String, onSelect: (String) -> Unit, onCustomClick: () -> Unit) {
    val rows = remember { ReportDesign.PRESET_COLORS.chunked(5) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        rows.forEachIndexed { rowIndex, rowColors ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                rowColors.forEach { hex ->
                    ColorSwatch(hex = hex, isSelected = hex.equals(selectedHex, ignoreCase = true), onClick = { onSelect(hex) })
                }
                if (rowIndex == rows.lastIndex) {
                    CustomColorSwatch(onClick = onCustomClick)
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(hex: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = remember(hex) { Color(android.graphics.Color.parseColor(hex)) }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(width = if (isSelected) 2.dp else 0.dp, color = TextPrimary, shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CustomColorSwatch(onClick: () -> Unit) {
    val rainbow = remember {
        Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red))
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(rainbow)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = "Custom color", tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

/** From-scratch HSV picker — a saturation/value square + a hue bar, both Canvas-drawn and
 *  drag/tap-driven, plus a two-way-bound hex field. No third-party color-picker dependency. */
@Composable
private fun ColorPickerDialog(initialHex: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val initialColorInt = remember(initialHex) {
        runCatching { android.graphics.Color.parseColor(initialHex) }.getOrDefault(ReportDesign.DEFAULT_ACCENT_COLOR)
    }
    val initialHsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initialColorInt, it) } }
    var hue by remember { mutableStateOf(initialHsv[0]) }
    var saturation by remember { mutableStateOf(initialHsv[1]) }
    var brightness by remember { mutableStateOf(initialHsv[2]) }
    var hexText by remember { mutableStateOf(initialHex.uppercase()) }

    fun currentColorInt(): Int = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))
    fun currentHex(): String = String.format("#%06X", 0xFFFFFF and currentColorInt())

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceWhite)
                .padding(20.dp)
        ) {
            Text("Custom Color", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            Spacer(Modifier.height(16.dp))

            SaturationValueBox(
                hue = hue,
                saturation = saturation,
                value = brightness,
                onChange = { newSat, newVal ->
                    saturation = newSat
                    brightness = newVal
                    hexText = currentHex()
                },
                modifier = Modifier.fillMaxWidth().height(160.dp)
            )
            Spacer(Modifier.height(14.dp))

            HueBar(
                hue = hue,
                onChange = { newHue ->
                    hue = newHue
                    hexText = currentHex()
                },
                modifier = Modifier.fillMaxWidth().height(26.dp)
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(currentColorInt()))
                        .border(1.dp, BorderGray, CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { text ->
                        hexText = text
                        runCatching {
                            val normalized = if (text.startsWith("#")) text else "#$text"
                            val parsed = android.graphics.Color.parseColor(normalized)
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(parsed, hsv)
                            hue = hsv[0]
                            saturation = hsv[1]
                            brightness = hsv[2]
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = { onConfirm(currentHex()) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(currentColorInt()))
                ) { Text("Apply", color = Color.White) }
            }
        }
    }
}

@Composable
private fun SaturationValueBox(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val hueColor = remember(hue) { Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))) }

    fun updateFromOffset(offset: Offset, width: Float, height: Float) {
        val newSat = (offset.x / width).coerceIn(0f, 1f)
        val newVal = 1f - (offset.y / height).coerceIn(0f, 1f)
        onChange(newSat, newVal)
    }

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    updateFromOffset(change.position, size.width.toFloat(), size.height.toFloat())
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset -> updateFromOffset(offset, size.width.toFloat(), size.height.toFloat()) }
            }
    ) {
        drawRect(color = hueColor)
        drawRect(brush = Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

        val indicatorCenter = Offset(saturation * size.width, (1f - value) * size.height)
        drawCircle(color = Color.White, radius = 8.dp.toPx(), center = indicatorCenter, style = Stroke(width = 3.dp.toPx()))
        drawCircle(color = Color.Black.copy(alpha = 0.35f), radius = 8.dp.toPx(), center = indicatorCenter, style = Stroke(width = 1.dp.toPx()))
    }
}

@Composable
private fun HueBar(hue: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val hueGradient = remember {
        Brush.horizontalGradient((0..360 step 60).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it.toFloat(), 1f, 1f))) })
    }

    fun updateFromX(x: Float, width: Float) {
        onChange((x / width * 360f).coerceIn(0f, 360f))
    }

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    updateFromX(change.position.x, size.width.toFloat())
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset -> updateFromX(offset.x, size.width.toFloat()) }
            }
    ) {
        drawRect(brush = hueGradient)
        val indicatorX = (hue / 360f) * size.width
        val radius = size.height / 2f
        drawCircle(color = Color.White, radius = radius, center = Offset(indicatorX, radius), style = Stroke(width = 3.dp.toPx()))
        drawCircle(color = Color.Black.copy(alpha = 0.3f), radius = radius, center = Offset(indicatorX, radius), style = Stroke(width = 1.dp.toPx()))
    }
}
