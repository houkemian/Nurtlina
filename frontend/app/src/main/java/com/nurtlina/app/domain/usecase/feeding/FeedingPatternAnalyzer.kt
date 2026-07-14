package com.nurtlina.app.domain.usecase.feeding

import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.model.FeedingPattern
import java.time.Duration
import javax.inject.Inject

/**
 * Computes a baby's personal feeding pattern from historical [FeedLog] data.
 *
 * Uses the median interval of the last 10 valid feed-to-feed intervals as its
 * primary metric. Outliers (sleep-through, missed logs >8h, rapid corrections
 * <10min) are excluded so they don't distort the pattern.
 *
 * **Not medical logic** — purely descriptive statistics.
 */
class FeedingPatternAnalyzer @Inject constructor() {

    /** Intervals shorter than this are considered data-entry corrections. */
    private val minValidIntervalMinutes: Long = 10

    /** Intervals longer than this are considered sleep-through / missed logs. */
    private val maxValidIntervalMinutes: Long = 480 // 8 hours

    /** Fallback interval when there isn't enough data (sampleCount < 5). */
    val defaultIntervalMinutes: Int = 160

    /**
     * Analyze the given [feeds] and return a [FeedingPattern].
     *
     * @param feeds Recent feed logs (should be pre-sorted descending by startedAt).
     * @param lastFeedAmountMl Amount of the most recent feed (reserved for amount adjustment).
     * @param todayTotalAmountMl Today's total feed amount (reserved for amount adjustment).
     */
    fun analyze(
        feeds: List<FeedLog>,
        lastFeedAmountMl: Double? = null,
        todayTotalAmountMl: Double = 0.0,
    ): FeedingPattern {
        if (feeds.size < 2) {
            return FeedingPattern(
                medianIntervalMinutes = defaultIntervalMinutes,
                averageIntervalMinutes = defaultIntervalMinutes.toDouble(),
                sampleCount = 0,
            )
        }

        // Sort ascending for interval calculation
        val sorted = feeds.sortedBy { it.startedAt }

        // Calculate consecutive intervals (feed[i-1] startedAt → feed[i] startedAt)
        val intervals = sorted.zipWithNext { prev, curr ->
            Duration.between(prev.startedAt, curr.startedAt).toMinutes()
        }

        // Exclude outliers (night sleep, data-entry corrections)
        val valid = intervals.filter { it in minValidIntervalMinutes..maxValidIntervalMinutes }

        if (valid.size < 5) {
            return FeedingPattern(
                medianIntervalMinutes = defaultIntervalMinutes,
                averageIntervalMinutes = if (valid.isNotEmpty()) valid.average() else defaultIntervalMinutes.toDouble(),
                sampleCount = valid.size,
            )
        }

        val sortedValid = valid.sorted()
        val median = sortedValid[sortedValid.size / 2]

        return FeedingPattern(
            medianIntervalMinutes = median.toInt(),
            averageIntervalMinutes = valid.average(),
            sampleCount = valid.size,
        )
    }

    /**
     * Compute an amount-based adjustment to the feeding interval.
     *
     * - If the last few feeds average significantly less than the all-time average,
     *   shorten the interval (baby may be cluster-feeding / snacking).
     * - If they average significantly more, lengthen the interval.
     *
     * @return Adjustment in minutes, clamped to [-30, +30].
     */
    fun computeAmountAdjustment(
        feeds: List<FeedLog>,
        lastFeedAmountMl: Double?,
    ): Int {
        if (feeds.isEmpty() || lastFeedAmountMl == null) return 0

        val recentAmounts = feeds
            .sortedByDescending { it.startedAt }
            .take(3)
            .mapNotNull { it.amountMl }

        if (recentAmounts.isEmpty()) return 0

        val recentAvg = recentAmounts.average()
        val allTimeAvg = feeds.mapNotNull { it.amountMl }.average()
        if (allTimeAvg <= 0) return 0

        return when {
            recentAvg < allTimeAvg * 0.7 -> -30  // eating less → watch sooner
            recentAvg > allTimeAvg * 1.3 -> +30  // eating more → wait longer
            else -> 0
        }
    }
}
