package com.nurtlina.app.domain.usecase.feed

import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.model.FeedType
import com.nurtlina.app.domain.repository.FeedLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class LogFeedUseCaseTest {

    @Test
    fun `create feed persists a new feed log and returns it`() = runTest {
        val repo = FakeFeedLogRepository()
        val useCase = LogFeedUseCase(repo)
        val startedAt = Instant.parse("2026-06-17T10:00:00Z")

        val created = useCase(
            babyId = "baby-1",
            feedType = FeedType.FORMULA,
            amountMl = 120.0,
            startedAt = startedAt,
            endedAt = startedAt.plusSeconds(600),
        )

        assertEquals("baby-1", created.babyId)
        assertEquals(FeedType.FORMULA, created.feedType)
        assertEquals(120.0, created.amountMl)
        assertTrue(created.id.isNotBlank())
        assertEquals(1, repo.logs.size)
        assertEquals(created, repo.logs.single())
    }

    @Test
    fun `update feed changes amount and persists`() = runTest {
        val repo = FakeFeedLogRepository()
        val useCase = LogFeedUseCase(repo)
        val log = useCase(
            babyId = "baby-1",
            feedType = FeedType.FORMULA,
            amountMl = 120.0,
            startedAt = Instant.now(),
            endedAt = null,
        )

        val updated = useCase.update(log.copy(amountMl = 150.0))

        assertEquals(150.0, updated.amountMl)
        assertEquals(150.0, repo.logs.single().amountMl)
    }

    @Test
    fun `delete feed removes it from the repository`() = runTest {
        val repo = FakeFeedLogRepository()
        val useCase = LogFeedUseCase(repo)
        val log = useCase(
            babyId = "baby-1",
            feedType = FeedType.BREAST_MILK,
            amountMl = null,
            startedAt = Instant.now(),
            endedAt = null,
        )

        useCase.delete(log.id)

        assertTrue(repo.logs.isEmpty())
    }

    private class FakeFeedLogRepository : FeedLogRepository {
        val logs = mutableListOf<FeedLog>()

        override fun observeByBaby(babyId: String): Flow<List<FeedLog>> =
            flowOf(logs.filter { it.babyId == babyId })

        override fun observeByBabyAndRange(babyId: String, from: Instant, to: Instant): Flow<List<FeedLog>> =
            flowOf(logs)

        override suspend fun getByBabyAndRange(babyId: String, from: Instant, to: Instant): List<FeedLog> =
            logs

        override suspend fun getRecentByBaby(babyId: String, limit: Int): List<FeedLog> =
            logs.filter { it.babyId == babyId }.take(limit)

        override suspend fun upsert(log: FeedLog) {
            logs.removeAll { it.id == log.id }
            logs += log
        }

        override suspend fun delete(id: String) {
            logs.removeAll { it.id == id }
        }
    }
}
