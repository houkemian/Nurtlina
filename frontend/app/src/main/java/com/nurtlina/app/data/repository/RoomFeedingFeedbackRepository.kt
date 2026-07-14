package com.nurtlina.app.data.repository

import com.nurtlina.app.data.local.dao.FeedingReminderFeedbackDao
import com.nurtlina.app.data.local.entity.FeedingReminderFeedbackEntity
import com.nurtlina.app.domain.model.FeedingReminderFeedback
import com.nurtlina.app.domain.repository.FeedingFeedbackRepository
import javax.inject.Inject

class RoomFeedingFeedbackRepository @Inject constructor(
    private val dao: FeedingReminderFeedbackDao,
) : FeedingFeedbackRepository {

    override suspend fun record(feedback: FeedingReminderFeedback) {
        dao.insert(FeedingReminderFeedbackEntity.fromDomain(feedback))
    }

    override suspend fun getRecentByBaby(babyId: String, limit: Int): List<FeedingReminderFeedback> =
        dao.getRecentByBaby(babyId, limit).map { it.toDomain() }
}
