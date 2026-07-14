package com.nurtlina.app.domain.usecase.feeding

import com.nurtlina.app.domain.model.FeedingReminderFeedback
import com.nurtlina.app.domain.model.FeedbackType
import com.nurtlina.app.domain.repository.FeedingFeedbackRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class RecordFeedingFeedbackUseCase @Inject constructor(
    private val feedbackRepository: FeedingFeedbackRepository,
) {
    suspend operator fun invoke(
        babyId: String,
        reminderTime: Instant,
        feedbackType: FeedbackType,
    ) {
        feedbackRepository.record(
            FeedingReminderFeedback(
                id = UUID.randomUUID().toString(),
                babyId = babyId,
                reminderTime = reminderTime,
                feedbackType = feedbackType,
                createdAt = Instant.now(),
            )
        )
    }
}
