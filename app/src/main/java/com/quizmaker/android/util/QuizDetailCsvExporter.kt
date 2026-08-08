package com.quizmaker.android.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.quizmaker.android.data.model.QuizDetailViewData
import java.io.File

/** Writes the Quiz Detail View's per-respondent rows as a CSV and returns a share Intent for it. */
object QuizDetailCsvExporter {

    fun export(context: Context, data: QuizDetailViewData): Intent {
        fun escape(value: String) = "\"${value.replace("\"", "\"\"")}\""

        val headers = buildList {
            add("Name")
            add("Email")
            if (data.allowMultipleAttempts) add("Attempt")
            add("Score")
            add("Correct")
            add("Time")
            add("Completed")
        }
        val lines = mutableListOf(headers.joinToString(",") { escape(it) })

        data.rows.forEach { row ->
            val timeLabel = row.timeSeconds?.let { "${it / 60}m ${it % 60}s" } ?: "-"
            val cells = buildList {
                add(row.name)
                add(row.email)
                if (data.allowMultipleAttempts) add("${ordinal(row.attemptNumber)} attempt")
                add("${row.scorePercent}%")
                add("${row.correctCount}/${data.gradedCount}")
                add(timeLabel)
                add(formatDateTime(row.completedAt))
            }
            lines.add(cells.joinToString(",") { escape(it) })
        }

        val csv = lines.joinToString("\n")
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = data.quizTitle.ifBlank { "quiz" }.replace(Regex("[^A-Za-z0-9]+"), "_")
        val file = File(exportsDir, "${safeName}_detail_view.csv")
        file.writeText(csv)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun ordinal(n: Int): String {
        val suffixes = listOf("th", "st", "nd", "rd")
        val v = n % 100
        return "$n${suffixes.getOrElse(if (v in 11..13) 0 else n % 10) { "th" }}"
    }
}
