package com.quizmaker.android.ui.certificate

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.util.CertificatePdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class TemplateOption(val id: String, val label: String, val description: String)

private val TEMPLATE_OPTIONS = listOf(
    TemplateOption("traditional", "Traditional", "Gold border, gothic title, cursive name"),
    TemplateOption("modern", "Modern", "Bold indigo side block, left-aligned"),
    TemplateOption("simple", "Simple", "Minimal, monochrome, lots of whitespace"),
    TemplateOption("corporate", "Corporate", "Navy header band, formal two-column footer"),
    TemplateOption("elegant", "Elegant", "Ivory background, rose-gold corner flourishes"),
    TemplateOption("ribbon", "Ribbon", "Award-seal badge, bold and celebratory"),
    TemplateOption("geometric", "Geometric", "Angular color-block accents, modern sans-serif"),
    TemplateOption("dark", "Dark Premium", "Charcoal background, gold rules and type")
)

// Sample-only values — never persisted, never used for a real issued certificate (see
// TakeQuizScreen's own CertificatePdfExporter call, which is entirely separate from this
// screen). Filled in purely so the live preview reads like a finished certificate instead of
// showing empty gaps before the business has entered its own company/signer details.
private const val SAMPLE_COMPANY_NAME = "Bright Minds Academy"
private const val SAMPLE_SIGNER_NAME = "Priya Sharma"
private const val SAMPLE_SIGNER_ROLE = "Program Director"

/** Decodes the app's own splash-screen mark as a neutral placeholder "logo" for the preview when
 *  no business logo has been uploaded yet — tinted dark since the source asset is drawn in white
 *  for a colored splash background. Never used for a real exported certificate. */
