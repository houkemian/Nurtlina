package com.nurtlina.app.domain.model

import java.time.Instant

/** Confidence level of a feeding-window prediction. */
enum class Confidence { LOW, MEDIUM, HIGH }

/**
 * A gentle feeding-window suggestion based on the baby's recent patterns.
 *
 * **Not a prescription** — this is a "you may want to start watching for
 * feeding cues" window, not a "baby must eat now" directive.
 */
data class FeedingPrediction(
    val babyId: String,
    /** Start of the attention window (epoch millis). */
    val windowStart: Instant,
    /** End of the attention window (epoch millis). */
    val windowEnd: Instant,
    /** How much the pattern data supports this window. */
    val confidence: Confidence,
    /**
     * Human-readable explanation shown in the UI, e.g.
     * "Based on recent feeding patterns".
     */
    val reason: String,
    /**
     * The personalized interval (minutes) used to compute this window,
     * after median + amount adjustment + feedback adjustment.
     */
    val personalizedIntervalMinutes: Int,
    /** True when there is not enough historical data to suggest a window. */
    val isLearning: Boolean = false,
)
