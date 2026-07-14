package com.nurtlina.app.domain.model

/**
 * Computed feeding pattern from historical [FeedLog] data.
 *
 * **Not medical advice** — purely descriptive statistics of a baby's recent
 * feeding rhythm, used to suggest a gentle attention window.
 */
data class FeedingPattern(
    /** Median interval between consecutive feeds (minutes). The primary metric. */
    val medianIntervalMinutes: Int,
    /** Arithmetic mean interval (minutes). Informational, not used for predictions. */
    val averageIntervalMinutes: Double,
    /** Number of valid intervals that contributed to this pattern (≥5 for reliable). */
    val sampleCount: Int,
) {
    val isReliable: Boolean get() = sampleCount >= 5
}
