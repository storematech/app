package com.quizmaker.android.data.model

/** Domain-level view of a Quiz Maker account — combines the `profiles` row with the Supabase Auth user. */
data class Profile(
    val id: String,
    val email: String,
    val name: String,
    val businessName: String,
    val phoneNumber: String,
    val country: String,
    val role: String
)
