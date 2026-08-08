package com.quizmaker.android.ui.importquestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.QuestionRepository
import com.quizmaker.android.util.FailedImportRow
import com.quizmaker.android.util.QuestionExcelImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

data class ImportQuestionsUiState(
    val selectedFileName: String? = null,
    val isImporting: Boolean = false,
    val hasImported: Boolean = false,
    val successCount: Int = 0,
    val failedRows: List<FailedImportRow> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class ImportQuestionsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val questionRepository: QuestionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportQuestionsUiState())
    val uiState: StateFlow<ImportQuestionsUiState> = _uiState.asStateFlow()

    private var pendingInputStreamProvider: (() -> InputStream?)? = null

    fun onFileSelected(name: String, inputStreamProvider: () -> InputStream?) {
        pendingInputStreamProvider = inputStreamProvider
        _uiState.value = ImportQuestionsUiState(selectedFileName = name)
    }

    fun importSelectedFile() {
        val userId = authRepository.currentUserId() ?: return
        val provider = pendingInputStreamProvider ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, errorMessage = null)

            val stream = provider()
            if (stream == null) {
                _uiState.value = _uiState.value.copy(isImporting = false, errorMessage = "Couldn't open the selected file.")
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                stream.use { QuestionExcelImporter.parse(it) }
            }

            var successCount = 0
            val failed = result.failed.toMutableList()
            result.successful.forEach { parsed ->
                val created = questionRepository.createQuestion(
                    userId = userId,
                    text = parsed.text,
                    type = parsed.type,
                    points = parsed.points,
                    difficulty = parsed.difficulty,
                    explanation = null,
                    tags = parsed.tags,
                    options = parsed.options,
                    freeTextAnswer = parsed.freeTextAnswer,
                    imageUrl = null,
                    isUngraded = parsed.isUngraded
                )
                if (created is AppResult.Success) {
                    successCount++
                } else {
                    failed += FailedImportRow(0, "Couldn't save \"${parsed.text.take(40)}\"")
                }
            }

            _uiState.value = _uiState.value.copy(
                isImporting = false,
                hasImported = true,
                successCount = successCount,
                failedRows = failed
            )
        }
    }
}
