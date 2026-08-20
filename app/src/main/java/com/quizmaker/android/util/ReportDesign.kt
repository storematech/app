package com.quizmaker.android.util

import android.graphics.Color

/**
 * Four points on the same "how much does the accent color fill vs. just accent" spectrum, applied
 * by every *PdfExporter and PdfLetterhead via the [PdfBranding] it's folded into:
 * - [MODERN]: colored header band + white text + faint accent-tinted row stripes.
 * - [CLASSIC]: plain white background, black text, a thin accent-color rule under the header.
 * - [BOLD]: the accent color fills the whole table body (not just the header), white text
 *   throughout, alternating rows darkened slightly instead of tinted.
 * - [MINIMAL]: no fills anywhere, not even a rule — plain black text on white with neutral hairline
 *   borders; the accent color only shows up in the title text and the letterhead divider.
 */
enum class ReportTemplate { MODERN, CLASSIC, BOLD, MINIMAL }

/** The account's saved PDF report look, synced via SettingsRepository under setting_key = "report_design". */
data class ReportDesign(val template: ReportTemplate, val colorHex: String) {
    /** Falls back to the default brand blue if colorHex is malformed (old/foreign data, bad hand edit). */
    fun accentColorInt(): Int = runCatching { Color.parseColor(colorHex) }.getOrDefault(DEFAULT_ACCENT_COLOR)

    companion object {
        const val DEFAULT_ACCENT_COLOR = 0xFF2563EB.toInt()
        const val DEFAULT_COLOR_HEX = "#2563EB"
        val DEFAULT = ReportDesign(ReportTemplate.MODERN, DEFAULT_COLOR_HEX)

        /** The 10 curated presets shown on Report Design — index 0 is the default brand blue. */
        val PRESET_COLORS = listOf(
            "#2563EB", // Blue (default)
            "#4F46E5", // Indigo
            "#7C3AED", // Violet
            "#C026D3", // Fuchsia
            "#DB2777", // Rose
            "#DC2626", // Red
            "#EA580C", // Orange
            "#16A34A", // Green
            "#0D9488", // Teal
            "#334155"  // Slate
        )
    }
}
