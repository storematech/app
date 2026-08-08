package com.quizmaker.android.data.model

import kotlinx.datetime.Instant

data class LeaderboardEntry(
    val rank: Int,
    val userEmail: String,
    val userName: String,
    val earnedPoints: Int,
    val completedAt: Instant?,
    val timeTakenSeconds: Int? = null
)

data class LeaderboardData(
    val quizTitle: String,
    val maxPoints: Int,
    val entries: List<LeaderboardEntry>,
    val totalAttempts: Int,
    val averageScore: Int
)
