package com.nurtlina.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.room.withTransaction
import com.nurtlina.app.data.local.dao.BabyDao
import com.nurtlina.app.data.local.dao.DiaperLogDao
import com.nurtlina.app.data.local.dao.FeedLogDao
import com.nurtlina.app.data.local.dao.SleepLogDao
import com.nurtlina.app.data.local.db.NurtlinaDatabase
import com.nurtlina.app.data.local.entity.BabyEntity
import com.nurtlina.app.data.local.entity.DiaperLogEntity
import com.nurtlina.app.data.local.entity.FeedLogEntity
import com.nurtlina.app.data.local.entity.SleepLogEntity
import com.nurtlina.app.data.local.entity.SyncStatus
import com.nurtlina.app.data.remote.api.BabyChangeDto
import com.nurtlina.app.data.remote.api.BackendApiService
import com.nurtlina.app.data.remote.api.DiaperLogChangeDto
import com.nurtlina.app.data.remote.api.FeedLogChangeDto
import com.nurtlina.app.data.remote.api.SleepLogChangeDto
import com.nurtlina.app.data.remote.api.SyncPullResponse
import com.nurtlina.app.data.sync.SyncQueueProcessor
import com.nurtlina.app.domain.model.SyncState
import com.nurtlina.app.domain.repository.SessionRepository
import com.nurtlina.app.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FastAPI/Postgres-backed sync repository.
 *
 * Local writes continue to enqueue offline-safe changes through SyncQueueWriter.
 * This repository is the single UI/worker sync entrypoint: it flushes that queue,
 * pulls backend changes, and publishes sync state for Settings.
 */
