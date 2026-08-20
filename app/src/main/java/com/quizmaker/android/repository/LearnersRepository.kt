package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.Group
import com.quizmaker.android.data.model.Learner
import com.quizmaker.android.data.model.LearnerQuizAttempt
import com.quizmaker.android.data.model.toDomain
import com.quizmaker.android.data.remote.dto.GroupDto
import com.quizmaker.android.data.remote.dto.GroupInsertDto
import com.quizmaker.android.data.remote.dto.LearnerDto
import com.quizmaker.android.data.remote.dto.LearnerInsertDto
import com.quizmaker.android.data.remote.dto.LearnerQuizAttemptDto
import com.quizmaker.android.data.remote.dto.LearnerUpdateDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads/writes `learners` and `groups` — the roster a teacher manages outside of any single
 * quiz, shared with the website (see src/services/learnersService.ts there). Mini LMS course
 * assignment and the learner-login-portal credentials flow are web-only and deliberately not
 * ported here.
 */
@Singleton
class LearnersRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    // Explicit column lists rather than "*": both tables carry web-only columns (e.g. the
    // learner-portal credentials flow's `has_lms_access`) that aren't declared on the DTOs below,
    // and the default kotlinx.serialization Json this app's SupabaseClient uses does not ignore
    // unknown keys — selecting "*" would throw a decode error the moment such a column exists.
    private val learnerColumns = Columns.raw(
        "id, student_id, name, email, group_id, parent_name, parent_contact, notes, created_at, groups!left(name)"
    )
    private val groupColumns = Columns.raw("id, name, description, created_at, created_by")
    private val quizAttemptColumns = Columns.raw("id, score, total_questions, completed_at, time_taken, quizzes(title)")

    suspend fun getLearners(userId: String): AppResult<List<Learner>> = safeCall {
        supabase.from("learners")
            .select(learnerColumns) {
                filter { eq("created_by", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<LearnerDto>()
            .map { it.toDomain() }
    }

    suspend fun getGroups(userId: String): AppResult<List<Group>> = safeCall {
        supabase.from("groups")
            .select(groupColumns) {
                filter { eq("created_by", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<GroupDto>()
            .map { it.toDomain() }
    }

    suspend fun createGroup(userId: String, name: String, description: String?): AppResult<Group> = safeCall {
        supabase.from("groups")
            .insert(GroupInsertDto(name = name, description = description, createdBy = userId)) { select(groupColumns) }
            .decodeSingle<GroupDto>()
            .toDomain()
    }

    suspend fun createLearner(
        userId: String,
        name: String,
        email: String,
        groupId: String?,
        parentName: String?,
        parentContact: String?,
        notes: String?
    ): AppResult<Unit> = safeCall {
        supabase.from("learners").insert(
            LearnerInsertDto(
                studentId = generateStudentId(),
                name = name,
                email = email.lowercase(),
                groupId = groupId,
                parentName = parentName,
                parentContact = parentContact,
                notes = notes,
                createdBy = userId
            )
        )
        Unit
    }

    suspend fun updateLearner(
        learnerId: String,
        name: String,
        email: String,
        groupId: String?,
        parentName: String?,
        parentContact: String?,
        notes: String?
    ): AppResult<Unit> = safeCall {
        supabase.from("learners").update(
            LearnerUpdateDto(
                name = name,
                email = email.lowercase(),
                groupId = groupId,
                parentName = parentName,
                parentContact = parentContact,
                notes = notes
            )
        ) {
            filter { eq("id", learnerId) }
        }
        Unit
    }

    suspend fun deleteLearner(learnerId: String): AppResult<Unit> = safeCall {
        supabase.from("learners").delete { filter { eq("id", learnerId) } }
        Unit
    }

    suspend fun getQuizAttempts(learnerId: String): AppResult<List<LearnerQuizAttempt>> = safeCall {
        supabase.from("quiz_attempts")
            .select(quizAttemptColumns) {
                filter { eq("learner_id", learnerId) }
                order("completed_at", Order.DESCENDING)
            }
            .decodeList<LearnerQuizAttemptDto>()
            .map { it.toDomain() }
    }

    /** Mirrors the web's generateStudentId() fallback — a timestamp-based id if the RPC fails, rather than blocking learner creation. */
    private suspend fun generateStudentId(): String = runCatching {
        "STU${supabase.postgrest.rpc("generate_student_id").decodeAs<Long>()}"
    }.getOrElse { "STU${System.currentTimeMillis()}" }
}
