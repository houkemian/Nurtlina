package com.nurtlina.app.core.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nurtlina.app.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs on device boot to restore pending notifications.
 *
 * Since v2.0 removed the bottle timer system, there are no active-bottle
 * notifications to reschedule. Next-feed reminders are scheduled reactively
 * when a feed is logged (via [NextFeedNotificationScheduler]), so they don't
 * need a boot-time restore pass.
 */
@HiltWorker
class RescheduleNotificationsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Bottle timer notifications are removed in v2.0.
        // Next-feed reminders are scheduled on-demand when feeds are logged.
        return Result.success()
    }
}
