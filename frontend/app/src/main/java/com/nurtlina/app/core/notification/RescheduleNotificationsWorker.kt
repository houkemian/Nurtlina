package com.nurtlina.app.core.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nurtlina.app.domain.repository.BottleRepository
import com.nurtlina.app.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RescheduleNotificationsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val bottleRepository: BottleRepository,
    private val settingsRepository: SettingsRepository,
    private val scheduler: BottleNotificationScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val settings = settingsRepository.get()
            if (!settings.notificationEnabled) return Result.success()

            val activeBottles = bottleRepository.getAllActive()
            activeBottles.forEach { bottle ->
                scheduler.schedule(bottle, settings.reminderBeforeExpiryMinutes)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
