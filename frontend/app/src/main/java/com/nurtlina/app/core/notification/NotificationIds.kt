package com.nurtlina.app.core.notification

object NotificationIds {
    const val CHANNEL_BOTTLE_TIMER = "bottle_timer"
    const val CHANNEL_FEEDING = "feeding_reminder"

    fun bottleBeforeExpiry(bottleId: String) = "bottle_before_${bottleId}"
    fun bottleExpired(bottleId: String) = "bottle_expired_${bottleId}"
    fun feedingReminder45(bottleId: String) = "feeding_45_${bottleId}"
    fun feedingReminder60(bottleId: String) = "feeding_60_${bottleId}"

    fun bottleBeforeExpiryInt(bottleId: String): Int = bottleBeforeExpiry(bottleId).hashCode()
    fun bottleExpiredInt(bottleId: String): Int = bottleExpired(bottleId).hashCode()
    fun feedingReminder45Int(bottleId: String): Int = feedingReminder45(bottleId).hashCode()
    fun feedingReminder60Int(bottleId: String): Int = feedingReminder60(bottleId).hashCode()
}
