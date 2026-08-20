package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.remote.dto.LearnerAutoCreateJson
import com.quizmaker.android.data.remote.dto.ReportDesignJson
import com.quizmaker.android.data.remote.dto.UserSettingInsertDto
import com.quizmaker.android.data.remote.dto.UserSettingUpdateDto
import com.quizmaker.android.data.remote.dto.UserSettingValueDto
import com.quizmaker.android.util.ReportDesign
import com.quizmaker.android.util.ReportTemplate
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton

private const val REPORT_DESIGN_KEY = "report_design"
private const val LEARNER_AUTO_CREATE_KEY = "learner_auto_create"

/**
 * Generic per-account settings store — one reusable `user_settings(user_id, setting_key,
 * setting_value)` table instead of a table per setting (more account-level settings expected
 * over time). [getSetting]/[upsertSetting] are the generic primitives; [getReportDesign]/
 * [saveReportDesign] are the first typed convenience wrapper on top.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    // Explicit column rather than "*": the table also carries id/user_id/created_at that a bare
    // select() would return but this DTO doesn't declare, and this app's SupabaseClient Json
    // doesn't ignore unknown keys — the same trap already hit and fixed once this session in
    // ReportedQuestionsRepository.
    private val settingValueColumn = Columns.raw("setting_value")

    suspend fun getSetting(userId: String, key: String): AppResult<JsonObject?> = safeCall(notifyOnError = false) {
        supabase.from("user_settings")
            .select(settingValueColumn) { filter { eq("user_id", userId); eq("setting_key", key) } }
            .decodeSingleOrNull<UserSettingValueDto>()
            ?.settingValue
    }

    /** (user_id, setting_key) isn't the primary key, so this can't rely on Postgrest's onConflict
     *  upsert (unused/unverified anywhere else in this codebase) — read first, update if a row
     *  already exists else insert. */
    suspend fun upsertSetting(userId: String, key: String, value: JsonObject): AppResult<Unit> = safeCall {
        val existing = (getSetting(userId, key) as? AppResult.Success)?.data
        if (existing != null) {
            supabase.from("user_settings").update(UserSettingUpdateDto(settingValue = value)) {
                filter { eq("user_id", userId); eq("setting_key", key) }
            }
        } else {
            supabase.from("user_settings").insert(
                UserSettingInsertDto(userId = userId, settingKey = key, settingValue = value)
            )
        }
        Unit
    }

    suspend fun getReportDesign(userId: String): AppResult<ReportDesign> =
        when (val result = getSetting(userId, REPORT_DESIGN_KEY)) {
            is AppResult.Success -> AppResult.Success(result.data?.let(::decodeReportDesign) ?: ReportDesign.DEFAULT)
            is AppResult.Error -> result
        }

    suspend fun saveReportDesign(userId: String, design: ReportDesign): AppResult<Unit> =
        upsertSetting(userId, REPORT_DESIGN_KEY, encodeReportDesign(design))

    private fun decodeReportDesign(value: JsonObject): ReportDesign = runCatching {
        val dto = json.decodeFromJsonElement<ReportDesignJson>(value)
        ReportDesign(
            template = runCatching { ReportTemplate.valueOf(dto.template.uppercase()) }.getOrDefault(ReportTemplate.MODERN),
            colorHex = dto.colorHex
        )
    }.getOrDefault(ReportDesign.DEFAULT)

    private fun encodeReportDesign(design: ReportDesign): JsonObject =
        json.encodeToJsonElement(ReportDesignJson(design.template.name.lowercase(), design.colorHex)).jsonObject

    /** Also read directly (bypassing RLS via security definer) by auto_create_learner_if_enabled()
     *  from the anonymous quiz-taking session — see supabase/sql/learner_auto_create.sql. */
    suspend fun getLearnerAutoCreate(userId: String): AppResult<Boolean> =
        when (val result = getSetting(userId, LEARNER_AUTO_CREATE_KEY)) {
            is AppResult.Success -> AppResult.Success(
                result.data?.let { runCatching { json.decodeFromJsonElement<LearnerAutoCreateJson>(it).enabled }.getOrDefault(false) } ?: false
            )
            is AppResult.Error -> result
        }

    suspend fun saveLearnerAutoCreate(userId: String, enabled: Boolean): AppResult<Unit> =
        upsertSetting(userId, LEARNER_AUTO_CREATE_KEY, json.encodeToJsonElement(LearnerAutoCreateJson(enabled)).jsonObject)
}
