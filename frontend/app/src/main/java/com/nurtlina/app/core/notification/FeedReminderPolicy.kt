package com.nurtlina.app.core.notification

import java.time.Instant

object FeedReminderPolicy {
    fun isWithinReminderAge(lastFeedStartedAt: Instant, now: Instant): Boolean =
        !now.isAfter(lastFeedStartedAt.plus(FeedReminderConfig.maximumReminderAge))
}