private fun sampleLogoBitmap(context: android.content.Context): android.graphics.Bitmap? = runCatching {
    val drawable = androidx.core.content.res.ResourcesCompat
        .getDrawable(context.resources, com.quizmaker.android.R.drawable.ic_splash_icon, null)
        ?.mutate() ?: return@runCatching null
    androidx.core.graphics.drawable.DrawableCompat.setTint(drawable, android.graphics.Color.parseColor("#334155"))
    val width = drawable.intrinsicWidth.coerceAtLeast(1)
    val height = drawable.intrinsicHeight.coerceAtLeast(1)
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(canvas)
    bitmap
}.getOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificateDesignerScreen(
    onNavigateBack: () -> Unit,
    viewModel: CertificateDesignerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }

    val logoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val (bytes, contentType) = withContext(Dispatchers.IO) {
                val type = context.contentResolver.getType(uri) ?: "image/jpeg"
                val data = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                data to type
            }
            if (bytes != null) {
                viewModel.uploadLogo(bytes, contentType, contentType.toImageExtension())
            }
        }
    }
    val signaturePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val (bytes, contentType) = withContext(Dispatchers.IO) {
                val type = context.contentResolver.getType(uri) ?: "image/jpeg"
                val data = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                data to type
            }
            if (bytes != null) {
                viewModel.uploadSignature(bytes, contentType, contentType.toImageExtension())
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("Certificate design saved")
        }
    }

    var logoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uiState.logoUrl) {
        logoBitmap = uiState.logoUrl?.let { loadRemoteBitmap(context, it) }
    }
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uiState.signatureUrl) {
        signatureBitmap = uiState.signatureUrl?.let { loadRemoteBitmap(context, it) }
    }

    val issueDateText = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date()) }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Certificate Design", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                Text("Shown with a sample recipient name — edit it below just for this preview.", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(CERTIFICATE_WIDTH / CERTIFICATE_HEIGHT)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                ) {
                    val sampleLogo = remember { sampleLogoBitmap(context) }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val nativeCanvas = drawContext.canvas.nativeCanvas
                        val scale = size.width / CERTIFICATE_WIDTH
                        nativeCanvas.save()
                        nativeCanvas.scale(scale, scale)
                        drawCertificate(
                            template = uiState.template,
                            context = context,
                            canvas = nativeCanvas,
                            width = CERTIFICATE_WIDTH,
                            height = CERTIFICATE_HEIGHT,
                            // Falls back to realistic sample copy (never persisted — see
                            // SAMPLE_COMPANY_NAME's KDoc) so the live preview always reads like a
                            // finished certificate, not an empty template with blank gaps.
                            data = CertificateRenderData(
                                title = uiState.title.ifBlank { "Certificate of Achievement" },
                                bodyText = uiState.bodyText.ifBlank {
                                    "This certificate is proudly presented for outstanding performance and dedication."
                                },
                                companyName = uiState.companyName.ifBlank { SAMPLE_COMPANY_NAME },
                                signerName = uiState.signerName.ifBlank { SAMPLE_SIGNER_NAME },
                                signerRole = uiState.signerRole.ifBlank { SAMPLE_SIGNER_ROLE },
                                recipientName = uiState.previewRecipientName,
                                issueDateLabel = uiState.issueDateLabel.ifBlank { "Date of Issue" },
                                issueDateText = issueDateText
                            ),
                            logoBitmap = logoBitmap ?: sampleLogo,
                            signatureBitmap = signatureBitmap
                        )
                        nativeCanvas.restore()
                    }
                }

                Spacer(Modifier.height(28.dp))

                Text("Template", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    TEMPLATE_OPTIONS.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { option ->
                                TemplateCard(
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

                OutlinedTextField(
                    value = uiState.previewRecipientName,
                    onValueChange = viewModel::onPreviewRecipientNameChange,
                    label = { Text("Preview Recipient Name") },
                    supportingText = { Text("Preview only — not saved with the design.") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(28.dp))

                Text("Content", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text("Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.bodyText,
                    onValueChange = viewModel::onBodyTextChange,
                    label = { Text("Body Text") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.companyName,
                    onValueChange = viewModel::onCompanyNameChange,
                    label = { Text("Company") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.signerName,
                    onValueChange = viewModel::onSignerNameChange,
                    label = { Text("Signer Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.signerRole,
                    onValueChange = viewModel::onSignerRoleChange,
                    label = { Text("Signer Role") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.issueDateLabel,
                    onValueChange = viewModel::onIssueDateLabelChange,
                    label = { Text("Date-of-issue label") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(28.dp))

                Text("Logo", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Spacer(Modifier.height(14.dp))
                ImagePickerRow(
                    imageUrl = uiState.logoUrl,
                    isUploading = uiState.isUploadingLogo,
                    placeholderIcon = Icons.Default.CardMembership,
                    onPick = { logoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onRemove = viewModel::removeLogo
                )

                Spacer(Modifier.height(20.dp))

                Text("Signature", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Spacer(Modifier.height(14.dp))
                ImagePickerRow(
                    imageUrl = uiState.signatureUrl,
                    isUploading = uiState.isUploadingSignature,
                    placeholderIcon = Icons.Default.Draw,
                    onPick = { signaturePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onRemove = viewModel::removeSignature
                )

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = viewModel::saveDesign,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Save")
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isExporting = true
                            val intent = CertificatePdfExporter.export(
                                context = context,
                                template = uiState.template,
                                data = CertificateRenderData(
                                    title = uiState.title,
                                    bodyText = uiState.bodyText,
                                    companyName = uiState.companyName,
                                    signerName = uiState.signerName,
                                    signerRole = uiState.signerRole,
                                    recipientName = uiState.previewRecipientName,
                                    issueDateLabel = uiState.issueDateLabel,
                                    issueDateText = issueDateText
                                ),
                                logoUrl = uiState.logoUrl,
                                signatureUrl = uiState.signatureUrl
                            )
                            isExporting = false
                            context.startActivity(Intent.createChooser(intent, "Share certificate"))
                        }
                    },
                    enabled = !isExporting,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Download / Share PDF")
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TemplateCard(
    option: TemplateOption,
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

@Composable
private fun ImagePickerRow(
    imageUrl: String?,
    isUploading: Boolean,
    placeholderIcon: ImageVector,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderGray, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(6.dp)
                )
            } else {
                Icon(placeholderIcon, contentDescription = null, tint = TextSecondary)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            OutlinedButton(onClick = onPick, enabled = !isUploading) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.height(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (imageUrl != null) "Change" else "Upload")
                }
            }
            if (imageUrl != null) {
                TextButton(onClick = onRemove, enabled = !isUploading) {
                    Text("Remove")
                }
            }
        }
    }
}

private fun String.toImageExtension(): String = when {
    contains("png") -> "png"
    contains("webp") -> "webp"
    else -> "jpg"
}

/** Never throws — a failed download just leaves the preview without that image. */
private suspend fun loadRemoteBitmap(context: Context, url: String): Bitmap? = runCatching {
    val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
    (context.imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap
}.getOrNull()
