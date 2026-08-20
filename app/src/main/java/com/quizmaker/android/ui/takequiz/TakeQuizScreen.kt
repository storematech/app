package com.quizmaker.android.ui.takequiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.util.formatPoints

@Composable
fun TakeQuizScreen(
    onViewLeaderboard: (String) -> Unit,
    onFinished: () -> Unit,
    viewModel: TakeQuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState.phase) {
                TakeQuizPhase.LOADING -> LoadingContent()
                TakeQuizPhase.NOT_FOUND, TakeQuizPhase.ALREADY_COMPLETED -> MessageContent(
                    message = uiState.errorMessage ?: "This quiz isn't available.",
                    onDone = onFinished
                )
                TakeQuizPhase.REGISTRATION -> RegistrationContent(uiState, viewModel)
                TakeQuizPhase.OTP -> OtpContent(uiState, viewModel)
                TakeQuizPhase.QUESTIONS -> QuestionsContent(uiState, viewModel)
                TakeQuizPhase.SUBMITTING -> LoadingContent(label = "Submitting your answers…")
                TakeQuizPhase.RESULT -> ResultContent(uiState, onViewLeaderboard, onFinished)
            }
        }
    }
}

@Composable
private fun LoadingContent(label: String? = null) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        if (label != null) {
            Spacer(Modifier.height(16.dp))
            Text(label)
        }
    }
}

@Composable
private fun MessageContent(message: String, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone) { Text("Close") }
    }
}

@Composable
private fun RegistrationContent(uiState: TakeQuizUiState, viewModel: TakeQuizViewModel) {
    val quiz = uiState.quizForTaking?.quiz ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(quiz.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (quiz.description.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(quiz.description, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${uiState.quizForTaking?.questions?.size ?: 0} questions" +
                (quiz.timeLimit?.let { " · $it min" } ?: quiz.timePerQuestion?.let { " · ${it}s per question" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        uiState.errorMessage?.let {
            ErrorBanner(message = it)
            Spacer(Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        if (quiz.collectEmail) {
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }
        if (quiz.collectPhone) {
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = viewModel::onPhoneChange,
                label = { Text("Phone number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }
        if (quiz.collectAddress) {
            OutlinedTextField(
                value = uiState.address,
                onValueChange = viewModel::onAddressChange,
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = viewModel::submitRegistration,
            enabled = !uiState.isSendingOtp,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isSendingOtp) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Start quiz")
            }
        }
    }
}

@Composable
private fun OtpContent(uiState: TakeQuizUiState, viewModel: TakeQuizViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Verify your email", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "We sent a 6-digit code to ${uiState.email}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        uiState.otpError?.let {
            ErrorBanner(message = it)
            Spacer(Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = uiState.otpCode,
            onValueChange = viewModel::onOtpCodeChange,
            label = { Text("Verification code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = viewModel::verifyOtp,
            enabled = !uiState.isSendingOtp,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isSendingOtp) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Verify")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = viewModel::resendOtp, modifier = Modifier.fillMaxWidth()) {
            Text("Resend code")
        }
    }
}

@Composable
private fun QuestionsContent(uiState: TakeQuizUiState, viewModel: TakeQuizViewModel) {
    val question = uiState.currentQuestion ?: return
    val total = uiState.quizForTaking?.questions?.size ?: 1
    val answer = uiState.answers[question.id] ?: AnswerInput()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LinearProgressIndicator(
            progress = { (uiState.currentQuestionIndex + 1) / total.toFloat() },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Question ${uiState.currentQuestionIndex + 1} of $total", style = MaterialTheme.typography.bodyMedium)
            (uiState.overallSecondsRemaining ?: uiState.perQuestionSecondsRemaining)?.let { seconds ->
                Text(formatTime(seconds), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(16.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(question.text, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            when (question.type) {
                QuestionType.SINGLE_CHOICE -> question.options.forEach { option ->
                    OptionRow(
                        text = option.text,
                        selected = option.id in answer.selectedOptionIds,
                        multi = false,
                        onClick = { viewModel.selectSingleOption(question.id, option.id) }
                    )
                }

                QuestionType.MULTI_CHOICE -> question.options.forEach { option ->
                    OptionRow(
                        text = option.text,
                        selected = option.id in answer.selectedOptionIds,
                        multi = true,
                        onClick = { viewModel.toggleMultiOption(question.id, option.id) }
                    )
                }

                QuestionType.FREE_TEXT, QuestionType.FILL_IN_BLANK -> OutlinedTextField(
                    value = answer.freeText,
                    onValueChange = { viewModel.onFreeTextChange(question.id, it) },
                    label = { Text("Your answer") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(
                onClick = viewModel::goToPreviousQuestion,
                enabled = uiState.currentQuestionIndex > 0
            ) { Text("Previous") }
            Button(onClick = viewModel::goToNextQuestion) {
                Text(if (uiState.isLastQuestion) "Submit" else "Next")
            }
        }
    }
}

@Composable
private fun OptionRow(text: String, selected: Boolean, multi: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .selectable(selected = selected, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (multi) {
            Checkbox(checked = selected, onCheckedChange = { onClick() })
        } else {
            RadioButton(selected = selected, onClick = onClick)
        }
        Text(text, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ResultContent(uiState: TakeQuizUiState, onViewLeaderboard: (String) -> Unit, onFinished: () -> Unit) {
    val quiz = uiState.quizForTaking?.quiz
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Quiz complete!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        if (quiz?.showResults != false) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${uiState.finalScore.formatPoints()} / ${uiState.maxPoints.formatPoints()}", style = MaterialTheme.typography.headlineMedium)
                    Text("${uiState.scorePercent}%", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            Text(
                "Your response has been submitted.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(24.dp))
        if (quiz?.showLeaderboard == true) {
            Button(onClick = { onViewLeaderboard(quiz.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("View leaderboard")
            }
            Spacer(Modifier.height(12.dp))
        }
        OutlinedButton(onClick = onFinished, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}
