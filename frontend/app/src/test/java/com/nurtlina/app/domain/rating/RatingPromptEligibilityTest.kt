package com.nurtlina.app.domain.rating

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingPromptEligibilityTest {
    private val eligibility = RatingPromptEligibility()
    private val now: Instant = Instant.parse("2026-06-17T12:00:00Z")

    @Test
    fun eligibleAfterMinimumUsage() {
        val decision = evaluate(eligibleState())
        assertEquals(RatingPromptDecision.Eligible, decision)
    }

    @Test
    fun blocksDuringNightMode() {
        val decision = evaluate(eligibleState(), nightModeEnabled = true)
        assertBlocked(RatingPromptBlockedReason.NIGHT_MODE, decision)
    }

    @Test
    fun blocksUntilEnoughFeedsAreLogged() {
        val decision = evaluate(eligibleState(feedLoggedCount = 4))
        assertBlocked(RatingPromptBlockedReason.NOT_ENOUGH_FEEDS, decision)
    }

    @Test
    fun blocksDuringCooldownAfterMaybeLater() {
        val decision = evaluate(
            eligibleState(ratingPromptLastShownAt = now.minusSeconds(29L * 24L * 60L * 60L))
        )
        assertBlocked(RatingPromptBlockedReason.COOLDOWN, decision)
    }

    @Test
    fun blocksRecentNotificationSession() {
        val decision = evaluate(eligibleState(lastNotificationOpenAt = now.minusSeconds(60)))
        assertBlocked(RatingPromptBlockedReason.RECENT_NOTIFICATION_SESSION, decision)
    }

    @Test
    fun blocksRecentNegativeAction() {
        val decision = evaluate(eligibleState(lastNegativeActionAt = now.minusSeconds(60)))
        assertBlocked(RatingPromptBlockedReason.RECENT_NEGATIVE_ACTION, decision)
    }

    private fun evaluate(
        state: RatingPromptState,
        nightModeEnabled: Boolean = false,
    ): RatingPromptDecision = eligibility.evaluate(
        state = state,
        nightModeEnabled = nightModeEnabled,
        alreadyShownThisSession = false,
        now = now,
    )

    private fun eligibleState(
        ratingPromptLastShownAt: Instant? = null,
        lastNotificationOpenAt: Instant? = null,
        lastNegativeActionAt: Instant? = null,
        feedLoggedCount: Int = 5,
    ): RatingPromptState = RatingPromptState(
        ratingPromptShownCount = 0,
        ratingPromptLastShownAt = ratingPromptLastShownAt,
        eligiblePositiveActionCount = 3,
        feedLoggedCount = feedLoggedCount,
        firstLaunchAt = now.minusSeconds(4L * 24L * 60L * 60L),
        lastNotificationOpenAt = lastNotificationOpenAt,
        lastNegativeActionAt = lastNegativeActionAt,
    )

    private fun assertBlocked(
        reason: RatingPromptBlockedReason,
        decision: RatingPromptDecision,
    ) {
        assertTrue(decision is RatingPromptDecision.Blocked)
        assertEquals(reason, (decision as RatingPromptDecision.Blocked).reason)
    }
}
