package com.quizmaker.android.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.quizmaker.android.data.model.QuizResponse
import java.io.File

/** Writes a filtered list of quiz responses (across all quizzes) as a summary CSV. */
object ResponsesCsvExporter {

    fun export(context: Context, responses: List<QuizResponse>, quizTitleById: Map<String, String>): Intent {
        fun escape(value: String) = "\"${value.replace("\"", "\"\"")}\""

        val headers = listOf("Quiz", "User", "Score", "Status", "Time Taken", "Started At", "Completed At")
        val lines = mutableListOf(headers.joinToString(",") { escape(it) })

        responses.forEach { response ->
            val status = if (response.cancelled) "Cancelled" else if (response.completed) "Completed" else "In Progress"
            val timeLabel = response.timeTakenSeconds?.let { "${it / 60}m ${it % 60}s" } ?: "-"
            val cells = listOf(
                quizTitleById[response.quizId] ?: "Unknown Quiz",
                response.userName?.ifBlank { null } ?: response.userEmail,
                "${response.score}%",
                status,
                timeLabel,
                formatDateTime(response.startedAt),
                formatDateTime(response.completedAt)
            )
            lines.add(cells.joinToString(",") { escape(it) })
        }

        val csv = lines.joinToString("\n")
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, "quiz_responses.csv")
        file.writeText(csv)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
