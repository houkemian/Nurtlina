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
        const val EXTRA_BOTTLE_ID = "bottle_id"
        const val EXTRA_BABY_ID = "baby_id"
        const val EXTRA_NOTIF_ID = "notif_id"
        const val EXTRA_NOTIF_TYPE = "notif_type"

        const val TYPE_BEFORE_EXPIRY = "before_expiry"
        const val TYPE_EXPIRED = "expired"
        const val TYPE_FEEDING_45 = "feeding_45"
        const val TYPE_FEEDING_60 = "feeding_60"
        const val TYPE_NEXT_FEED = "next_feed"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val bottleId = intent.getStringExtra(EXTRA_BOTTLE_ID)
        val babyId = intent.getStringExtra(EXTRA_BABY_ID)
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)
        val notifType = intent.getStringExtra(EXTRA_NOTIF_TYPE) ?: return
        if (notifType != TYPE_NEXT_FEED && bottleId == null) return

        val (title, body) = when (notifType) {
            TYPE_BEFORE_EXPIRY -> Pair(
                context.getString(R.string.notif_before_expiry_title),
                context.getString(R.string.notif_before_expiry_body)
            )
            TYPE_EXPIRED -> Pair(
                context.getString(R.string.notif_expired_title),
                context.getString(R.string.notif_expired_body)
            )
            TYPE_FEEDING_45 -> Pair(
                context.getString(R.string.notif_feeding_45_title),
                context.getString(R.string.notif_feeding_45_body)
            )
            TYPE_FEEDING_60 -> Pair(
                context.getString(R.string.notif_feeding_60_title),
                context.getString(R.string.notif_feeding_60_body)
            )
            TYPE_NEXT_FEED -> Pair(
                context.getString(R.string.notif_next_feed_title),
                context.getString(R.string.notif_next_feed_body)
            )
            else -> return
        }

        if (!canPostNotifications(context)) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(context, manager)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            bottleId?.let { putExtra(EXTRA_BOTTLE_ID, it) }
            babyId?.let { putExtra(EXTRA_BABY_ID, it) }
            putExtra(EXTRA_NOTIF_TYPE, notifType)
        }
        val tapPi = PendingIntent.getActivity(
            context, notifId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = when (notifType) {
            TYPE_NEXT_FEED -> NotificationIds.CHANNEL_NEXT_FEED_SOFT
            TYPE_FEEDING_45,
            TYPE_FEEDING_60 -> NotificationIds.CHANNEL_FEEDING
            else -> NotificationIds.CHANNEL_BOTTLE_TIMER
        }
        val priority = if (notifType == TYPE_NEXT_FEED) {
            NotificationCompat.PRIORITY_DEFAULT
        } else {
            NotificationCompat.PRIORITY_HIGH
        }

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(tapPi)
            .setAutoCancel(true)
            .setPriority(priority)
            .setVibrate(if (notifType == TYPE_NEXT_FEED) longArrayOf(0L) else null)

        if (notifType == TYPE_NEXT_FEED) {
            notificationBuilder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
        }

        val notification = notificationBuilder.build()

        manager.notify(notifId, notification)
    }

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannels(context: Context, manager: NotificationManager) {
        val bottleChannel = NotificationChannel(
            NotificationIds.CHANNEL_BOTTLE_TIMER,
            context.getString(R.string.notif_channel_bottle_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_bottle_desc)
        }
        val feedingChannel = NotificationChannel(
            NotificationIds.CHANNEL_FEEDING,
            context.getString(R.string.notif_channel_feeding_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_feeding_desc)
        }
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
        manager.createNotificationChannels(listOf(bottleChannel, feedingChannel, nextFeedChannel))
    }
}
