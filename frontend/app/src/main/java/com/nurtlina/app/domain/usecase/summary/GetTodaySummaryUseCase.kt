package com.nurtlina.app.domain.usecase.summary

import com.nurtlina.app.domain.model.TodaySummary
import com.nurtlina.app.domain.repository.DiaperLogRepository
import com.nurtlina.app.domain.repository.FeedLogRepository
import com.nurtlina.app.domain.repository.SleepLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

class GetTodaySummaryUseCase @Inject constructor(
    private val feedLogRepository: FeedLogRepository,
    private val diaperLogRepository: DiaperLogRepository,
    private val sleepLogRepository: SleepLogRepository,
) {
    operator fun invoke(babyId: String): Flow<TodaySummary> {
        val zone = ZoneId.systemDefault()
        val todayStart = ZonedDateTime.now(zone).toLocalDate()
            .atStartOfDay(zone).toInstant()
        val todayEnd = todayStart.plusSeconds(86400)

        return combine(
            feedLogRepository.observeByBabyAndRange(babyId, todayStart, todayEnd),
            diaperLogRepository.observeByBabyAndRange(babyId, todayStart, todayEnd),
            sleepLogRepository.observeByBabyAndRange(babyId, todayStart, todayEnd),
            sleepLogRepository.observeActiveSleep(babyId),
        ) { feeds, diapers, sleeps, activeSleep ->
            val totalAmountMl = feeds.sumOf { it.amountMl ?: 0.0 }
            val sleepDurationMillis = sleeps
                .filter { it.endedAt != null }
                .sumOf { it.durationMillis() ?: 0L }

            TodaySummary(
                totalFeedCount = feeds.size,
                totalAmountMl = totalAmountMl,
                diaperCount = diapers.size,
                sleepDurationMillis = sleepDurationMillis,
                activeSleepStartedAt = activeSleep?.startedAt?.toEpochMilli(),
            )
        }
    }
}
