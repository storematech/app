package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.Serializable

/** Request/response shape for the shared `verify-quiz-access` Supabase Edge Function — same one the
 *  web app already calls from TakeQuiz.tsx for non-public (all_learners/group) quizzes. */
@Serializable
data class VerifyQuizAccessRequest(val quizId: String, val userEmail: String)

@Serializable
data class VerifyQuizAccessResponse(
    val allowed: Boolean = false,
    val reason: String? = null,
    val learnerId: String? = null,
    val error: String? = null
)
