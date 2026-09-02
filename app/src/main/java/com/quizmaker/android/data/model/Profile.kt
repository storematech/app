package com.quizmaker.android.data.model

import kotlin.time.Instant

/** Domain-level view of a Quiz Maker account — combines the `profiles` row with the Supabase Auth user. */
data class Profile(
    val id: String,
    val email: String,
    val name: String,
    val businessName: String,
    val phoneNumber: String,
    val country: String,
    val role: String,
    val userType: String?,
    val licenseExpiredDate: String?,
    val createdAt: Instant?,
    /** Public URL into the `business-logos` Storage bucket, or null if never uploaded — the PDF letterhead's logo. */
    val businessLogo: String?,
    /** The PDF letterhead's address line, shown alongside the logo. */
    val address: String?,
    /** The PDF letterhead's website line. */
    val website: String?,
    /** The PDF letterhead's registration/license number line. */
    val registrationNumber: String?,
    /** The PDF letterhead's contact phone — deliberately separate from [phoneNumber] (that one's the
     *  account's own OTP/quiz-taking contact number, not necessarily meant for a public letterhead). */
    val letterheadPhone: String?,
    /** The PDF letterhead's contact email — deliberately separate from [email] (the login address). */
    val letterheadEmail: String?,
    /** The PDF letterhead's short tagline/slogan line under the business name. */
    val tagline: String?,
    /** The PDF letterhead's GST/tax ID line. */
    val gstNumber: String?
) {
    /** Mirrors the web app's premium check (TrialAndPricing.tsx): paid plan tiers only. */
    val isPremium: Boolean
        get() = userType.orEmpty().lowercase() in setOf("starter", "school", "school pro")
}
