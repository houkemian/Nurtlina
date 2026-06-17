package com.nurtlina.app.domain.rating

import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.BottleStatus
import com.nurtlina.app.domain.model.GuidelineRegion
import com.nurtlina.app.domain.model.MilkType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingPromptEligibilityTest {
    private val eligibility = RatingPromptEligibility()
    private val now: Instant = Instant.parse("2026-06-17T12:00:00Z")

    @Test
    fun eligibleAfterMinimumUsageAndNoActiveRisk() {
        val decision = eligibility.evaluate(
            state = eligibleState(),
            activeBottles = emptyList(),
            nightModeEnabled = false,
            alreadyShownThisSession = false,
            now = now,
        )

        assertEquals(RatingPromptDecision.Eligible, decision)
    }

    @Test
    fun blocksDuringNightMode() {
        val decision = eligibility.evaluate(
            state = eligibleState(),
            activeBottles = emptyList(),
            nightModeEnabled = true,
            alreadyShownThisSession = false,
            now = now,
        )

        assertBlocked(RatingPromptBlockedReason.NIGHT_MODE, decision)
    }

    @Test
    fun blocksWhenBottleIsExpired() {
        val decision = eligibility.evaluate(
            state = eligibleState(),
            activeBottles = listOf(bottle(status = BottleStatus.NOT_STARTED, expiresAt = now.minusSeconds(1))),
            nightModeEnabled = false,
            alreadyShownThisSession = false,
            now = now,
        )

        assertBlocked(RatingPromptBlockedReason.ACTIVE_EXPIRED_BOTTLE, decision)
    }

    @Test
    fun blocksDuringCooldownAfterMaybeLater() {
        val decision = eligibility.evaluate(
            state = eligibleState(ratingPromptLastShownAt = now.minusSeconds(29L * 24L * 60L * 60L)),
            activeBottles = emptyList(),
            nightModeEnabled = false,
            alreadyShownThisSession = false,
            now = now,
        )

        assertBlocked(RatingPromptBlockedReason.COOLDOWN, decision)
    }

    @Test
    fun blocksRecentNotificationSession() {
        val decision = eligibility.evaluate(
            state = eligibleState(lastNotificationOpenAt = now.minusSeconds(60)),
            activeBottles = emptyList(),
            nightModeEnabled = false,
            alreadyShownThisSession = false,
            now = now,
        )

        assertBlocked(RatingPromptBlockedReason.RECENT_NOTIFICATION_SESSION, decision)
    }

    @Test
    fun blocksRecentNegativeAction() {
        val decision = eligibility.evaluate(
            state = eligibleState(lastNegativeActionAt = now.minusSeconds(60)),
            activeBottles = emptyList(),
            nightModeEnabled = false,
            alreadyShownThisSession = false,
            now = now,
        )

        assertBlocked(RatingPromptBlockedReason.RECENT_NEGATIVE_ACTION, decision)
    }

    private fun eligibleState(
        ratingPromptLastShownAt: Instant? = null,
        lastNotificationOpenAt: Instant? = null,
        lastNegativeActionAt: Instant? = null,
    ): RatingPromptState = RatingPromptState(
        ratingPromptShownCount = 0,
        ratingPromptLastShownAt = ratingPromptLastShownAt,
        eligiblePositiveActionCount = 3,
        bottleTimerCreatedCount = 5,
        firstLaunchAt = now.minusSeconds(4L * 24L * 60L * 60L),
        lastNotificationOpenAt = lastNotificationOpenAt,
        lastNegativeActionAt = lastNegativeActionAt,
    )

    private fun bottle(
        status: BottleStatus,
        expiresAt: Instant?,
    ): Bottle = Bottle(
        id = "bottle-id",
        babyId = "baby-id",
        milkType = MilkType.FORMULA,
        amountMl = 120.0,
        preparedAt = now.minusSeconds(60),
        feedingStartedAt = null,
        refrigeratedAt = null,
        status = status,
        guidelineRegion = GuidelineRegion.US,
        expiresAt = expiresAt,
        discardedAt = null,
        fedAt = null,
        note = null,
        createdAt = now.minusSeconds(60),
        updatedAt = now.minusSeconds(60),
    )

    private fun assertBlocked(
        reason: RatingPromptBlockedReason,
        decision: RatingPromptDecision,
    ) {
        assertTrue(decision is RatingPromptDecision.Blocked)
        assertEquals(reason, (decision as RatingPromptDecision.Blocked).reason)
    }
}
