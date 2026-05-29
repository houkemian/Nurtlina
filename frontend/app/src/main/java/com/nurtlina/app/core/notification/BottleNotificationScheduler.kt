package com.nurtlina.app.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.BottleStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BottleNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(bottle: Bottle, reminderBeforeMinutes: Int = 15) {
        if (bottle.status.isTerminal) return
        val expiresAt = bottle.expiresAt ?: return
        val now = Instant.now()

        val beforeExpiry = expiresAt.minusSeconds(reminderBeforeMinutes * 60L)
        if (beforeExpiry.isAfter(now)) {
            scheduleAlarm(
                tag = NotificationIds.bottleBeforeExpiry(bottle.id),
                notifId = NotificationIds.bottleBeforeExpiryInt(bottle.id),
                bottleId = bottle.id,
                triggerAtMillis = beforeExpiry.toEpochMilli(),
                notifType = NotificationReceiver.TYPE_BEFORE_EXPIRY,
            )
        }

        if (expiresAt.isAfter(now)) {
            scheduleAlarm(
                tag = NotificationIds.bottleExpired(bottle.id),
                notifId = NotificationIds.bottleExpiredInt(bottle.id),
                bottleId = bottle.id,
                triggerAtMillis = expiresAt.toEpochMilli(),
                notifType = NotificationReceiver.TYPE_EXPIRED,
            )
        }

        if (bottle.status == BottleStatus.FEEDING_STARTED) {
            val feedingStart = bottle.feedingStartedAt ?: return
            val at45 = feedingStart.plusSeconds(45 * 60L)
            val at60 = feedingStart.plusSeconds(60 * 60L)

            if (at45.isAfter(now)) {
                scheduleAlarm(
                    tag = NotificationIds.feedingReminder45(bottle.id),
                    notifId = NotificationIds.feedingReminder45Int(bottle.id),
                    bottleId = bottle.id,
                    triggerAtMillis = at45.toEpochMilli(),
                    notifType = NotificationReceiver.TYPE_FEEDING_45,
                )
            }
            if (at60.isAfter(now)) {
                scheduleAlarm(
                    tag = NotificationIds.feedingReminder60(bottle.id),
                    notifId = NotificationIds.feedingReminder60Int(bottle.id),
                    bottleId = bottle.id,
                    triggerAtMillis = at60.toEpochMilli(),
                    notifType = NotificationReceiver.TYPE_FEEDING_60,
                )
            }
        }
    }

    fun cancel(bottleId: String) {
        listOf(
            NotificationIds.bottleBeforeExpiryInt(bottleId) to NotificationIds.bottleBeforeExpiry(bottleId),
            NotificationIds.bottleExpiredInt(bottleId) to NotificationIds.bottleExpired(bottleId),
            NotificationIds.feedingReminder45Int(bottleId) to NotificationIds.feedingReminder45(bottleId),
            NotificationIds.feedingReminder60Int(bottleId) to NotificationIds.feedingReminder60(bottleId),
        ).forEach { (notifId, _) ->
            val intent = Intent(context, NotificationReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context, notifId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pi)
        }
    }

    private fun scheduleAlarm(
        tag: String,
        notifId: Int,
        bottleId: String,
        triggerAtMillis: Long,
        notifType: String,
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_BOTTLE_ID, bottleId)
            putExtra(NotificationReceiver.EXTRA_NOTIF_ID, notifId)
            putExtra(NotificationReceiver.EXTRA_NOTIF_TYPE, notifType)
        }
        val pi = PendingIntent.getBroadcast(
            context, notifId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }
}
