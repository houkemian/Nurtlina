package com.nurtlina.app.core.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class FeedReminderPolicyTest {
    private val now = Instant.parse("2026-07-17T12:00:00Z")

    @Test
    fun `reminder is allowed before 24 hours`() {
        val lastFeed = now.minus(Duration.ofHours(23)).minus(Duration.ofMinutes(59))
        assertTrue(FeedReminderPolicy.isWithinReminderAge(lastFeed, now))
    }

    @Test
    fun `reminder is allowed at exactly 24 hours`() {
        assertTrue(FeedReminderPolicy.isWithinReminderAge(now.minus(Duration.ofHours(24)), now))
    }

    @Test
    fun `reminder is stopped after 24 hours`() {
        val lastFeed = now.minus(Duration.ofHours(24)).minusMillis(1)
        assertFalse(FeedReminderPolicy.isWithinReminderAge(lastFeed, now))
    }
}
