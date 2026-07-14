package com.nurtlina.app.domain.repository

import com.nurtlina.app.domain.model.FeedingReminderFeedback

interface FeedingFeedbackRepository {
    suspend fun record(feedback: FeedingReminderFeedback)
    suspend fun getRecentByBaby(babyId: String, limit: Int = 10): List<FeedingReminderFeedback>
}
