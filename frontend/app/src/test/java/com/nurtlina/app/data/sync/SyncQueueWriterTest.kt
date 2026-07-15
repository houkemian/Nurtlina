package com.nurtlina.app.data.sync

import com.nurtlina.app.data.local.dao.SyncQueueDao
import com.nurtlina.app.data.local.entity.FeedLogEntity
import com.nurtlina.app.data.local.entity.SyncQueueEntity
import com.nurtlina.app.domain.model.SessionInfo
import com.nurtlina.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SyncQueueWriterTest {

    @Test
    fun `enqueue feed log writes a ready sync queue item`() = runTest {
        val dao = FakeSyncQueueDao()
        val writer = SyncQueueWriter(dao, FakeSessionRepository())
        val timestamp = Instant.parse("2024-01-01T10:00:00Z").toEpochMilli()
        val feedLog = FeedLogEntity(
            id = "feed-1",
            babyId = "baby-1",
            bottleId = null,
            feedType = "FORMULA",
            amountMl = 120.0,
            startedAt = timestamp,
            endedAt = timestamp,
            side = null,
            note = null,
            createdAt = timestamp,
            updatedAt = timestamp,
        )

        writer.enqueueFeedLog(feedLog)

        val item = dao.items.single()
        assertEquals(SyncEntityTypes.FEED_LOG, item.entityType)
        assertEquals("feed-1", item.entityId)
        assertEquals(SyncOperations.UPSERT_FEED_LOG, item.operation)
        assertEquals(0, item.retryCount)
        assertTrue(item.payloadJson.contains("\"family_id\":\"family-1\""))
    }

    private class FakeSyncQueueDao : SyncQueueDao {
        val items = mutableListOf<SyncQueueEntity>()

        override suspend fun upsert(item: SyncQueueEntity) {
            items.removeAll { it.id == item.id }
            items += item
        }

        override suspend fun getReady(nowMillis: Long, limit: Int): List<SyncQueueEntity> =
            items.filter { it.nextRetryAt <= nowMillis }.take(limit)

        override suspend fun getById(id: String): SyncQueueEntity? = items.firstOrNull { it.id == id }

        override suspend fun delete(id: String) {
            items.removeAll { it.id == id }
        }

        override suspend fun count(): Int = items.size
    }

    private class FakeSessionRepository : SessionRepository {
        override fun observe(): Flow<SessionInfo> = flowOf(session)

        override suspend fun get(): SessionInfo = session

        override suspend fun saveBackendSession(
            backendUserId: String,
            defaultFamilyId: String,
            lastInitAt: Instant,
        ) = Unit

        override suspend fun clearBackendSession() = Unit

        private val session = SessionInfo(
            backendUserId = "user-1",
            defaultFamilyId = "family-1",
            clientId = "android-test",
            lastInitAt = Instant.parse("2024-01-01T00:00:00Z"),
        )
    }
}
