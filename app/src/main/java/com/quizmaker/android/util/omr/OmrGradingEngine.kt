package com.quizmaker.android.util.omr

import android.graphics.Bitmap
import android.util.Log
import com.quizmaker.android.data.model.OmrLayout
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.roundToInt

/** One bubbled question's detected mark(s). [Detected] carries 1 optionId for a SINGLE_CHOICE
 *  question, 0..n for MULTI_CHOICE. [Blank] means no bubble cleared [OmrGradingEngine.FILL_THRESHOLD].
 *  [Ambiguous] (SINGLE_CHOICE only) means 2+ bubbles were too close in fill ratio to call a winner —
 *  these must be surfaced to the teacher for a manual tap-to-resolve, never auto-scored. */
sealed class OmrBubbleResult {
    data class Detected(val optionIds: List<String>) : OmrBubbleResult()
    object Blank : OmrBubbleResult()
    object Ambiguous : OmrBubbleResult()
}

/** [fillRatios] is optionId -> darkPixelRatio for every bubble on this question, kept around so a
 *  review UI can show/debug why a call was made (or let the teacher eyeball a near-miss). */
data class OmrScanQuestionResult(
    val questionId: String,
    val result: OmrBubbleResult,
    val fillRatios: Map<String, Float>
)

/** [warpedBitmap] is the deskewed, canonical page image (null if fiducial detection failed — see
 *  [fiducialsFound]), handy for a review screen to show the teacher what was actually sampled. */
data class OmrScanResult(
    val perQuestion: List<OmrScanQuestionResult>,
    val warpedBitmap: Bitmap?,
    val fiducialsFound: Boolean
)

/**
 * Classical (non-ML, OpenCV-based) grading of a photographed OMR bubble sheet against the
 * [OmrLayout] recorded when its PDF was generated (see `OmrSheetPdfExporter`). No machine learning
 * is used anywhere in this pipeline — every step is thresholding/contour/geometry, per this
 * feature's explicit constraint.
 *
 * ## v1 scope limitation: single page per photo
 * Only `layout.pages[0]` is graded from a given [Bitmap] — one photographed page maps to exactly
 * one physical sheet of paper. A quiz whose bubble sheet spans multiple pages needs [scan] called
 * once per photographed page; the calling ViewModel owns that loop (and merging the per-page
 * [OmrScanResult]s) — this function deliberately does not attempt multi-page stitching, matching
 * the plan's explicit "acceptable v1 scope limit" call.
 *
 * ## Algorithm
 * 1. Grayscale + blur + Otsu-threshold the raw photo to find contour candidates for the 4 corner
 *    fiducials (solid black squares — see `OmrSheetPdfExporter.drawFiducials`).
 * 2. Pick the best square-ish candidate nearest each of the image's 4 quadrants. If fewer than 4
 *    are found, bail out gracefully (`fiducialsFound = false`, every bubbled question reported as
 *    [OmrBubbleResult.Ambiguous] so a review screen has something to show rather than nothing) —
 *    the UI is expected to tell the teacher to retake the photo in that case.
 * 3. Compute a perspective transform from the 4 detected fiducial centroids to their "ideal"
 *    position in a fixed working pixel space (page rendered at [WORKING_DPI]), and warp the
 *    original photo into that canonical, deskewed space.
 * 4. Adaptive-threshold the warped image (more robust to uneven photo lighting than a single global
 *    Otsu threshold would be on a real phone photo) and sample a small square crop centered on each
 *    [com.quizmaker.android.data.model.OmrBubble]'s scaled position, computing what fraction of that
 *    crop is "dark" (filled-in pencil/pen mark).
 * 5. Decide Detected/Blank/Ambiguous per question from those fill ratios.
 */
object OmrGradingEngine {

    private const val TAG = "OmrGradingEngine"

