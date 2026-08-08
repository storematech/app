package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape of the `sale_day` table — time-boxed premium license sale windows. */
@Serializable
data class SaleDayDto(
    val id: Long,
    @SerialName("sale_dayname") val saleDayName: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("end_at") val endAt: String? = null
)
