package com.nurtlina.app.core.notification

object NotificationIds {
    const val CHANNEL_FEEDING = "feeding_reminder"
    const val CHANNEL_NEXT_FEED_SOFT = "next_feed_reminder_soft"

    fun nextFeedReminder(babyId: String) = "next_feed_${babyId}"
    fun nextFeedReminderInt(babyId: String): Int = nextFeedReminder(babyId).hashCode()
}