    /** Working resolution the photographed page is warped into: the canonical image is
     *  `layout.pageWidth * WORKING_DPI / 72` px wide (PDF points -> pixels at this DPI). 200 is a
     *  reasonable starting point for A4 with ~14pt (~0.2in) bubbles -- about a 39px-wide bubble at
     *  this DPI, plenty for a threshold+crop sample. Tune this constant up if real-world photos need
     *  finer sampling. */
    private const val WORKING_DPI = 200f

    /** A bubble's dark-pixel ratio must reach this fraction of its sampled crop to count as "filled"
     *  at all. Below this, a SINGLE_CHOICE question is [OmrBubbleResult.Blank] and a MULTI_CHOICE
     *  bubble is simply not included in the detected set. */
    private const val FILL_THRESHOLD = 0.35f

    /** For SINGLE_CHOICE: if the top two fill ratios are within this margin of each other, the mark
     *  is too ambiguous to call automatically (e.g. a smudge, or two bubbles genuinely half-filled). */
    private const val AMBIGUITY_MARGIN = 0.12f

    // Fiducial-candidate contour filtering, expressed as fractions of the raw photo's own
    // dimensions so this works regardless of the phone camera's actual resolution.
    private const val FIDUCIAL_MIN_AREA_FRACTION = 0.00005
    private const val FIDUCIAL_MAX_AREA_FRACTION = 0.02
    private const val FIDUCIAL_MAX_ASPECT_DEVIATION = 0.35 // |w/h - 1.0| must be under this to count as "square"

    // A bubble's sampled crop is a plain square (side = 2*radius*BUBBLE_SAMPLE_INSET), not a true
    // circular mask -- simpler to get right with Mat.submat()/Core.countNonZero() than building a
    // circular mask Mat per bubble, and slightly inset from the drawn circle's own radius so the
    // sample doesn't pick up the printed bubble's stroke outline itself as "dark".
    private const val BUBBLE_SAMPLE_INSET = 0.8f

    // Adaptive threshold block size (must be odd) for the warped page -- roughly matches a bubble's
    // own diameter at WORKING_DPI so local lighting variation is normalized without also erasing the
    // marks themselves.
    private const val ADAPTIVE_BLOCK_SIZE = 35
    private const val ADAPTIVE_C = 10.0

    private val openCvInitialized = AtomicBoolean(false)

    /**
     * DECISION: org.opencv:opencv 4.10.0 (the Maven Central artifact used here, NOT the legacy
     * "OpenCV Android SDK" zip distribution) packages its native library so that OpenCV's own JNI
     * bridge classes load `libopencv_java4.so` via a static initializer the first time a native
     * class is touched -- there's no separate "OpenCV Manager" app to bind to like the old SDK
     * required. In principle that means no explicit init call is required before the first
     * `Utils`/`Imgproc`/`Core` call. However, since this is the first OpenCV usage in this codebase
     * and there's no existing precedent to cross-check that auto-load behavior against on this
     * project's exact Gradle/AGP/NDK setup, we call `OpenCVLoader.initLocal()` defensively anyway --
     * it's cheap, synchronous, idempotent-by-design in OpenCV itself, and guarded here to run only
     * once per process. If it turns out to be genuinely unnecessary, it's a harmless no-op; if the
     * static-load assumption above is wrong on some device, this call is what saves us.
     */
    private fun ensureOpenCvInitialized() {
        if (openCvInitialized.compareAndSet(false, true)) {
            runCatching { OpenCVLoader.initLocal() }
                .onFailure { Log.w(TAG, "OpenCVLoader.initLocal() failed; relying on OpenCV's own static native load", it) }
        }
    }

    /** Never throws -- any OpenCV failure on unpredictable real-photo input degrades to a
     *  `fiducialsFound = false` result rather than crashing the caller. */
    fun scan(bitmap: Bitmap, layout: OmrLayout): OmrScanResult {
        ensureOpenCvInitialized()
        return try {
            scanInternal(bitmap, layout)
        } catch (t: Throwable) {
            Log.e(TAG, "OMR scan failed", t)
            degenerateResult(layout)
        }
    }

