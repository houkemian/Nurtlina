package com.nurtlina.app.domain.repository

import com.nurtlina.app.domain.model.SleepLog
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface SleepLogRepository {
    fun observeByBaby(babyId: String): Flow<List<SleepLog>>
    fun observeByBabyAndRange(babyId: String, from: Instant, to: Instant): Flow<List<SleepLog>>
    fun observeActiveSleep(babyId: String): Flow<SleepLog?>
    suspend fun getByBabyAndRange(babyId: String, from: Instant, to: Instant): List<SleepLog>
    suspend fun getActiveSleep(babyId: String): SleepLog?
    suspend fun upsert(log: SleepLog)
    suspend fun delete(id: String)
}
