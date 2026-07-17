package com.nurtlina.app.core.notification

import java.time.Duration

object FeedReminderConfig {
    /** Default fallback interval; prefer [com.nurtlina.app.domain.model.UserSettings.feedReminderIntervalMinutes]. */
    val defaultFeedIntervalMinutes: Int = 160
    val maximumReminderAge: Duration = Duration.ofHours(24)
    @Deprecated("Use defaultFeedIntervalMinutes or read from UserSettings", ReplaceWith("Duration.ofMinutes(defaultFeedIntervalMinutes.toLong())"))
    val nextFeedAttentionInterval: Duration = Duration.ofMinutes(defaultFeedIntervalMinutes.toLong())
}
