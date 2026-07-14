package com.nurtlina.app.domain.usecase.insights

import com.nurtlina.app.domain.model.DiaperLog
import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.model.SleepLog
import com.nurtlina.app.domain.repository.DiaperLogRepository
import com.nurtlina.app.domain.repository.FeedLogRepository
import com.nurtlina.app.domain.repository.SleepLogRepository
import com.nurtlina.app.ui.insights.DailyDataPoint
import com.nurtlina.app.ui.insights.InsightsTrendData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Computes a multi-day summary of feed/diaper/sleep logs grouped by day.
 *
 * Used by [com.nurtlina.app.ui.insights.InsightsScreen] to render trend bar charts
 * for the last N days. The returned [Flow] reacts to changes in any of the three
 * log types within the query window.
 */
class GetMultiDaySummaryUseCase @Inject constructor(
    private val feedLogRepository: FeedLogRepository,
    private val diaperLogRepository: DiaperLogRepository,
    private val sleepLogRepository: SleepLogRepository,
) {

    /**
     * @param babyId   The baby whose logs to query.
     * @param daysBack Number of past days to include (including today).
     * @return A [Flow] of [InsightsTrendData] that emits whenever any log
     *         in the range changes.
     */
    operator fun invoke(babyId: String, daysBack: Int): Flow<InsightsTrendData> {
        val zone = ZoneId.systemDefault()
        val todayStart = ZonedDateTime.now(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val rangeStart = todayStart.minusSeconds(86_400L * (daysBack - 1))
        val rangeEnd = todayStart.plusSeconds(86_400L)

        return combine(
            feedLogRepository.observeByBabyAndRange(babyId, rangeStart, rangeEnd),
            diaperLogRepository.observeByBabyAndRange(babyId, rangeStart, rangeEnd),
            sleepLogRepository.observeByBabyAndRange(babyId, rangeStart, rangeEnd),
        ) { feeds, diapers, sleeps ->
            val today = LocalDate.now(zone)
            val feedsPerDay = groupFeedsByDay(feeds, daysBack, today)
            val diapersPerDay = groupDiapersByDay(diapers, daysBack, today)
            val sleepHoursPerDay = groupSleepByDay(sleeps, daysBack, today, zone)
            val avgAmountMl = if (feeds.isNotEmpty()) {
                feeds.mapNotNull { it.amountMl }.average()
            } else {
                null
            }

            InsightsTrendData(
                feedsPerDay = feedsPerDay,
                avgAmountMl = avgAmountMl,
                diapersPerDay = diapersPerDay,
                sleepHoursPerDay = sleepHoursPerDay,
            )
        }
    }

    // ── Grouping helpers ──────────────────────────────────────────────────────

    private fun groupFeedsByDay(
        feeds: List<FeedLog>,
        daysBack: Int,
        today: LocalDate,
    ): List<DailyDataPoint> {
        val counts = feeds.groupingBy { it.startedAt.atZone(ZoneId.systemDefault()).toLocalDate() }
            .eachCount()
        return buildDailyList(daysBack, today) { date ->
            counts[date]?.toFloat() ?: 0f
        }
    }

    private fun groupDiapersByDay(
        diapers: List<DiaperLog>,
        daysBack: Int,
        today: LocalDate,
    ): List<DailyDataPoint> {
        val counts = diapers.groupingBy { it.changedAt.atZone(ZoneId.systemDefault()).toLocalDate() }
            .eachCount()
        return buildDailyList(daysBack, today) { date ->
            counts[date]?.toFloat() ?: 0f
        }
    }

    private fun groupSleepByDay(
        sleeps: List<SleepLog>,
        daysBack: Int,
        today: LocalDate,
        zone: ZoneId,
    ): List<DailyDataPoint> {
        val completed = sleeps.filter { it.endedAt != null }
        val durationByDay = mutableMapOf<LocalDate, Long>()
        for (sleep in completed) {
            val day = sleep.startedAt.atZone(zone).toLocalDate()
            val dur = sleep.durationMillis() ?: 0L
            durationByDay[day] = (durationByDay[day] ?: 0L) + dur
        }
        return buildDailyList(daysBack, today) { date ->
            val totalMs = durationByDay[date] ?: 0L
            totalMs / 3_600_000f  // ms → hours (float)
        }
    }

    private fun buildDailyList(
        daysBack: Int,
        today: LocalDate,
        valueFor: (LocalDate) -> Float,
    ): List<DailyDataPoint> {
        return (daysBack - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            DailyDataPoint(
                label = formatDayLabel(date, daysBack),
                value = valueFor(date),
            )
        }
    }

    // ── Label formatting ──────────────────────────────────────────────────────

    private fun formatDayLabel(date: LocalDate, daysBack: Int): String {
        return when {
            daysBack <= 7 -> {
                // Short day-of-week: Mon, Tue, ...
                date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
            }
            daysBack <= 14 -> {
                // Day + abbreviated month: 7/1, 7/2, ...
                "${date.monthValue}/${date.dayOfMonth}"
            }
            else -> {
                // Just day number for 30-day view
                "${date.dayOfMonth}"
            }
        }
    }
}
