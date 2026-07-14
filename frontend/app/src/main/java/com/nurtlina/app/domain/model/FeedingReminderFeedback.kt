package com.nurtlina.app.domain.model

import java.time.Instant

enum class FeedbackType {
    TOO_EARLY,
    JUST_RIGHT,
    TOO_LATE,
}

data class FeedingReminderFeedback(
    val id: String,
    val babyId: String,
    val reminderTime: Instant,
    val feedbackType: FeedbackType,
    val createdAt: Instant,
)
