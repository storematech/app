package com.quizmaker.android.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Writes an arbitrary headers+rows table as CSV — shared by every "Tools" export button (Onboarding/Feedback submissions, Poll/Voting results, RSVP registrations). Same shape as ResponsesCsvExporter.kt, just generic instead of one fixed data type. */
object GenericCsvExporter {

    fun export(context: Context, fileName: String, headers: List<String>, rows: List<List<String>>): Intent {
        fun escape(value: String) = "\"${value.replace("\"", "\"\"")}\""

        val lines = mutableListOf(headers.joinToString(",") { escape(it) })
        rows.forEach { row -> lines.add(row.joinToString(",") { escape(it) }) }
        val csv = lines.joinToString("\n")

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, fileName)
        file.writeText(csv)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
