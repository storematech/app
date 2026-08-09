package com.quizmaker.android.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.R
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoDark
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.GradientButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToForgotPassword: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun onGoogleSignInClick() {
        scope.launch {
            launchGoogleSignIn(
                context = context,
                onIdToken = viewModel::signInWithGoogle,
                onError = viewModel::onGoogleSignInError
            )
        }
    }

    // Status bar icon color (white, for this screen's full-bleed gradient hero) is owned
    // centrally by NavGraph.kt now, based on which route is current — not per-screen.

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
                Text("Y", color = Color.White, fontFamily = PoppinsFamily, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text("Yuno LMS", color = Color.White, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitleFor(uiState.step, uiState.signUpSucceeded),
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
                ErrorBanner(message = message, onDismiss = viewModel::clearError)
                Spacer(Modifier.height(16.dp))
            }

            AnimatedContent(
                targetState = uiState.step,
                transitionSpec = {
                    val forward = targetState != AuthStep.EMAIL
                    (slideInHorizontally(tween(220)) { if (forward) it / 3 else -it / 3 })
                        .togetherWith(slideOutHorizontally(tween(220)) { if (forward) -it / 3 else it / 3 })
                },
                label = "auth-step"
            ) { step ->
                when (step) {
                    AuthStep.EMAIL -> EmailStep(
                        isChecking = uiState.isCheckingEmail,
                        isGoogleLoading = uiState.isGoogleLoading,
                        onContinue = viewModel::continueWithEmail,
                        onGoogleSignIn = ::onGoogleSignInClick
                    )
                    AuthStep.SIGN_IN -> SignInStep(
                        email = uiState.email,
                        isLoading = uiState.isLoading,
                        onChangeEmail = viewModel::changeEmail,
                        onForgotPassword = onNavigateToForgotPassword,
                        onSignIn = viewModel::signIn
                    )
                    AuthStep.SIGN_UP -> SignUpStep(
                        email = uiState.email,
                        isLoading = uiState.isLoading,
                        succeeded = uiState.signUpSucceeded,
                        onChangeEmail = viewModel::changeEmail,
                        onCreateAccount = viewModel::signUp
                    )
                }
            }
        }
    }
}

private fun subtitleFor(step: AuthStep, signUpSucceeded: Boolean): String = when {
    step == AuthStep.SIGN_UP && signUpSucceeded -> "You're almost there"
    step == AuthStep.SIGN_IN -> "Welcome back"
    step == AuthStep.SIGN_UP -> "Let's set up your account"
    else -> "Everything you need to run great assessments"
}

@Composable
private fun EmailStep(
    isChecking: Boolean,
    isGoogleLoading: Boolean,
    onContinue: (String) -> Unit,
    onGoogleSignIn: () -> Unit
) {
    var email by remember { mutableStateOf("") }

    Column {
        Text("Sign in or create an account", color = TextPrimary, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))
        Text("Enter your email to get started", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(14.dp),
            colors = premiumFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        GradientButton(
            text = "Continue",
            onClick = { onContinue(email) },
            loading = isChecking,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))
        OrDivider()
        Spacer(Modifier.height(20.dp))

        GoogleSignInButton(loading = isGoogleLoading, onClick = onGoogleSignIn)
    }
}

@Composable
private fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = BorderGray, modifier = Modifier.weight(1f))
        Text("OR", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp))
        HorizontalDivider(color = BorderGray, modifier = Modifier.weight(1f))
    }
}

/** Not Firebase — Android's native Credential Manager account picker, see GoogleAuth.kt. */
@Composable
private fun GoogleSignInButton(loading: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceWhite)
            .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
            .clickable(enabled = !loading, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BrandIndigo, strokeWidth = 2.dp)
        } else {
            Image(
                painter = painterResource(R.drawable.ic_google_logo),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text("Continue with Google", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun SignInStep(
    email: String,
    isLoading: Boolean,
    onChangeEmail: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignIn: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column {
        Text("Welcome back", color = TextPrimary, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))
        EmailChip(email = email, onChangeEmail = onChangeEmail)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(14.dp),
            colors = premiumFieldColors(),
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
        )
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
            Text("Forgot password?", color = BrandIndigo, fontSize = 13.sp)
        }

        Spacer(Modifier.height(14.dp))
        GradientButton(
            text = "Sign In",
            onClick = { onSignIn(password) },
            loading = isLoading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SignUpStep(
    email: String,
    isLoading: Boolean,
    succeeded: Boolean,
    onChangeEmail: () -> Unit,
    onCreateAccount: (name: String, password: String) -> Unit
) {
    if (succeeded) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(SuccessGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Check your inbox", color = TextPrimary, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "We've sent a confirmation link to $email. Verify your email, then come back and sign in.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            GradientButton(text = "Back to sign in", onClick = onChangeEmail, modifier = Modifier.fillMaxWidth())
        }
        return
    }

    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column {
        Text("Create your account", color = TextPrimary, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))
        EmailChip(email = email, onChangeEmail = onChangeEmail)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full name") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            shape = RoundedCornerShape(14.dp),
            colors = premiumFieldColors(),
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Create a password") },
            supportingText = { Text("At least 6 characters") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(14.dp),
            colors = premiumFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        GradientButton(
            text = "Create Account",
            onClick = { onCreateAccount(name, password) },
            loading = isLoading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EmailChip(email: String, onChangeEmail: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(BrandIndigo.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(email, color = TextPrimary, fontSize = 13.sp, maxLines = 1)
        Spacer(Modifier.width(8.dp))
        Text(
            "Change",
            color = BrandIndigo,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.clickable(onClick = onChangeEmail)
        )
    }
}

@Composable
private fun premiumFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandIndigo,
    focusedLabelColor = BrandIndigo,
    cursorColor = BrandIndigo
)
