package com.nurtlina.app.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NextFeedNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(babyId: String, lastFeedStartedAt: Instant, intervalMinutes: Int = FeedReminderConfig.defaultFeedIntervalMinutes) {
        val triggerAt = lastFeedStartedAt.plus(Duration.ofMinutes(intervalMinutes.toLong()))
        scheduleAt(babyId, triggerAt)
    }

    /** Schedule a reminder to fire at the beginning of a feeding window. */
    fun scheduleWindow(babyId: String, windowStart: Instant) {
        scheduleAt(babyId, windowStart)
    }

    private fun scheduleAt(babyId: String, triggerAt: Instant) {
        if (!triggerAt.isAfter(Instant.now())) return

        val notifId = NotificationIds.nextFeedReminderInt(babyId)
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_BABY_ID, babyId)
            putExtra(NotificationReceiver.EXTRA_NOTIF_ID, notifId)
            putExtra(NotificationReceiver.EXTRA_NOTIF_TYPE, NotificationReceiver.TYPE_NEXT_FEED)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt.toEpochMilli(), pi)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt.toEpochMilli(), pi)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt.toEpochMilli(), pi)
        }
    }

    fun cancel(babyId: String) {
        val notifId = NotificationIds.nextFeedReminderInt(babyId)
        val intent = Intent(context, NotificationReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pi)
    }
}
