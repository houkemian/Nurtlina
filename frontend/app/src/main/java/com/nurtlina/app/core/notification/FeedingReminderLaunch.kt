package com.nurtlina.app.core.notification

data class FeedingReminderLaunch(
    val babyId: String,
    val token: Long = System.nanoTime(),
)
