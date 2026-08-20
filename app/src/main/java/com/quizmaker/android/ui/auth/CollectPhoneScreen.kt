package com.quizmaker.android.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoDark
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.util.Country
import com.quizmaker.android.util.CountryCodes

/**
 * One-time onboarding interstitial shown after sign-in/sign-up when `profiles.phone_number` is
 * still empty — see SessionViewModel.needsPhoneNumber(). No OTP/verification: this is a plain
 * collection field, saved straight to Supabase.
 */
@Composable
fun CollectPhoneScreen(
    onSaved: () -> Unit,
    viewModel: CollectPhoneViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onSaved()
    }

    // Status bar icon color (white, for this screen's full-bleed hero) is owned centrally by
    // NavGraph.kt now, based on which route is current — not per-screen.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BrandIndigoDark, BrandIndigo)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 28.dp, bottom = 20.dp, start = 28.dp, end = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("One last thing", color = Color.White, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Add your phone number to start free",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(SurfaceWhite)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 32.dp)
        ) {
            uiState.errorMessage?.let { message ->
                ErrorBanner(message = message)
                Spacer(Modifier.height(16.dp))
            }

            Text("What's your phone number?", color = TextPrimary, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text("We'll only ask once — this helps us reach you about your account.", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                CountryCodeChip(
                    country = uiState.selectedCountry,
                    onClick = viewModel::openCountryPicker
                )
                OutlinedTextField(
                    value = uiState.phoneNumber,
                    onValueChange = viewModel::onPhoneNumberChange,
                    label = { Text("Phone number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(14.dp),
                    colors = premiumFieldColors(),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(24.dp))

            GradientButton(
                text = "Continue",
                onClick = viewModel::save,
                enabled = uiState.phoneNumber.isNotBlank(),
                loading = uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (uiState.isCountryPickerOpen) {
        CountryPickerDialog(
            onDismiss = viewModel::closeCountryPicker,
            onSelect = viewModel::onCountrySelected
        )
    }
}

@Composable
private fun CountryCodeChip(country: Country, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Transparent)
            .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(country.dialCode, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CountryPickerDialog(onDismiss: () -> Unit, onSelect: (Country) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) {
            CountryCodes.all
        } else {
            CountryCodes.all.filter {
                it.countryName.contains(query, ignoreCase = true) || it.dialCode.contains(query)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(SurfaceWhite)
        ) {
            Text(
                "Select country",
                color = TextPrimary,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search country or code") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = premiumFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = BorderGray)

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filtered, key = { it.iso }) { country ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(country) }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(country.countryName, color = TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text(country.dialCode, color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun premiumFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandIndigo,
    focusedLabelColor = BrandIndigo,
    cursorColor = BrandIndigo
)
