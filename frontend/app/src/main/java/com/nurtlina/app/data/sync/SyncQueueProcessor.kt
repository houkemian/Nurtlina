package com.nurtlina.app.data.sync

import androidx.room.withTransaction
import com.google.gson.Gson
import com.nurtlina.app.data.local.dao.BabyDao
import com.nurtlina.app.data.local.dao.DiaperLogDao
import com.nurtlina.app.data.local.dao.FeedLogDao
import com.nurtlina.app.data.local.dao.SleepLogDao
import com.nurtlina.app.data.local.dao.SyncQueueDao
import com.nurtlina.app.data.local.db.NurtlinaDatabase
import com.nurtlina.app.data.local.entity.SyncQueueEntity
import com.nurtlina.app.data.local.entity.SyncStatus
import com.nurtlina.app.data.remote.api.BackendApiService
import com.nurtlina.app.data.remote.api.BabyChangeDto
import com.nurtlina.app.data.remote.api.DiaperLogChangeDto
import com.nurtlina.app.data.remote.api.FeedLogChangeDto
import com.nurtlina.app.data.remote.api.SleepLogChangeDto
import com.nurtlina.app.data.remote.api.SyncPushRequest
import com.nurtlina.app.domain.model.SyncResult
import com.nurtlina.app.domain.repository.SessionRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncQueueProcessor @Inject constructor(
    private val database: NurtlinaDatabase,
    private val syncQueueDao: SyncQueueDao,
    private val babyDao: BabyDao,
    private val feedLogDao: FeedLogDao,
    private val diaperLogDao: DiaperLogDao,
    private val sleepLogDao: SleepLogDao,
    private val sessionRepository: SessionRepository,
    private val api: BackendApiService,
) {
    private val gson = Gson()

    suspend fun syncNow(): SyncResult {
        val session = sessionRepository.get()
        val familyId = session.defaultFamilyId ?: return SyncResult(syncedCount = 0, failedCount = 0)
        val readyItems = syncQueueDao.getReady(Instant.now().toEpochMilli(), MAX_BATCH_SIZE)

        var synced = 0
        var failed = 0
        readyItems.forEach { item ->
            val result = runCatching { pushItem(item, familyId, session.clientId) }
            if (result.isSuccess) {
                markSynced(item)
                synced += 1
            } else {
                markFailed(item, result.exceptionOrNull())
                failed += 1
            }
        }

        return SyncResult(syncedCount = synced, failedCount = failed)
    }

    private suspend fun pushItem(item: SyncQueueEntity, familyId: String, clientId: String) {
        when (item.operation) {
            SyncOperations.UPSERT_BABY,
            SyncOperations.DELETE_BABY -> api.pushBabies(
                SyncPushRequest(familyId, clientId, listOf(gson.fromJson(item.payloadJson, BabyChangeDto::class.java))),
            ).ensureAccepted(item)
            SyncOperations.UPSERT_FEED_LOG,
            SyncOperations.DELETE_FEED_LOG -> api.pushFeedLogs(
                SyncPushRequest(familyId, clientId, listOf(gson.fromJson(item.payloadJson, FeedLogChangeDto::class.java))),
            ).ensureAccepted(item)
            SyncOperations.UPSERT_DIAPER_LOG,
            SyncOperations.DELETE_DIAPER_LOG -> api.pushDiaperLogs(
                SyncPushRequest(familyId, clientId, listOf(gson.fromJson(item.payloadJson, DiaperLogChangeDto::class.java))),
            ).ensureAccepted(item)
            SyncOperations.UPSERT_SLEEP_LOG,
            SyncOperations.DELETE_SLEEP_LOG -> api.pushSleepLogs(
                SyncPushRequest(familyId, clientId, listOf(gson.fromJson(item.payloadJson, SleepLogChangeDto::class.java))),
            ).ensureAccepted(item)
            else -> Unit
        }
    }

    private fun com.nurtlina.app.data.remote.api.SyncPushResponse.ensureAccepted(item: SyncQueueEntity) {
        when {
            accepted.contains(item.entityId) -> Unit
            conflicts.contains(item.entityId) -> error("Sync conflict for ${item.entityType}:${item.entityId}")
            rejected.contains(item.entityId) -> error("Backend rejected ${item.entityType}:${item.entityId}")
            else -> error("Backend did not accept ${item.entityType}:${item.entityId}")
        }
    }

    private suspend fun markSynced(item: SyncQueueEntity) {
        val now = Instant.now().toEpochMilli()
        database.withTransaction {
            syncQueueDao.delete(item.id)
            updateBusinessSyncState(item, SyncStatus.SYNCED, now)
        }
    }

    private suspend fun markFailed(item: SyncQueueEntity, throwable: Throwable?) {
        val retryCount = item.retryCount + 1
        val now = Instant.now().toEpochMilli()
        val nextRetryAt = now + retryDelayMillis(retryCount)
        val error = throwable?.message?.take(MAX_ERROR_LENGTH) ?: "Sync failed"
        database.withTransaction {
            syncQueueDao.upsert(
                item.copy(
                    retryCount = retryCount,
                    nextRetryAt = nextRetryAt,
                    lastError = error,
                ),
            )
            updateBusinessSyncState(item, SyncStatus.FAILED, null)
        }
    }

    private suspend fun updateBusinessSyncState(
        item: SyncQueueEntity,
        status: SyncStatus,
        lastSyncedAt: Long?,
    ) {
        when (item.entityType) {
            SyncEntityTypes.BABY -> babyDao.updateSyncState(item.entityId, status.name, lastSyncedAt)
            SyncEntityTypes.FEED_LOG -> feedLogDao.updateSyncState(item.entityId, status.name, lastSyncedAt)
            SyncEntityTypes.DIAPER_LOG -> diaperLogDao.updateSyncState(item.entityId, status.name, lastSyncedAt)
            SyncEntityTypes.SLEEP_LOG -> sleepLogDao.updateSyncState(item.entityId, status.name, lastSyncedAt)
        }
    }

    private fun retryDelayMillis(retryCount: Int): Long =
        RETRY_DELAYS_MINUTES.getOrElse(retryCount - 1) { RETRY_DELAYS_MINUTES.last() } * 60_000L

    companion object {
        private const val MAX_BATCH_SIZE = 50
        private const val MAX_ERROR_LENGTH = 240
        private val RETRY_DELAYS_MINUTES = listOf(1L, 5L, 15L, 60L, 360L)
    }
}
