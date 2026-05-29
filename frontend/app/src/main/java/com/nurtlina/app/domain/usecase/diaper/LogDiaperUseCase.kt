package com.nurtlina.app.domain.usecase.diaper

import com.nurtlina.app.domain.model.DiaperLog
import com.nurtlina.app.domain.model.DiaperType
import com.nurtlina.app.domain.repository.DiaperLogRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class LogDiaperUseCase @Inject constructor(
    private val diaperLogRepository: DiaperLogRepository,
) {
    suspend operator fun invoke(
        babyId: String,
        diaperType: DiaperType,
        changedAt: Instant = Instant.now(),
        note: String? = null,
    ): DiaperLog {
        val now = Instant.now()
        val log = DiaperLog(
            id = UUID.randomUUID().toString(),
            babyId = babyId,
            diaperType = diaperType,
            changedAt = changedAt,
            note = note,
            createdAt = now,
            updatedAt = now,
        )
        diaperLogRepository.upsert(log)
        return log
    }

    suspend fun update(log: DiaperLog): DiaperLog {
        val updated = log.copy(updatedAt = Instant.now())
        diaperLogRepository.upsert(updated)
        return updated
    }

    suspend fun delete(id: String) = diaperLogRepository.delete(id)
}