    private fun degenerateResult(layout: OmrLayout): OmrScanResult {
        val bubbledQuestions = layout.pages.firstOrNull()?.questions.orEmpty().filter { it.bubbles.isNotEmpty() }
        return OmrScanResult(
            perQuestion = bubbledQuestions.map { OmrScanQuestionResult(it.questionId, OmrBubbleResult.Ambiguous, emptyMap()) },
            warpedBitmap = null,
            fiducialsFound = false
        )
    }

    private data class FiducialCandidate(val cx: Double, val cy: Double, val area: Double)

    private fun scanInternal(bitmap: Bitmap, layout: OmrLayout): OmrScanResult {
        val page = layout.pages.firstOrNull() ?: return OmrScanResult(emptyList(), null, fiducialsFound = false)
        if (page.fiducials.size < 4) return degenerateResult(layout)

        val mats = mutableListOf<Mat>()
        fun track(m: Mat): Mat { mats.add(m); return m }

        try {
            val srcColor = track(Mat())
            Utils.bitmapToMat(bitmap, srcColor) // RGBA
            val srcGray = track(Mat())
            Imgproc.cvtColor(srcColor, srcGray, Imgproc.COLOR_RGBA2GRAY)

            val blurred = track(Mat())
            Imgproc.GaussianBlur(srcGray, blurred, Size(5.0, 5.0), 0.0)

            val otsuThresh = track(Mat())
            Imgproc.threshold(blurred, otsuThresh, 0.0, 255.0, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)

            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = track(Mat())
            Imgproc.findContours(otsuThresh, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

            val imgW = srcGray.cols().toDouble()
            val imgH = srcGray.rows().toDouble()
            val imgArea = imgW * imgH

            val candidates = mutableListOf<FiducialCandidate>()
            for (c in contours) {
                val rect = Imgproc.boundingRect(c)
                c.release()
                val area = rect.width.toDouble() * rect.height.toDouble()
                val areaFraction = area / imgArea
                if (areaFraction < FIDUCIAL_MIN_AREA_FRACTION || areaFraction > FIDUCIAL_MAX_AREA_FRACTION) continue
                val aspect = rect.width.toDouble() / rect.height.toDouble()
                if (abs(aspect - 1.0) > FIDUCIAL_MAX_ASPECT_DEVIATION) continue
                candidates.add(FiducialCandidate(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0, area))
            }

            val halfW = imgW / 2.0
            val halfH = imgH / 2.0
            fun bestIn(xRange: ClosedFloatingPointRange<Double>, yRange: ClosedFloatingPointRange<Double>): FiducialCandidate? =
                candidates.filter { it.cx in xRange && it.cy in yRange }.maxByOrNull { it.area }

            // Same TL/TR/BL/BR order OmrSheetPdfExporter.drawFiducials builds page.fiducials in.
            val topLeft = bestIn(0.0..halfW, 0.0..halfH)
            val topRight = bestIn(halfW..imgW, 0.0..halfH)
            val bottomLeft = bestIn(0.0..halfW, halfH..imgH)
            val bottomRight = bestIn(halfW..imgW, halfH..imgH)
            if (topLeft == null || topRight == null || bottomLeft == null || bottomRight == null) {
                return degenerateResult(layout)
            }

            val scale = WORKING_DPI / 72f
            val workingW = (layout.pageWidth * scale).roundToInt().coerceAtLeast(1)
            val workingH = (layout.pageHeight * scale).roundToInt().coerceAtLeast(1)

            val srcCorners = track(MatOfPoint2f(
                Point(topLeft.cx, topLeft.cy),
                Point(topRight.cx, topRight.cy),
                Point(bottomLeft.cx, bottomLeft.cy),
                Point(bottomRight.cx, bottomRight.cy)
            ))
            val destCorners = track(MatOfPoint2f(
                Point((page.fiducials[0].cx * scale).toDouble(), (page.fiducials[0].cy * scale).toDouble()),
                Point((page.fiducials[1].cx * scale).toDouble(), (page.fiducials[1].cy * scale).toDouble()),
                Point((page.fiducials[2].cx * scale).toDouble(), (page.fiducials[2].cy * scale).toDouble()),
                Point((page.fiducials[3].cx * scale).toDouble(), (page.fiducials[3].cy * scale).toDouble())
            ))

            val transform = track(Imgproc.getPerspectiveTransform(srcCorners, destCorners))
            val warpedColor = track(Mat())
            Imgproc.warpPerspective(srcColor, warpedColor, transform, Size(workingW.toDouble(), workingH.toDouble()))

            val warpedGray = track(Mat())
            Imgproc.cvtColor(warpedColor, warpedGray, Imgproc.COLOR_RGBA2GRAY)

            val binaryWarped = track(Mat())
            Imgproc.adaptiveThreshold(
                warpedGray, binaryWarped, 255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV,
                ADAPTIVE_BLOCK_SIZE, ADAPTIVE_C
            )

            val perQuestion = page.questions.filter { it.bubbles.isNotEmpty() }.map { q ->
                val fillRatios = q.bubbles.associate { b -> b.optionId to sampleFillRatio(binaryWarped, b.cx, b.cy, b.radius, scale) }
                val result: OmrBubbleResult = if (q.type == "multi-choice") {
                    val detected = q.bubbles.filter { (fillRatios[it.optionId] ?: 0f) >= FILL_THRESHOLD }.map { it.optionId }
                    if (detected.isEmpty()) OmrBubbleResult.Blank else OmrBubbleResult.Detected(detected)
                } else {
                    // "option" (SINGLE_CHOICE) -- any other bubbled type is treated the same way defensively.
                    val sorted = q.bubbles.sortedByDescending { fillRatios[it.optionId] ?: 0f }
                    val topRatio = sorted.getOrNull(0)?.let { fillRatios[it.optionId] } ?: 0f
                    val secondRatio = sorted.getOrNull(1)?.let { fillRatios[it.optionId] } ?: 0f
                    when {
                        topRatio < FILL_THRESHOLD -> OmrBubbleResult.Blank
                        (topRatio - secondRatio) < AMBIGUITY_MARGIN -> OmrBubbleResult.Ambiguous
                        else -> OmrBubbleResult.Detected(listOf(sorted[0].optionId))
                    }
                }
                OmrScanQuestionResult(q.questionId, result, fillRatios)
            }

            val warpedBitmap = Bitmap.createBitmap(workingW, workingH, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(warpedColor, warpedBitmap)

            return OmrScanResult(perQuestion = perQuestion, warpedBitmap = warpedBitmap, fiducialsFound = true)
        } finally {
            mats.forEach { runCatching { it.release() } }
        }
    }

    /** Samples a square crop (side = `2 * radiusPt*scale * BUBBLE_SAMPLE_INSET`) centered on the
     *  bubble's scaled position and returns the fraction of "dark" (foreground, post-threshold)
     *  pixels in it. */
    private fun sampleFillRatio(binaryWarped: Mat, cxPt: Float, cyPt: Float, radiusPt: Float, scale: Float): Float {
        val cx = cxPt * scale
        val cy = cyPt * scale
        val half = (radiusPt * scale * BUBBLE_SAMPLE_INSET)
        val left = (cx - half).roundToInt().coerceIn(0, binaryWarped.cols() - 1)
        val top = (cy - half).roundToInt().coerceIn(0, binaryWarped.rows() - 1)
        val right = (cx + half).roundToInt().coerceIn(left + 1, binaryWarped.cols())
        val bottom = (cy + half).roundToInt().coerceIn(top + 1, binaryWarped.rows())
        val roi = binaryWarped.submat(top, bottom, left, right)
        val nonZero = Core.countNonZero(roi)
        val total = roi.rows() * roi.cols()
        roi.release()
        return if (total > 0) nonZero.toFloat() / total.toFloat() else 0f
    }
}
