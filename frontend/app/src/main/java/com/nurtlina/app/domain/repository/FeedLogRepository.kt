package com.nurtlina.app.domain.repository

import com.nurtlina.app.domain.model.FeedLog
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface FeedLogRepository {
    fun observeByBaby(babyId: String): Flow<List<FeedLog>>
    fun observeByBabyAndRange(babyId: String, from: Instant, to: Instant): Flow<List<FeedLog>>
    suspend fun getByBabyAndRange(babyId: String, from: Instant, to: Instant): List<FeedLog>
    suspend fun getRecentByBaby(babyId: String, limit: Int = 10): List<FeedLog>
    suspend fun upsert(log: FeedLog)
    suspend fun delete(id: String)
}
