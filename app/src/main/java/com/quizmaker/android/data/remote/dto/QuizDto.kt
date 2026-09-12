package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape of the `quizzes` table (subset of columns used by Phase 1 screens). */
@Serializable
data class QuizDto(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("is_public") val isPublic: Boolean? = null,
    @SerialName("visibility_type") val visibilityType: String? = null,
    @SerialName("assigned_group_id") val assignedGroupId: String? = null,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("time_limit") val timeLimit: Int? = null,
    @SerialName("time_limit_type") val timeLimitType: String? = null,
    @SerialName("time_per_question") val timePerQuestion: Int? = null,
    @SerialName("shuffle_questions") val shuffleQuestions: Boolean? = null,
    @SerialName("share_id") val shareId: String? = null,
    @SerialName("is_closed") val isClosed: Boolean? = null,
    @SerialName("show_results") val showResults: Boolean? = null,
    @SerialName("send_result_email") val sendResultEmail: Boolean? = null,
    @SerialName("allow_result_pdf") val allowResultPdf: Boolean? = null,
    @SerialName("show_leaderboard") val showLeaderboard: Boolean? = null,
    @SerialName("issue_certificate") val issueCertificate: Boolean? = null,
    @SerialName("certificate_pass_score") val certificatePassScore: Int? = null,
    @SerialName("is_offline_exam") val isOfflineExam: Boolean? = null,
    @SerialName("show_contact_details") val showContactDetails: Boolean? = null,
    val instructions: String? = null,
    val theme: String? = null,
    @SerialName("quiz_color") val quizColor: String? = null,
    @SerialName("collect_email") val collectEmail: Boolean? = null,
    @SerialName("collect_address") val collectAddress: Boolean? = null,
    @SerialName("collect_phone") val collectPhone: Boolean? = null,
    @SerialName("require_otp_verification") val requireOtpVerification: Boolean? = null,
    @SerialName("allow_multiple_attempts") val allowMultipleAttempts: Boolean? = null,
    @SerialName("trainer_id") val trainerId: String? = null,
    @SerialName("max_points") val maxPoints: Double? = null,
    @SerialName("negative_marking_mode") val negativeMarkingMode: String? = null,
    @SerialName("negative_marking_value") val negativeMarkingValue: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

/**
 * Payload used when creating a new quiz — mirrors createQuiz() in the web app's supabaseService.ts.
 *
 * IMPORTANT: kotlinx.serialization's default `Json` (used by supabase-kt) has `encodeDefaults = false`,
 * meaning any property left at its declared default value is dropped from the outgoing JSON entirely
 * (relying on the Postgres column's own DEFAULT instead — which we don't want for an explicit insert).
 * So every field the caller must always control is a *required* constructor parameter here (no default),
 * guaranteeing it's always serialized. Only genuinely-optional fields keep a nullable `= null` default.
 */
@Serializable
data class QuizInsertDto(
    val title: String,
    val description: String?,
    @SerialName("created_by") val createdBy: String,
    @SerialName("is_public") val isPublic: Boolean,
    @SerialName("visibility_type") val visibilityType: String,
    @SerialName("assigned_group_id") val assignedGroupId: String? = null,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("time_limit") val timeLimit: Int?,
    @SerialName("time_limit_type") val timeLimitType: String,
    @SerialName("time_per_question") val timePerQuestion: Int?,
    @SerialName("shuffle_questions") val shuffleQuestions: Boolean,
    @SerialName("show_results") val showResults: Boolean,
    @SerialName("send_result_email") val sendResultEmail: Boolean,
    @SerialName("allow_result_pdf") val allowResultPdf: Boolean,
    @SerialName("show_leaderboard") val showLeaderboard: Boolean,
    @SerialName("issue_certificate") val issueCertificate: Boolean,
    @SerialName("certificate_pass_score") val certificatePassScore: Int? = null,
    @SerialName("is_offline_exam") val isOfflineExam: Boolean,
    @SerialName("show_contact_details") val showContactDetails: Boolean,
    val instructions: String?,
    val theme: String,
    @SerialName("quiz_color") val quizColor: String,
    @SerialName("collect_email") val collectEmail: Boolean,
    @SerialName("collect_address") val collectAddress: Boolean,
    @SerialName("collect_phone") val collectPhone: Boolean,
    @SerialName("require_otp_verification") val requireOtpVerification: Boolean,
    @SerialName("allow_multiple_attempts") val allowMultipleAttempts: Boolean,
    @SerialName("negative_marking_mode") val negativeMarkingMode: String,
    @SerialName("negative_marking_value") val negativeMarkingValue: Double,
    @SerialName("share_id") val shareId: String
)
