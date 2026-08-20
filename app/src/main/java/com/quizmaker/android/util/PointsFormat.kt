package com.quizmaker.android.util

import kotlin.math.roundToInt

/** "1.0" -> "1", "1.5" -> "1.5", "1.25" -> "1.25" — trims the decimal noise Double.toString() adds
 *  for whole numbers, without ever showing more than 2 decimal places. */
fun Double.formatPoints(): String {
    val rounded = (this * 100).roundToInt() / 100.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        "%.2f".format(rounded).trimEnd('0').trimEnd('.')
    }
}
