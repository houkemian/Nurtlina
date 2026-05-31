package com.nurtlina.app.data.sync

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nurtlina.app.domain.model.SyncResult
import com.nurtlina.app.domain.repository.SyncManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerSyncManager @Inject constructor(
    private val workManager: WorkManager,
    private val syncQueueProcessor: SyncQueueProcessor,
) : SyncManager {

    override fun requestSyncSoon() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        workManager.enqueueUniqueWork(
            SyncWorker.ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    override suspend fun syncNow(): SyncResult = syncQueueProcessor.syncNow()
}
