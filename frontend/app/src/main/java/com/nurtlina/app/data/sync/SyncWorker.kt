package com.nurtlina.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodic background sync worker.
 *
 * - Runs every 30 minutes when the device has any network connection.
 * - Uses exponential backoff so transient network errors don't hammer the backend.
 * - A failed sync never crashes or corrupts local timer state; errors are logged
 *   to SyncRepository.observeSyncState() for optional UI display.
 *
 * Scheduling notes:
 * - The work is deduplicated by [WORK_NAME] so multiple enqueue calls are safe.
 * - Use [REPLACE] policy so settings changes (e.g. sync disabled) take effect.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncQueueProcessor: SyncQueueProcessor,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val syncResult = syncQueueProcessor.syncNow()
        return if (syncResult.isSuccess) {
            Result.success()
        } else {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "nurtlina_sync_worker"
        const val ONE_TIME_WORK_NAME = "nurtlina_sync_now_worker"
        private const val MAX_ATTEMPTS = 3

        /**
         * Enqueues the periodic sync work.
         * Call from [NurtlinaApp] after the user signs in or enables sync.
         */
        fun enqueue(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 30,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10,
                    TimeUnit.SECONDS,
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        /**
         * Cancels the periodic sync work, e.g. when the user disables sync or signs out.
         */
        fun cancel(workManager: WorkManager) {
            workManager.cancelUniqueWork(WORK_NAME)
        }
    }
}
