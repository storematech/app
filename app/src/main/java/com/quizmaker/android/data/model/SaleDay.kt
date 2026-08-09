package com.quizmaker.android.data.model

import com.quizmaker.android.data.remote.dto.SaleDayDto
import kotlinx.datetime.Instant

/** A time-boxed premium license sale window, from the `sale_day` table. */
data class SaleDay(
    val name: String,
    val startedAt: Instant?,
    val endAt: Instant?,
    /** Percentage off the regular plan price, e.g. 60 for "60% off". 0 if not set for this sale. */
    val discountPercent: Int
)

fun SaleDayDto.toDomain(): SaleDay = SaleDay(
    name = saleDayName.orEmpty(),
    startedAt = startedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
    endAt = endAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
    discountPercent = (discount ?: 0).coerceIn(0, 100)
)
