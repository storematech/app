package com.quizmaker.android.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.quizmaker.android.ui.common.ErrorBanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            uiState.errorMessage?.let {
                ErrorBanner(message = it)
                Spacer(Modifier.height(16.dp))
            }

            Text("Account", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.email,
                onValueChange = {},
                label = { Text("Email") },
                enabled = false,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Full name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.businessName,
                onValueChange = viewModel::onBusinessNameChange,
                label = { Text("Business name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.phoneNumber,
                onValueChange = viewModel::onPhoneNumberChange,
                label = { Text("Phone number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.country,
                onValueChange = viewModel::onCountryChange,
                label = { Text("Country") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.address,
                onValueChange = viewModel::onAddressChange,
                label = { Text("Address") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            if (uiState.saveSuccess) {
                Text("Saved!", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
            }
            Button(onClick = viewModel::saveProfile, enabled = !uiState.isSaving, modifier = Modifier.fillMaxWidth()) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save changes")
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text("Business Letterhead", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Shown at the top of exported PDFs (quiz results, leaderboards, reports).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(12.dp)),
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
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text("Remove", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text("Change password", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            uiState.passwordError?.let {
                ErrorBanner(message = it)
                Spacer(Modifier.height(8.dp))
            }
            if (uiState.passwordChangeSuccess) {
                Text("Password updated!", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(
                value = uiState.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                label = { Text("New password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = { Text("Confirm new password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = viewModel::changePassword,
                enabled = !uiState.isChangingPassword,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isChangingPassword) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Update password")
                }
            }

            Spacer(Modifier.height(32.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Signed in as ${uiState.email}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = viewModel::signOut, modifier = Modifier.fillMaxWidth()) {
                        Text("Sign out", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
