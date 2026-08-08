package com.quizmaker.android.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.quizmaker.android.data.model.AnswerStatus
import com.quizmaker.android.data.model.ResponseDetailData
import java.io.File

/** Writes one participant's question-by-question breakdown as a CSV and returns a share Intent for it. */
object ResponseDetailCsvExporter {

    fun export(context: Context, data: ResponseDetailData): Intent {
        fun escape(value: String) = "\"${value.replace("\"", "\"\"")}\""

        val headers = listOf("Question", "Type", "Submitted Answer", "Correct Answer", "Points", "Status")
        val lines = mutableListOf(headers.joinToString(",") { escape(it) })

        data.answers.forEach { answer ->
            val statusLabel = when (answer.status) {
                AnswerStatus.CORRECT -> "Correct"
                AnswerStatus.PARTIAL -> "Partial"
                AnswerStatus.INCORRECT -> "Incorrect"
                AnswerStatus.UNGRADED -> "Ungraded"
            }
            val cells = listOf(
                answer.questionText,
                answer.questionType.value,
                answer.studentAnswer,
                answer.correctAnswer,
                if (answer.status == AnswerStatus.UNGRADED) "-" else "${answer.pointsEarned}/${answer.maxPoints}",
                statusLabel
            )
            lines.add(cells.joinToString(",") { escape(it) })
        }

        val csv = lines.joinToString("\n")
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = data.userEmail.ifBlank { "response" }.replace(Regex("[^A-Za-z0-9]+"), "_")
        val file = File(exportsDir, "response_$safeName.csv")
        file.writeText(csv)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
