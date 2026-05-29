package com.nurtlina.app.domain.usecase.sleep

import com.nurtlina.app.domain.model.SleepLog
import com.nurtlina.app.domain.repository.SleepLogRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class SleepUseCase @Inject constructor(
    private val sleepLogRepository: SleepLogRepository,
) {
    suspend fun startSleep(babyId: String, startedAt: Instant = Instant.now()): SleepLog {
        val now = Instant.now()
        val log = SleepLog(
            id = UUID.randomUUID().toString(),
            babyId = babyId,
            startedAt = startedAt,
            endedAt = null,
            note = null,
            createdAt = now,
            updatedAt = now,
        )
        sleepLogRepository.upsert(log)
        return log
    }

    suspend fun endSleep(babyId: String, endedAt: Instant = Instant.now()): SleepLog? {
        val active = sleepLogRepository.getActiveSleep(babyId) ?: return null
        val updated = active.copy(endedAt = endedAt, updatedAt = Instant.now())
        sleepLogRepository.upsert(updated)
        return updated
    }

    suspend fun addPastSleep(
        babyId: String,
        startedAt: Instant,
        endedAt: Instant,
        note: String? = null,
    ): SleepLog {
        val now = Instant.now()
        val log = SleepLog(
            id = UUID.randomUUID().toString(),
            babyId = babyId,
            startedAt = startedAt,
            endedAt = endedAt,
            note = note,
            createdAt = now,
            updatedAt = now,
        )
        sleepLogRepository.upsert(log)
        return log
    }

    suspend fun update(log: SleepLog): SleepLog {
        val updated = log.copy(updatedAt = Instant.now())
        sleepLogRepository.upsert(updated)
        return updated
    }

    suspend fun delete(id: String) = sleepLogRepository.delete(id)
}
