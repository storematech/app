package com.quizmaker.android.ui.common

import android.graphics.Typeface
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.util.superscriptBareExponents
import io.noties.markwon.Markwon
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin

/** Markwon's ext-latex only ever registers its INLINE math handler for a DOUBLE-dollar delimiter
 *  — see JLatexMathInlineProcessor's own matcher, `(\$\$)([\s\S]+?)\1` — even though every AI-
 *  generated and hand-written question in this app is written with the conventional single-dollar
 *  LaTeX/Markdown inline syntax (`$g=10$`, `$30^\circ$`). A single `$...$` never even gets
 *  attempted as math with that matcher — it just passes through as literal text, dollar signs and
 *  all, which is exactly the "math doesn't render, I just see raw $ and ^" bug this works around.
 *  Upgrades every single-dollar span to double-dollar before Markwon ever sees it.
 *
 *  A lot of curated/AI content also writes bare exponents with NO `$...$` around them at all
 *  ("9x10^9 Nm^2/C^2") — those never reach Markwon as math in the first place, so the plain
 *  (non-$) portions are separately run through superscriptBareExponents() ("10^9" -> "10⁹"),
 *  which needs no math renderer at all. Applied only OUTSIDE $...$ spans — a literal superscript
 *  character inside a formula sent to JLaTeXMath isn't the same as the `^` syntax it expects. */
private val SINGLE_DOLLAR_MATH = Regex("\\$([^$\\n]+)\\$")

private fun normalizeMathDelimiters(text: String): String {
    if (!text.contains('$')) return superscriptBareExponents(text)
    if (text.contains("$$")) return text // real double-dollar block content, if any -- leave alone
    val result = StringBuilder()
    var last = 0
    for (match in SINGLE_DOLLAR_MATH.findAll(text)) {
        result.append(superscriptBareExponents(text.substring(last, match.range.first)))
        result.append("$$").append(match.groupValues[1]).append("$$")
        last = match.range.last + 1
    }
    result.append(superscriptBareExponents(text.substring(last)))
    return result.toString()
}

/**
 * Renders question/option text that may contain inline (`$...$`) or block (`$$...$$`) LaTeX math
 * mixed with normal text — e.g. "Which statement about $\arg(z)$ is false?" — as a real typeset
 * formula (stacked fractions, radicals, proper symbols), not raw LaTeX source. Plain text with no
 * math in it renders just like a normal `Text` composable; callers never need to check first.
 *
 * Compose's own Text/AnnotatedString can't lay out an actual formula, so this wraps a classic
 * Android TextView via AndroidView — the standard interop pattern for this gap — driven by
 * Markwon (a markdown renderer) with its ext-latex plugin (which wraps JLaTeXMath for the actual
 * glyph rendering). Because Markwon's base is a markdown parser, stray `*`/`_`/`#` characters in
 * plain question text will be interpreted as markdown emphasis/headers rather than shown literally
 * — an accepted tradeoff; math-heavy text rarely contains those outside intentional LaTeX.
 */
@Composable
fun MathText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextPrimary,
    fontSize: TextUnit = 14.sp,
    maxLines: Int = Int.MAX_VALUE,
    // Classic TextViews only support binary bold/normal (no Compose FontWeight.Medium/SemiBold
    // granularity) — an accepted approximation for the handful of call sites that bolded text.
    bold: Boolean = false
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val textSizePx = with(density) { fontSize.toPx() }

    // Keyed on textSizePx since JLatexMathPlugin bakes the formula render size in at construction.
    val markwon = remember(textSizePx) {
        Markwon.builder(context)
            // JLatexMathPlugin.inlinesEnabled(true) below calls builder.require(MarkwonInlineParserPlugin)
            // internally — in Markwon 4.6.x, inline-span parsing was split out of CorePlugin into its
            // own plugin, so it must be registered explicitly or JLatexMathPlugin.configure() throws
            // "Requested plugin is not added: MarkwonInlineParserPlugin" (crashed the Review step).
            .usePlugin(MarkwonInlineParserPlugin.create())
            .usePlugin(
                JLatexMathPlugin.create(textSizePx) { builder ->
                    // Most question/option text is a sentence with a formula embedded inline
                    // ("...where $\arg(z)$ denotes..."), not a standalone display equation, so
                    // inline math needs to be on — Markwon's ext-latex only handles block ($$)
                    // math by default.
                    builder.inlinesEnabled(true)
                }
            )
            .build()
    }

    AndroidView(
        // Deliberately NOT fillMaxWidth() by default — callers embedding this beside other
        // content in a Row (e.g. next to a checkbox/radio button) need it to size to its own
        // content instead of claiming the whole row and overlapping its siblings. Pass
        // Modifier.fillMaxWidth() explicitly from the caller when that behavior IS wanted.
        modifier = modifier,
        factory = { ctx -> TextView(ctx) },
        update = { tv ->
            tv.setTextColor(color.toArgb())
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
            tv.setTypeface(tv.typeface, if (bold) Typeface.BOLD else Typeface.NORMAL)
            tv.maxLines = maxLines
            tv.ellipsize = if (maxLines != Int.MAX_VALUE) TextUtils.TruncateAt.END else null
            try {
                markwon.setMarkdown(tv, normalizeMathDelimiters(text))
            } catch (t: Throwable) {
                // A single malformed formula (LaTeX syntax JLaTeXMath can't parse) must never
                // crash the whole screen — some hand-written or AI-generated $...$ content will
                // inevitably have a typo JLaTeXMath rejects. Fall back to the raw text plainly
                // rather than taking the app down over one bad question.
                Log.e("MathText", "Failed to render math text, falling back to plain: $text", t)
                tv.text = text
            }
        }
    )
}

/** Live-rendered preview meant to sit right under a question/option text input field, so whoever
 *  is typing or pasting LaTeX (e.g. `$\frac{a}{b}$`) can see it as a real formula immediately,
 *  instead of only finding out it's malformed after saving. Skipped entirely for plain text with
 *  no `$` in it, so it adds no visual noise to the vast majority of questions with no math. */
@Composable
fun MathPreview(text: String) {
    if (!text.contains('$')) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AppBackground)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text("PREVIEW", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(4.dp))
        MathText(text = text, color = TextPrimary, fontSize = 14.sp)
    }
}
