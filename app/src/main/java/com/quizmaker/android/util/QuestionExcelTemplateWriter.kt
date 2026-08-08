package com.quizmaker.android.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.dhatim.fastexcel.Workbook
import java.io.File
import java.io.FileOutputStream

private const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

private val TEMPLATE_HEADERS = listOf(
    "Question", "Type", "Option A", "Option B", "Option C", "Option D",
    "Correct Answer", "Points", "Tags", "Difficulty"
)

private val TEMPLATE_EXAMPLES = listOf(
    listOf("What is the capital of France?", "option", "London", "Paris", "Berlin", "Madrid", "B", "1", "Geography", "easy"),
    listOf("Explain the water cycle", "free-text", "", "", "", "", "", "2", "Science", "medium")
)

/** Generates the same importable column layout the web app's template uses. */
object QuestionExcelTemplateWriter {
    fun export(context: Context): Intent {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, "question_import_template.xlsx")

        FileOutputStream(file).use { fos ->
            val workbook = Workbook(fos, "Yuno LMS", "1.0")
            val sheet = workbook.newWorksheet("Questions")
            TEMPLATE_HEADERS.forEachIndexed { col, header -> sheet.value(0, col, header) }
            TEMPLATE_EXAMPLES.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { col, value -> sheet.value(rowIndex + 1, col, value) }
            }
            sheet.finish()
            workbook.finish()
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = XLSX_MIME
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
