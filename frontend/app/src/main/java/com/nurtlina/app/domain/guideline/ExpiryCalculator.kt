package com.nurtlina.app.domain.guideline

import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.BottleStatus
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Calculates bottle expiry times based on guideline rules.
 * This is pure business logic — no Android framework dependencies.
 * All time math is in minutes to avoid floating-point issues.
 */
object ExpiryCalculator {

    /**
     * Returns the expiry [Instant] for a given bottle and rule.
     * Returns null if the bottle status is terminal or has no expiry.
     */
    fun calculate(bottle: Bottle, rule: GuidelineRule): Instant? {
        return when (bottle.status) {
            BottleStatus.NOT_STARTED -> {
                val minutes = rule.roomTempMinutes ?: return null
                bottle.preparedAt.plus(minutes.toLong(), ChronoUnit.MINUTES)
            }
            BottleStatus.REFRIGERATED -> {
                val minutes = rule.refrigeratedMinutes ?: return null
                bottle.preparedAt.plus(minutes.toLong(), ChronoUnit.MINUTES)
            }
            BottleStatus.FEEDING_STARTED -> {
                val minutes = rule.feedingStartedMinutes ?: return null
                val base = bottle.feedingStartedAt ?: return null
                base.plus(minutes.toLong(), ChronoUnit.MINUTES)
            }
            BottleStatus.EXPIRED -> bottle.expiresAt
            BottleStatus.FED,
            BottleStatus.DISCARDED,
            BottleStatus.CANCELED -> null
        }
    }

    fun isExpired(expiresAt: Instant?, now: Instant): Boolean {
        return expiresAt != null && now.isAfter(expiresAt)
    }

    fun minutesUntilExpiry(expiresAt: Instant, now: Instant): Long {
        return ChronoUnit.MINUTES.between(now, expiresAt)
    }
}
