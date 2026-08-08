package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.SaleDay
import com.quizmaker.android.data.model.toDomain
import com.quizmaker.android.data.remote.dto.SaleDayDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

/** Reads the `sale_day` table so the Dashboard can surface a banner while a sale window is live. */
@Singleton
class SaleDayRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getSaleDays(): AppResult<List<SaleDay>> = safeCall {
        supabase.from("sale_day")
            .select()
            .decodeList<SaleDayDto>()
            .map { it.toDomain() }
    }
}
