package com.quizmaker.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoDark
import com.quizmaker.android.core.theme.PoppinsFamily

/**
 * Shown while the session gate resolves (SessionGate.LOADING) — same blue + "Y" mark as the
 * native splash screen (Theme.QuizMaker.Splash / ic_splash_icon.xml) so there's no jarring
 * hand-off between the OS splash and the first Compose frame.
 */
@Composable
fun AppLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BrandIndigoDark, BrandIndigo))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text("Y", color = Color.White, fontFamily = PoppinsFamily, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text("Yuno LMS", color = Color.White, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Color.White, strokeWidth = 2.5.dp)
        }
    }
}
