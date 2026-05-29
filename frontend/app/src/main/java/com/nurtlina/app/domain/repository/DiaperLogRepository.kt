package com.nurtlina.app.domain.repository

import com.nurtlina.app.domain.model.DiaperLog
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface DiaperLogRepository {
    fun observeByBaby(babyId: String): Flow<List<DiaperLog>>
    fun observeByBabyAndRange(babyId: String, from: Instant, to: Instant): Flow<List<DiaperLog>>
    suspend fun getByBabyAndRange(babyId: String, from: Instant, to: Instant): List<DiaperLog>
    suspend fun upsert(log: DiaperLog)
    suspend fun delete(id: String)
}
