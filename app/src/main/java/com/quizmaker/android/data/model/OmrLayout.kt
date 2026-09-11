package com.quizmaker.android.data.model

import kotlinx.serialization.Serializable

/**
 * Printable OMR ("bubble sheet") layout for one quiz export — persisted as the `layout` jsonb
 * column on `omr_sheets` (see supabase/migrations/20260910120000_add_omr_sheets.sql) so a later
 * scan/grade pass can map a detected mark on the photographed paper back to the exact
 * question/option that produced it.
 *
 * Coordinates are in PDF points, in the same coordinate space as [OmrSheetPdfExporter] draws onto
 * (top-left origin, same as android.graphics.Canvas / PdfDocument) — [pageWidth]/[pageHeight] give
 * the exporter's own PAGE_WIDTH/PAGE_HEIGHT so a scanned photo (whatever its pixel resolution) can
 * be rescaled to this same coordinate space before comparing against fiducial/bubble positions.
 *
 * [OmrQuestionLayout.type] deliberately stores [QuestionType.value] (e.g. "option"/"multi-choice")
 * rather than the enum itself, so this model stays a plain serializable data shape independent of
 * the domain enum's own wire format — matches how [com.quizmaker.android.data.remote.dto.QuestionDto]
 * already stores `type` as a raw String column.
 */
@Serializable
data class OmrBubble(
    val optionId: String,
    val label: String,
    val cx: Float,
    val cy: Float,
    val radius: Float
)

@Serializable
data class OmrQuestionLayout(
    val questionId: String,
    val type: String,
    val bubbles: List<OmrBubble> = emptyList()
)

/** One of the 4 solid black corner squares printed on every page — used by the (future) scanning
 *  pass to detect the page's rotation/scale/skew before mapping bubble coordinates onto the photo. */
@Serializable
data class OmrFiducial(
    val cx: Float,
    val cy: Float,
    val size: Float
)

@Serializable
data class OmrPage(
    val fiducials: List<OmrFiducial>,
    val questions: List<OmrQuestionLayout>
)

@Serializable
data class OmrLayout(
    val pageWidth: Float,
    val pageHeight: Float,
    val pages: List<OmrPage>
)