@Singleton
class ApiSyncRepository @Inject constructor(
    private val database: NurtlinaDatabase,
    private val api: BackendApiService,
    private val syncQueueProcessor: SyncQueueProcessor,
    private val sessionRepository: SessionRepository,
    private val babyDao: BabyDao,
    private val feedLogDao: FeedLogDao,
    private val diaperLogDao: DiaperLogDao,
    private val sleepLogDao: SleepLogDao,
    private val dataStore: DataStore<Preferences>,
) : SyncRepository {

    private val _syncState = MutableStateFlow(SyncState(null, false, null))

    override fun observeSyncState(): Flow<SyncState> = _syncState.asStateFlow()

    override suspend fun syncAll(): Result<Unit> = runCatching {
        val session = sessionRepository.get()
        val familyId = session.defaultFamilyId ?: return@runCatching

        _syncState.update { it.copy(isSyncing = true, lastError = null) }

        val pushResult = syncQueueProcessor.syncNow()
        if (!pushResult.isSuccess) {
            error("Sync failed for ${pushResult.failedCount} queued item(s).")
        }

        val lastPulledAt = pullChanges(familyId, session.clientId, readLastPulledAt())
        writeLastPulledAt(lastPulledAt.toEpochMilli())

        _syncState.update {
            it.copy(
                isSyncing = false,
                lastSyncedAt = Instant.now(),
                lastError = null,
            )
        }
    }.onFailure { throwable ->
        _syncState.update { it.copy(isSyncing = false, lastError = throwable.message) }
    }

    override suspend fun syncRecord(collectionName: String, id: String): Result<Unit> = syncAll()

    override suspend fun requestFullSync() {
        writeLastPulledAt(0L)
    }

    override suspend fun resetSyncState() {
        dataStore.edit { prefs ->
            prefs.remove(LAST_PULLED_AT_KEY)
        }
        _syncState.value = SyncState(null, false, null)
    }

    private suspend fun pullChanges(
        familyId: String,
        clientId: String,
        initialCursor: Long,
    ): Instant {
        var cursor = Instant.ofEpochMilli(initialCursor)
        var latestServerTime = cursor

        do {
            val response = api.pullChanges(
                familyId = familyId,
                clientId = clientId,
                since = cursor.toString(),
            )
            applyPullResponse(response)
            latestServerTime = response.serverTime.toInstant()
            cursor = response.nextCursor?.toInstant() ?: latestServerTime
        } while (response.hasMore && response.nextCursor != null)

        return latestServerTime
    }

    private suspend fun applyPullResponse(response: SyncPullResponse) {
        val syncedAt = response.serverTime.toInstantMillis()
        database.withTransaction {
            response.babies.forEach { applyBaby(it, syncedAt) }
            response.feedLogs.forEach { applyFeedLog(it, syncedAt) }
            response.diaperLogs.forEach { applyDiaperLog(it, syncedAt) }
            response.sleepLogs.forEach { applySleepLog(it, syncedAt) }
        }
    }

    private suspend fun applyBaby(dto: BabyChangeDto, syncedAt: Long) {
        val remoteUpdatedAt = dto.updatedAt.toInstantMillis()
        val local = babyDao.getById(dto.id)
        if (local != null && local.updatedAt >= remoteUpdatedAt) return

        if (dto.deletedAt != null) {
            babyDao.delete(dto.id)
            return
        }

        babyDao.upsert(
            BabyEntity(
                id = dto.id,
                name = dto.name,
                birthDate = dto.birthDate,
                avatarColor = dto.avatarColor,
                createdAt = dto.createdAt.toInstantMillis(),
                updatedAt = remoteUpdatedAt,
                archivedAt = null,
                familyId = dto.familyId,
                deletedAt = null,
                syncStatus = SyncStatus.SYNCED.name,
                syncVersion = dto.schemaVersion,
                clientId = dto.clientId,
                lastSyncedAt = syncedAt,
            ),
        )
    }

    private suspend fun applyFeedLog(dto: FeedLogChangeDto, syncedAt: Long) {
        val remoteUpdatedAt = dto.updatedAt.toInstantMillis()
        val local = feedLogDao.getById(dto.id)
        if (local != null && local.updatedAt >= remoteUpdatedAt) return

        if (dto.deletedAt != null) {
            feedLogDao.delete(dto.id)
            return
        }

        feedLogDao.upsert(
            FeedLogEntity(
                id = dto.id,
                babyId = dto.babyId,
                bottleId = dto.bottleId,
                feedType = dto.feedType,
                amountMl = dto.amountMl,
                startedAt = dto.startedAt.toInstantMillis(),
                endedAt = dto.endedAt.toInstantMillisOrNull(),
                side = null,
                note = dto.note,
                createdAt = dto.createdAt.toInstantMillis(),
                updatedAt = remoteUpdatedAt,
                familyId = dto.familyId,
                deletedAt = null,
                syncStatus = SyncStatus.SYNCED.name,
                syncVersion = dto.schemaVersion,
                clientId = dto.clientId,
                lastSyncedAt = syncedAt,
            ),
        )
    }

    private suspend fun applyDiaperLog(dto: DiaperLogChangeDto, syncedAt: Long) {
        val remoteUpdatedAt = dto.updatedAt.toInstantMillis()
        val local = diaperLogDao.getById(dto.id)
        if (local != null && local.updatedAt >= remoteUpdatedAt) return

        if (dto.deletedAt != null) {
            diaperLogDao.delete(dto.id)
            return
        }

        diaperLogDao.upsert(
            DiaperLogEntity(
                id = dto.id,
                babyId = dto.babyId,
                diaperType = dto.diaperType,
                changedAt = dto.changedAt.toInstantMillis(),
                note = dto.note,
                createdAt = dto.createdAt.toInstantMillis(),
                updatedAt = remoteUpdatedAt,
                familyId = dto.familyId,
                deletedAt = null,
                syncStatus = SyncStatus.SYNCED.name,
                syncVersion = dto.schemaVersion,
                clientId = dto.clientId,
                lastSyncedAt = syncedAt,
            ),
        )
    }

    private suspend fun applySleepLog(dto: SleepLogChangeDto, syncedAt: Long) {
        val remoteUpdatedAt = dto.updatedAt.toInstantMillis()
        val local = sleepLogDao.getById(dto.id)
        if (local != null && local.updatedAt >= remoteUpdatedAt) return

        if (dto.deletedAt != null) {
            sleepLogDao.delete(dto.id)
            return
        }

        sleepLogDao.upsert(
            SleepLogEntity(
                id = dto.id,
                babyId = dto.babyId,
                startedAt = dto.startedAt.toInstantMillis(),
                endedAt = dto.endedAt.toInstantMillisOrNull(),
                note = dto.note,
                createdAt = dto.createdAt.toInstantMillis(),
                updatedAt = remoteUpdatedAt,
                familyId = dto.familyId,
                deletedAt = null,
                syncStatus = SyncStatus.SYNCED.name,
                syncVersion = dto.schemaVersion,
                clientId = dto.clientId,
                lastSyncedAt = syncedAt,
            ),
        )
    }

    private suspend fun readLastPulledAt(): Long =
        dataStore.data.first()[LAST_PULLED_AT_KEY] ?: 0L

    private suspend fun writeLastPulledAt(millis: Long) {
        dataStore.edit { prefs -> prefs[LAST_PULLED_AT_KEY] = millis }
    }

    private fun String.toInstant(): Instant = Instant.parse(this)

    private fun String.toInstantMillis(): Long = toInstant().toEpochMilli()

    private fun String?.toInstantMillisOrNull(): Long? = this?.toInstantMillis()

    companion object {
        private val LAST_PULLED_AT_KEY = longPreferencesKey("api_sync_last_pulled_at")
    }
}
