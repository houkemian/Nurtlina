package com.nurtlina.app.domain.usecase.summary

import com.nurtlina.app.domain.model.DiaperLog
import com.nurtlina.app.domain.model.DiaperType
import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.model.FeedType
import com.nurtlina.app.domain.model.SleepLog
import com.nurtlina.app.domain.repository.DiaperLogRepository
import com.nurtlina.app.domain.repository.FeedLogRepository
import com.nurtlina.app.domain.repository.SleepLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class GetTodaySummaryUseCaseTest {

    private val t0: Instant = Instant.parse("2026-06-17T10:00:00Z")

    @Test
    fun `summarizes feeds diapers and completed sleep`() = runTest {
        val feedRepo = FakeFeedLogRepository().apply {
            logs = listOf(
                feed("f1", amountMl = 120.0),
                feed("f2", amountMl = 90.0),
                feed("f3", amountMl = null), // nursing feed, no amount
            )
        }
        val diaperRepo = FakeDiaperLogRepository().apply {
            diapers = listOf(diaper("d1"), diaper("d2"))
        }
        val sleepRepo = FakeSleepLogRepository().apply {
            sleeps = listOf(
                sleep("s1", startedAt = t0, endedAt = t0.plusSeconds(3600)),
            )
            activeSleep = sleep("s2", startedAt = t0.plusSeconds(3600), endedAt = null)
        }
        val useCase = GetTodaySummaryUseCase(feedRepo, diaperRepo, sleepRepo)

        val summary = useCase("baby-1").first()

        assertEquals(3, summary.totalFeedCount)
        assertEquals(210.0, summary.totalAmountMl, 0.0)
        assertEquals(2, summary.diaperCount)
        assertEquals(3_600_000L, summary.sleepDurationMillis)
        assertEquals(t0.plusSeconds(3600).toEpochMilli(), summary.activeSleepStartedAt)
    }

    @Test
    fun `returns zeros when no logs exist`() = runTest {
        val useCase = GetTodaySummaryUseCase(
            FakeFeedLogRepository(),
            FakeDiaperLogRepository(),
            FakeSleepLogRepository(),
        )

        val summary = useCase("baby-1").first()

        assertEquals(0, summary.totalFeedCount)
        assertEquals(0.0, summary.totalAmountMl, 0.0)
        assertEquals(0, summary.diaperCount)
        assertEquals(0L, summary.sleepDurationMillis)
        assertNull(summary.activeSleepStartedAt)
    }

    private fun feed(id: String, amountMl: Double?): FeedLog = FeedLog(
        id = id,
        babyId = "baby-1",
        bottleId = null,
        feedType = FeedType.FORMULA,
        amountMl = amountMl,
        startedAt = t0,
        endedAt = t0.plusSeconds(600),
        side = null,
        note = null,
        createdAt = t0,
        updatedAt = t0,
    )

    private fun diaper(id: String): DiaperLog = DiaperLog(
        id = id,
        babyId = "baby-1",
        diaperType = DiaperType.WET,
        changedAt = t0,
        note = null,
        createdAt = t0,
        updatedAt = t0,
    )

    private fun sleep(id: String, startedAt: Instant, endedAt: Instant?): SleepLog = SleepLog(
        id = id,
        babyId = "baby-1",
        startedAt = startedAt,
        endedAt = endedAt,
        note = null,
        createdAt = t0,
        updatedAt = t0,
    )

    private class FakeFeedLogRepository : FeedLogRepository {
        var logs: List<FeedLog> = emptyList()
        override fun observeByBaby(babyId: String): Flow<List<FeedLog>> = flowOf(logs)
        override fun observeByBabyAndRange(babyId: String, from: Instant, to: Instant): Flow<List<FeedLog>> = flowOf(logs)
        override suspend fun getByBabyAndRange(babyId: String, from: Instant, to: Instant): List<FeedLog> = logs
        override suspend fun getRecentByBaby(babyId: String, limit: Int): List<FeedLog> = logs.take(limit)
        override suspend fun upsert(log: FeedLog) = Unit
        override suspend fun delete(id: String) = Unit
    }

    private class FakeDiaperLogRepository : DiaperLogRepository {
        var diapers: List<DiaperLog> = emptyList()
        override fun observeByBaby(babyId: String): Flow<List<DiaperLog>> = flowOf(diapers)
        override fun observeByBabyAndRange(babyId: String, from: Instant, to: Instant): Flow<List<DiaperLog>> = flowOf(diapers)
        override suspend fun getByBabyAndRange(babyId: String, from: Instant, to: Instant): List<DiaperLog> = diapers
        override suspend fun upsert(log: DiaperLog) = Unit
        override suspend fun delete(id: String) = Unit
    }

    private class FakeSleepLogRepository : SleepLogRepository {
        var sleeps: List<SleepLog> = emptyList()
        var activeSleep: SleepLog? = null
        override fun observeByBaby(babyId: String): Flow<List<SleepLog>> = flowOf(sleeps)
        override fun observeByBabyAndRange(babyId: String, from: Instant, to: Instant): Flow<List<SleepLog>> = flowOf(sleeps)
        override fun observeActiveSleep(babyId: String): Flow<SleepLog?> = flowOf(activeSleep)
        override suspend fun getByBabyAndRange(babyId: String, from: Instant, to: Instant): List<SleepLog> = sleeps
        override suspend fun getActiveSleep(babyId: String): SleepLog? = activeSleep
        override suspend fun upsert(log: SleepLog) = Unit
        override suspend fun delete(id: String) = Unit
    }
}
