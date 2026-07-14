package com.nurtlina.app.domain.usecase.feeding

import com.nurtlina.app.domain.model.Confidence
import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.model.FeedingPrediction
import com.nurtlina.app.domain.model.FeedingReminderFeedback
import com.nurtlina.app.domain.model.FeedbackType
import com.nurtlina.app.domain.repository.FeedLogRepository
import com.nurtlina.app.domain.repository.FeedingFeedbackRepository
import java.time.Instant
import javax.inject.Inject

/**
 * Generates a gentle feeding-window prediction based on the baby's recent
 * patterns, amount trends, and user feedback.
 *
 * **Not a prescription** — output is a suggestion window, not a "must feed" time.
 */
class GenerateFeedingPredictionUseCase @Inject constructor(
    private val feedLogRepository: FeedLogRepository,
    private val feedbackRepository: FeedingFeedbackRepository,
    private val patternAnalyzer: FeedingPatternAnalyzer,
) {

    /** How far before the predicted time the window opens (minutes). */
    private val windowLeadMinutes: Long = 20

    /** How far after the predicted time the window closes (minutes). */
    private val windowTrailMinutes: Long = 30

    /**
     * Generate a [FeedingPrediction] for the given baby.
     *
     * @param babyId The baby to predict for.
     * @param now Current time (injectable for testability).
     */
    suspend operator fun invoke(babyId: String, now: Instant = Instant.now()): FeedingPrediction {
        val recentFeeds = feedLogRepository.getRecentByBaby(babyId, limit = 15)

        if (recentFeeds.isEmpty()) {
            return FeedingPrediction(
                babyId = babyId,
                windowStart = now,
                windowEnd = now,
                confidence = Confidence.LOW,
                reason = "Log a few feeds to learn your baby's pattern",
                personalizedIntervalMinutes = patternAnalyzer.defaultIntervalMinutes,
                isLearning = true,
            )
        }

        val latestFeed = recentFeeds.maxByOrNull { it.startedAt }!!
        val lastFeedAmountMl = latestFeed.amountMl

        // 1. Pattern from recent feeds
        val pattern = patternAnalyzer.analyze(recentFeeds, lastFeedAmountMl)

        // 2. Amount adjustment
        val amountAdj = patternAnalyzer.computeAmountAdjustment(recentFeeds, lastFeedAmountMl)

        // 3. Feedback adjustment
        val recentFeedback = feedbackRepository.getRecentByBaby(babyId, limit = 10)
        val feedbackAdj = computeFeedbackAdjustment(recentFeedback)

        // 4. Personalized interval
        val personalized = (pattern.medianIntervalMinutes + amountAdj + feedbackAdj)
            .coerceIn(90, 300) // 1.5h to 5h hard bounds

        // 5. Window
        val predictedCenter = latestFeed.startedAt.plusSeconds(personalized * 60L)
        val windowStart = predictedCenter.minusSeconds(windowLeadMinutes * 60L)
        val windowEnd = predictedCenter.plusSeconds(windowTrailMinutes * 60L)

        // 6. Confidence
        val confidence = when {
            pattern.sampleCount >= 8 && abs(feedbackAdj) <= 5 -> Confidence.HIGH
            pattern.sampleCount >= 5 -> Confidence.MEDIUM
            else -> Confidence.LOW
        }

        return FeedingPrediction(
            babyId = babyId,
            windowStart = windowStart,
            windowEnd = windowEnd,
            confidence = confidence,
            reason = "Based on recent feeding patterns",
            personalizedIntervalMinutes = personalized,
            isLearning = false,
        )
    }

    // ── Feedback adjustment ───────────────────────────────────────────────────

    private fun computeFeedbackAdjustment(feedback: List<FeedingReminderFeedback>): Int {
        if (feedback.isEmpty()) return 0

        val counts = feedback.groupingBy { it.feedbackType }.eachCount()
        var adjustment = 0

        if ((counts[FeedbackType.TOO_LATE] ?: 0) >= 3) {
            adjustment -= 15  // user says window is too late → move earlier
        }
        if ((counts[FeedbackType.TOO_EARLY] ?: 0) >= 3) {
            adjustment += 15  // user says window is too early → move later
        }

        return adjustment.coerceIn(-30, 30)
    }

    companion object {
        private fun abs(n: Int) = if (n < 0) -n else n
    }
}
