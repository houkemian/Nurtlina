package com.nurtlina.app.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.nurtlina.app.MainActivity
import com.nurtlina.app.R

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_BABY_ID = "baby_id"
        const val EXTRA_NOTIF_ID = "notif_id"
        const val EXTRA_NOTIF_TYPE = "notif_type"

        // v2.0: only next-feed reminders are active; bottle-timer types below are
        // kept for backward compatibility with any still-pending scheduled alarms.
        const val TYPE_BEFORE_EXPIRY = "before_expiry"
        const val TYPE_EXPIRED = "expired"
        const val TYPE_FEEDING_45 = "feeding_45"
        const val TYPE_FEEDING_60 = "feeding_60"
        const val TYPE_NEXT_FEED = "next_feed"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val babyId = intent.getStringExtra(EXTRA_BABY_ID)
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)
        val notifType = intent.getStringExtra(EXTRA_NOTIF_TYPE) ?: return

        // v2.0: bottle-timer notification types are no longer scheduled.
        // Ignore them gracefully if they arrive from stale pending intents.
        if (notifType != TYPE_NEXT_FEED) return

        val title = context.getString(R.string.notif_next_feed_title)
        val body = context.getString(R.string.notif_next_feed_body)

        if (!canPostNotifications(context)) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(context, manager)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            babyId?.let { putExtra(EXTRA_BABY_ID, it) }
            putExtra(EXTRA_NOTIF_TYPE, notifType)
        }
        val tapPi = PendingIntent.getActivity(
            context, notifId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationIds.CHANNEL_NEXT_FEED_SOFT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(tapPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
            .setVibrate(longArrayOf(0L))
            .build()

        manager.notify(notifId, notification)
    }

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannels(context: Context, manager: NotificationManager) {
        val softSoundAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val nextFeedChannel = NotificationChannel(
            NotificationIds.CHANNEL_NEXT_FEED_SOFT,
            context.getString(R.string.notif_channel_next_feed_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_next_feed_desc)
            setSound(Settings.System.DEFAULT_NOTIFICATION_URI, softSoundAttributes)
            enableVibration(false)
        }
        manager.createNotificationChannels(listOf(nextFeedChannel))
    }
}
