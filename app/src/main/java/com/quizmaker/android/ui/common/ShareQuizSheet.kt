package com.quizmaker.android.ui.common

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary

/** "Share Quiz" bottom sheet: a prominent share-code card plus the full link, QR, and native share. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareQuizSheet(quizTitle: String, shareUrl: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showQr by remember { mutableStateOf(false) }
    val shareCode = shareUrl.substringAfterLast("/").uppercase()

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceWhite) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(BrandIndigoLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Share Quiz", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                    Text(quizTitle, color = TextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(20.dp))

            // Share code card — the short, easy-to-read-aloud code, front and center.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .elevatedSurface(shape = RoundedCornerShape(18.dp), elevation = 0.dp, color = BrandIndigoLight)
                    .padding(18.dp)
            ) {
                Text("SHARE CODE", color = BrandIndigo, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        shareCode,
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        modifier = Modifier.weight(1f)
                    )
                    CopyIconButton { clipboardManager.setText(AnnotatedString(shareCode)) }
                }
            }
            Spacer(Modifier.height(14.dp))

            // Full link row — same code, expressed as the URL learners actually open.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    shareUrl,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                CopyIconButton { clipboardManager.setText(AnnotatedString(shareUrl)) }
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { showQr = !showQr },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = "Show QR code", tint = BrandIndigo, modifier = Modifier.size(18.dp))
                }
            }

            if (showQr) {
                Spacer(Modifier.height(16.dp))
                val qrBitmap = remember(shareUrl) { com.quizmaker.android.util.QrCodeGenerator.generate(shareUrl) }
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR code linking to this quiz",
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Anyone with this code or link can take the quiz without needing to log in",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(20.dp))

            GradientButton(
                text = "Share Link",
                leadingIcon = Icons.Default.Share,
                onClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Take my quiz \"$quizTitle\": $shareUrl")
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share quiz"))
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SurfaceWhite)
                    .border(1.dp, BorderGray, RoundedCornerShape(50))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text("Done", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CopyIconButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = BrandIndigo, modifier = Modifier.size(18.dp))
    }
}
