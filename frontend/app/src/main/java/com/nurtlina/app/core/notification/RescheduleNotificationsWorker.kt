package com.nurtlina.app.core.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs on device boot to restore pending notifications.
 *
 * Restores local next-feed reminders after reboot or app replacement.
 */
@HiltWorker
class RescheduleNotificationsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val nextFeedReminderCoordinator: NextFeedReminderCoordinator,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        nextFeedReminderCoordinator.refreshAll()
        return Result.success()
    }
}
