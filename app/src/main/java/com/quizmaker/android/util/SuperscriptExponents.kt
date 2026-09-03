package com.quizmaker.android.util

/**
 * A lot of this app's curated/AI-generated question content writes exponents as a bare caret --
 * "9x10^9 Nm^2/C^2", "m/s^2" -- with no `$...$` LaTeX delimiters around them at all (unlike the
 * inline-formula case MathText.kt's normalizeMathDelimiters() handles). Since nothing ever gets
 * sent to a math renderer for these, the caret just shows up literally. For a plain integer
 * exponent like this, real Unicode superscript characters render correctly in ANY plain text
 * (Compose Text, a PDF Canvas.drawText, anywhere) with no LaTeX/Markwon/JLaTeXMath involved --
 * "10^9" -> "10⁹", "m/s^2" -> "m/s²", "10^-19" -> "10⁻¹⁹".
 *
 * Callers that also handle `$...$` math segments (MathText.kt, MasterPaperPdfExporter.kt) must
 * only run this over the PLAIN portions of the text, never inside a $...$ span -- a literal
 * superscript character sent to a LaTeX renderer isn't the same as the `^` syntax it expects.
 */
private val SUPERSCRIPT_CHARS = mapOf(
    '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
    '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
    '-' to '⁻', '+' to '⁺'
)

private val BARE_EXPONENT = Regex("\\^(-?\\d+)")

fun superscriptBareExponents(text: String): String {
    if (!text.contains('^')) return text
    return BARE_EXPONENT.replace(text) { match ->
        match.groupValues[1].map { SUPERSCRIPT_CHARS[it] ?: it }.joinToString("")
    }
}
