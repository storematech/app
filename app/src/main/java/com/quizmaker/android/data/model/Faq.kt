package com.quizmaker.android.data.model

data class FaqItem(val question: String, val answer: String)

data class FaqSection(val title: String, val items: List<FaqItem>)

/**
 * Static FAQ content — unlike pricing, this doesn't need to change without an app release,
 * so it's kept simple as data in the app rather than another Edge Function.
 */
object FaqContent {
    val sections: List<FaqSection> = listOf(
        FaqSection(
            title = "Getting Started",
            items = listOf(
                FaqItem(
                    "What is Yuno LMS?",
                    "Yuno LMS is a quiz and assessment platform for creating quizzes, sharing them with " +
                        "participants, and reviewing results, leaderboards, and per-question analysis."
                ),
                FaqItem(
                    "How do I create my first quiz?",
                    "Tap \"Create Quiz\" from the Quizzes tab, add or pick questions from your Question Bank, " +
                        "configure settings like time limit and attempts, then share the quiz link."
                ),
                FaqItem(
                    "Can I create a quiz using AI?",
                    "Yes — tap the AI tab and describe a topic, or attach a PDF or photos of your source " +
                        "material. AI-generated questions are added to your Question Bank for review before " +
                        "you add them to a quiz."
                )
            )
        ),
        FaqSection(
            title = "Quizzes & Questions",
            items = listOf(
                FaqItem(
                    "What question types are supported?",
                    "Single choice, multiple choice, free text, and fill-in-the-blank."
                ),
                FaqItem(
                    "Can I reuse questions across multiple quizzes?",
                    "Yes — the Question Bank stores your questions with tags and difficulty so you can pick " +
                        "the same question for as many quizzes as you like."
                ),
                FaqItem(
                    "How do I share a quiz with participants?",
                    "Every quiz gets a shareable link from its card's Share button — anyone with the link can " +
                        "open and take the quiz."
                ),
                FaqItem(
                    "Can I set a time limit?",
                    "Yes, when creating or editing a quiz you can set a total time limit for the whole quiz or " +
                        "a per-question time limit."
                ),
                FaqItem(
                    "Can participants attempt a quiz more than once?",
                    "That's a per-quiz setting — enable \"Allow multiple attempts\" when creating or editing " +
                        "the quiz if you want participants to be able to retake it."
                )
            )
        ),
        FaqSection(
            title = "Results & Reports",
            items = listOf(
                FaqItem(
                    "Where can I see who completed my quiz?",
                    "Open a quiz's Detail View for a per-respondent table, or use the Responses tab to search " +
                        "and filter completions across all your quizzes."
                ),
                FaqItem(
                    "Can I export results?",
                    "Yes — Quiz Detail View, Responses, individual response details, and the Leaderboard can " +
                        "all be exported as PDF or CSV from their toolbar icons."
                ),
                FaqItem(
                    "What does Quiz Analysis show me?",
                    "A per-question breakdown across every response to a quiz — percent correct, and how many " +
                        "people got each question right, wrong, or skipped it."
                ),
                FaqItem(
                    "Can I download a Master Paper or answer key?",
                    "Yes — from a quiz's menu you can export a question paper, a master paper with the answer " +
                        "key, or a blank offline exam paper for printing."
                )
            )
        ),
        FaqSection(
            title = "Premium & Billing",
            items = listOf(
                FaqItem(
                    "What's included in Premium?",
                    "Unlimited quizzes, unlimited Question Bank entries, unlimited responses, priority support, " +
                        "and it's fully usable on mobile."
                ),
                FaqItem(
                    "How much does Premium cost?",
                    "Pricing depends on your region and can change — check the current price on the Pricing " +
                        "screen (More → Get Premium)."
                ),
                FaqItem(
                    "How do I upgrade to Premium?",
                    "Go to More and tap the \"Get Premium\" banner, then complete checkout with Razorpay."
                ),
                FaqItem(
                    "Is my payment secure?",
                    "Payments are processed by Razorpay and verified server-side before your account is " +
                        "upgraded — Yuno LMS never sees or stores your card/UPI details."
                ),
                FaqItem(
                    "How do I check when my license expires?",
                    "Once you're a Premium customer, More shows a green banner — tap it to see your license " +
                        "details, including the expiry date."
                ),
                FaqItem(
                    "Can I renew before my license expires?",
                    "Yes — renewing early adds the new period on top of your remaining time instead of " +
                        "replacing it."
                )
            )
        ),
        FaqSection(
            title = "Account & Support",
            items = listOf(
                FaqItem(
                    "How do I update my profile or change my password?",
                    "Go to More → My Profile to update your name, business name, phone, and country, or to " +
                        "change your password."
                ),
                FaqItem(
                    "Is Yuno LMS available on desktop?",
                    "Yes — visit yunolms.com from a browser for the full desktop experience with additional " +
                        "features."
                ),
                FaqItem(
                    "How do I contact support?",
                    "Go to More → Get Help & Support to message us on WhatsApp directly."
                )
            )
        )
    )
}
