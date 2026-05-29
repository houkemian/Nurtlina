package com.nurtlina.app.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nurtlina.app.core.notification.RescheduleNotificationsWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val work = OneTimeWorkRequestBuilder<RescheduleNotificationsWorker>().build()
            WorkManager.getInstance(context).enqueue(work)
        }
    }
}
