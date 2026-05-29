package com.nurtlina.app.domain.usecase.feed

import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.model.FeedType
import com.nurtlina.app.domain.model.NursingSide
import com.nurtlina.app.domain.repository.FeedLogRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class LogFeedUseCase @Inject constructor(
    private val feedLogRepository: FeedLogRepository,
) {
    suspend operator fun invoke(
        babyId: String,
        feedType: FeedType,
        amountMl: Double?,
        startedAt: Instant,
        endedAt: Instant?,
        bottleId: String? = null,
        side: NursingSide? = null,
        note: String? = null,
    ): FeedLog {
        val now = Instant.now()
        val log = FeedLog(
            id = UUID.randomUUID().toString(),
            babyId = babyId,
            bottleId = bottleId,
            feedType = feedType,
            amountMl = amountMl,
            startedAt = startedAt,
            endedAt = endedAt,
            side = side,
            note = note,
            createdAt = now,
            updatedAt = now,
        )
        feedLogRepository.upsert(log)
        return log
    }

    suspend fun update(log: FeedLog): FeedLog {
        val updated = log.copy(updatedAt = Instant.now())
        feedLogRepository.upsert(updated)
        return updated
    }

    suspend fun delete(id: String) = feedLogRepository.delete(id)
}
