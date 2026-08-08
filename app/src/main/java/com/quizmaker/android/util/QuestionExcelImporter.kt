package com.quizmaker.android.util

import com.quizmaker.android.data.model.QuestionDifficulty
import com.quizmaker.android.data.model.QuestionType
import org.dhatim.fastexcel.reader.ReadableWorkbook
import java.io.InputStream

/** One row parsed into a question ready to save, before it's actually written to the question bank. */
data class ParsedQuestion(
    val text: String,
    val type: QuestionType,
    val points: Int,
    val difficulty: QuestionDifficulty,
    val tags: List<String>,
    val options: List<Pair<String, Boolean>>,
    val freeTextAnswer: String?,
    val isUngraded: Boolean
)

data class FailedImportRow(val row: Int, val reason: String)

data class ExcelImportResult(val successful: List<ParsedQuestion>, val failed: List<FailedImportRow>)

/**
 * Parses the same column layout as the web app's Import Questions template:
 * Question, Type, Option A-D, Correct Answer, Points, Tags, Difficulty.
 */
object QuestionExcelImporter {
    private val OPTION_LETTERS = listOf("a", "b", "c", "d")

    fun parse(inputStream: InputStream): ExcelImportResult {
        val rows = try {
            ReadableWorkbook(inputStream).use { it.firstSheet.read() }
        } catch (e: Exception) {
            return ExcelImportResult(emptyList(), listOf(FailedImportRow(0, "Couldn't read this file — make sure it's a valid .xlsx file.")))
        }

        val successful = mutableListOf<ParsedQuestion>()
        val failed = mutableListOf<FailedImportRow>()

        for (i in 1 until rows.size) { // row 0 is the header
            val row = rows[i]
            val rowNum = i + 1
            val questionText = row.getCellText(0).trim()
            if (questionText.isBlank()) continue // blank row — skip silently

            val typeRaw = row.getCellText(1).trim().lowercase().ifBlank { "option" }
            val typeValue = if (typeRaw == "text") "free-text" else typeRaw
            if (typeValue !in setOf("option", "free-text", "multi-choice")) {
                failed += FailedImportRow(rowNum, "Type must be option, free-text, or multi-choice")
                continue
            }

            val points = row.getCellText(7).trim().toIntOrNull()?.takeIf { it > 0 } ?: 1
            val tags = row.getCellText(8).split(",").map { it.trim() }.filter { it.isNotBlank() }
            val difficultyRaw = row.getCellText(9).trim().lowercase().ifBlank { "medium" }
            val difficulty = QuestionDifficulty.entries.firstOrNull { it.value == difficultyRaw }
            if (difficulty == null) {
                failed += FailedImportRow(rowNum, "Difficulty must be easy, medium, or hard")
                continue
            }

            when (val type = QuestionType.fromValue(typeValue)) {
                QuestionType.SINGLE_CHOICE, QuestionType.MULTI_CHOICE -> {
                    val optionTexts = (2..5).map { row.getCellText(it).trim() }
                    if (optionTexts[0].isBlank() || optionTexts[1].isBlank()) {
                        failed += FailedImportRow(rowNum, "Option A and Option B are required")
                        continue
                    }
                    val presentOptions = optionTexts.mapIndexedNotNull { idx, text ->
                        if (text.isNotBlank()) OPTION_LETTERS[idx] to text else null
                    }
                    val correctLetters = row.getCellText(6).trim().lowercase()
                        .split(",", " ").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                    val validLetters = presentOptions.map { it.first }.toSet()
                    if (correctLetters.isEmpty() || correctLetters.any { it !in validLetters }) {
                        failed += FailedImportRow(rowNum, "Correct Answer must match a provided option (A-D)")
                        continue
                    }
                    if (type == QuestionType.SINGLE_CHOICE && correctLetters.size > 1) {
                        failed += FailedImportRow(rowNum, "Single choice questions need exactly one correct answer")
                        continue
                    }
                    val options = presentOptions.map { (letter, text) -> text to (letter in correctLetters) }
                    successful += ParsedQuestion(questionText, type, points, difficulty, tags, options, null, isUngraded = false)
                }
                QuestionType.FREE_TEXT, QuestionType.FILL_IN_BLANK -> {
                    val answer = row.getCellText(6).trim()
                    successful += ParsedQuestion(
                        questionText, type, points, difficulty, tags,
                        options = emptyList(),
                        freeTextAnswer = answer.ifBlank { null },
                        isUngraded = answer.isBlank()
                    )
                }
            }
        }

        return ExcelImportResult(successful, failed)
    }
}
