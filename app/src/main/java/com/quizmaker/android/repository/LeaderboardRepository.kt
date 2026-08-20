package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.LeaderboardData
import com.quizmaker.android.data.model.LeaderboardEntry
import com.quizmaker.android.data.remote.dto.QuizAnswerDetailDto
import com.quizmaker.android.data.remote.dto.QuizAttemptDto
import com.quizmaker.android.data.remote.dto.QuizDto
import com.quizmaker.android.data.remote.dto.QuizResponseDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
private fun isValidEmail(email: String?): Boolean = email != null && EMAIL_REGEX.matches(email)

/** Mirrors the ranking/dedup/scoring logic in the web app's LeaderboardDialog.tsx exactly, so
 * points shown here always agree with the website for the same quiz. */
@Singleton
class LeaderboardRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getLeaderboard(quizId: String): AppResult<LeaderboardData> = safeCall {
        val quiz = supabase.from("quizzes")
            .select { filter { eq("id", quizId) } }
            .decodeSingle<QuizDto>()
        val maxPoints = (quiz.maxPoints ?: 0.0).roundToInt()

        val responses = supabase.from("quiz_responses")
            .select {
                filter { eq("quiz_id", quizId); eq("completed", true) }
            }
            .decodeList<QuizResponseDto>()

        if (responses.isEmpty()) {
            return@safeCall LeaderboardData(
                quizTitle = quiz.title,
                maxPoints = maxPoints,
                entries = emptyList(),
                totalAttempts = 0,
                averageScore = 0
            )
        }

        val earnedByResponse: Map<String, Int> = supabase.from("quiz_answer_details")
            .select { filter { isIn("response_id", responses.map { it.id }) } }
            .decodeList<QuizAnswerDetailDto>()
            .groupBy { it.responseId }
            // Floors at 0 to stay consistent with quiz_responses.score, which QuizTakingRepository
            // already floors at the total level — a negative leaderboard total would be confusing.
            .mapValues { (_, details) -> details.sumOf { it.pointsEarned ?: 0.0 }.roundToInt().coerceAtLeast(0) }

        val validEmails = responses.map { it.userEmail }.filter(::isValidEmail)
        val timeTakenByEmail: Map<String, Int> = if (validEmails.isEmpty()) emptyMap() else {
            supabase.from("quiz_attempts")
                .select {
                    filter { eq("quiz_id", quizId); isIn("student_email", validEmails) }
                }
                .decodeList<QuizAttemptDto>()
                .mapNotNull { attempt -> attempt.studentEmail?.let { it to attempt.timeTaken } }
                .filter { it.second != null }
                .associate { it.first to it.second!! }
        }

        data class Attempt(val response: QuizResponseDto, val earnedPoints: Int)

        val attempts = responses.map { response ->
            // Mirrors the web's fallback: quiz_responses.score is a 0-100 percentage, so when no
            // per-question breakdown exists yet, scale it back up to points out of maxPoints.
            val fallback = if (maxPoints > 0 && response.score != null) {
                (response.score.toDouble() / 100.0 * maxPoints).roundToInt()
            } else {
                response.score ?: 0
            }
            Attempt(response, earnedByResponse[response.id] ?: fallback)
        }

        // Keep only the best attempt per email: highest points, then earliest completion.
        val bestByEmail = attempts
            .groupBy { it.response.userEmail }
            .mapValues { (_, list) ->
                list.minWith(
                    compareByDescending<Attempt> { it.earnedPoints }
                        .thenBy { it.response.completedAt ?: "" }
                )
            }
            .values
            .sortedWith(
                compareByDescending<Attempt> { it.earnedPoints }
                    .thenBy { it.response.completedAt ?: "" }
            )

        val entries = bestByEmail.mapIndexed { index, attempt ->
            LeaderboardEntry(
                rank = index + 1,
                userEmail = attempt.response.userEmail,
                userName = attempt.response.userName?.ifBlank { null } ?: attempt.response.userEmail.substringBefore("@"),
                earnedPoints = attempt.earnedPoints,
                completedAt = attempt.response.completedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
                timeTakenSeconds = timeTakenByEmail[attempt.response.userEmail]
            )
        }

        val averageScore = responses.map { it.score ?: 0 }.average().let { if (it.isNaN()) 0 else it.toInt() }

        LeaderboardData(
            quizTitle = quiz.title,
            maxPoints = maxPoints,
            entries = entries,
            totalAttempts = responses.size,
            averageScore = averageScore
        )
    }
}
