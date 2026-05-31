package com.nurtlina.app.core.notification

import java.time.Duration

object FeedReminderConfig {
    const val DEBUG_NEXT_FEED_REMINDER_ENABLED = true

    val nextFeedAttentionInterval: Duration
        get() = if (DEBUG_NEXT_FEED_REMINDER_ENABLED) {
            Duration.ofSeconds(60)
        } else {
            Duration.ofMinutes(160)
        }
}
