package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Decode side of the generic `user_settings` table — only the one column this app ever reads back. */
@Serializable
data class UserSettingValueDto(@SerialName("setting_value") val settingValue: JsonObject)

@Serializable
data class UserSettingInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("setting_key") val settingKey: String,
    @SerialName("setting_value") val settingValue: JsonObject
)

@Serializable
data class UserSettingUpdateDto(@SerialName("setting_value") val settingValue: JsonObject)

/** The "report_design" setting_value shape: {"template": "modern"|"classic", "colorHex": "#RRGGBB"}. */
@Serializable
data class ReportDesignJson(val template: String = "modern", val colorHex: String = "#2563EB")

/** The "learner_auto_create" setting_value shape: {"enabled": true|false} — also read directly by
 *  the auto_create_learner_if_enabled() Postgres function (see supabase/sql/learner_auto_create.sql). */
@Serializable
data class LearnerAutoCreateJson(val enabled: Boolean = false)
