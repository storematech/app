package com.quizmaker.android.ui.businesscard

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.LoadingCrossfade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class BusinessCardTemplateOption(val id: BusinessCardTemplate, val label: String, val description: String)

private val TEMPLATE_OPTIONS = listOf(
    BusinessCardTemplateOption(BusinessCardTemplate.CLASSIC, "Classic", "Bronze border, centered elegant serif"),
    BusinessCardTemplateOption(BusinessCardTemplate.MODERN, "Modern", "Bold accent-color side band"),
    BusinessCardTemplateOption(BusinessCardTemplate.MINIMAL, "Minimal", "Plenty of white space, one accent rule"),
    BusinessCardTemplateOption(BusinessCardTemplate.BOLD, "Bold", "Full accent-color background, white type"),
    BusinessCardTemplateOption(BusinessCardTemplate.CORPORATE, "Corporate", "Navy header band, two-column footer")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessCardScreen(
    onNavigateBack: () -> Unit,
    viewModel: BusinessCardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Business Card", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                Text("Live Preview", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Built from your business details in Profile & settings.", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(BUSINESS_CARD_WIDTH / BUSINESS_CARD_HEIGHT)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val nativeCanvas = drawContext.canvas.nativeCanvas
                        val scale = size.width / BUSINESS_CARD_WIDTH
                        nativeCanvas.save()
                        nativeCanvas.scale(scale, scale)
                        drawBusinessCard(
                            template = uiState.template,
                            context = context,
                            canvas = nativeCanvas,
                            width = BUSINESS_CARD_WIDTH,
                            height = BUSINESS_CARD_HEIGHT,
                            data = uiState.renderData,
                            logoBitmap = uiState.logoBitmap,
                            accentColor = uiState.accentColor
                        )
                        nativeCanvas.restore()
                    }
                }

                if (uiState.hasIncompleteDetails) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Add a tagline, phone, address or website in your profile for a fuller card.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onNavigateBack) {
                            Text("Edit", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                Text("Template", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    TEMPLATE_OPTIONS.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { option ->
                                BusinessCardTemplateCard(
                                    option = option,
                                    isSelected = uiState.template == option.id,
                                    onClick = { viewModel.onTemplateSelected(option.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                GradientButton(
                    text = if (isExporting) "Preparing…" else "Download / Share",
                    onClick = {
                        scope.launch {
                            isExporting = true
                            val intent = withContext(Dispatchers.IO) { viewModel.buildShareIntent(context) }
                            isExporting = false
                            context.startActivity(Intent.createChooser(intent, "Share business card"))
                        }
                    },
                    enabled = !isExporting,
                    loading = isExporting,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BusinessCardTemplateCard(
    option: BusinessCardTemplateOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceWhite)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) BrandIndigo else BorderGray,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(option.label, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(option.description, color = TextSecondary, fontSize = 11.sp)
    }
}
