package com.quizmaker.android.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.GradientButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Reset password", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val icon = if (uiState.resetEmailSent) Icons.Default.MarkEmailRead else Icons.Default.LockReset
            val iconTint = if (uiState.resetEmailSent) SuccessGreen else BrandIndigo
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(20.dp))

            if (uiState.resetEmailSent) {
                Text(
                    "If an account exists for that email, we've sent a link to reset your password.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                GradientButton(text = "Back to sign in", onClick = onNavigateBack, modifier = Modifier.fillMaxWidth())
            } else {
                Text(
                    "Enter the email you signed up with and we'll send you a reset link.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))

                uiState.errorMessage?.let {
                    ErrorBanner(message = it, onDismiss = viewModel::clearError)
                    Spacer(Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandIndigo,
                        focusedLabelColor = BrandIndigo,
                        cursorColor = BrandIndigo
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
                GradientButton(
                    text = "Send reset link",
                    onClick = { viewModel.resetPassword(email) },
                    loading = uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
