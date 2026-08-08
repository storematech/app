package com.quizmaker.android.data.model

import com.quizmaker.android.data.remote.dto.SaleDayDto
import kotlinx.datetime.Instant

/** A time-boxed premium license sale window, from the `sale_day` table. */
data class SaleDay(
    val name: String,
    val startedAt: Instant?,
    val endAt: Instant?
)

fun SaleDayDto.toDomain(): SaleDay = SaleDay(
    name = saleDayName.orEmpty(),
    startedAt = startedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
    endAt = endAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
)
