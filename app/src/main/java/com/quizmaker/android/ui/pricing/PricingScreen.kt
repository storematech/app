package com.quizmaker.android.ui.pricing

import android.app.Activity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.elevatedSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingScreen(
    onNavigateBack: () -> Unit,
    onPaymentSuccess: () -> Unit,
    viewModel: PricingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.paymentSuccess) {
        if (uiState.paymentSuccess) onPaymentSuccess()
    }
    LaunchedEffect(uiState.paymentError) {
        uiState.paymentError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissPaymentError()
        }
    }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Pricing", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LoadingCrossfade(isLoading = uiState.isLoading, modifier = Modifier.padding(padding)) {
            when {
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize().padding(20.dp)) {
                    ErrorBanner(message = uiState.errorMessage!!)
                }
                uiState.plan != null -> {
                    val plan = uiState.plan!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .elevatedSurface(shape = RoundedCornerShape(24.dp), elevation = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BrandIndigoLight)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (uiState.badge.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(BrandIndigo)
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(uiState.badge, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.height(14.dp))
                                }
                                Text(
                                    "${plan.label} ${plan.flag}",
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("Full premium access to Yuno LMS", color = TextSecondary, fontSize = 13.sp)
                                Spacer(Modifier.height(20.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        "${currencySymbol(plan.currency)}${plan.amount}",
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 44.sp,
                                        color = TextPrimary
                                    )
                                }
                                Text("per ${plan.interval}", color = TextSecondary, fontSize = 14.sp)
                            }

                            Column(modifier = Modifier.padding(20.dp)) {
                                uiState.features.forEach { feature ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text(feature, color = TextPrimary, fontSize = 14.sp)
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val activity = context as? Activity ?: return@Button
                                        viewModel.startCheckout(activity)
                                    },
                                    enabled = !uiState.isProcessingPayment,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo)
                                ) {
                                    if (uiState.isProcessingPayment) {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text("Buy Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun currencySymbol(currencyCode: String): String = when (currencyCode.uppercase()) {
    "INR" -> "₹"
    "USD" -> "$"
    else -> "$currencyCode "
}
