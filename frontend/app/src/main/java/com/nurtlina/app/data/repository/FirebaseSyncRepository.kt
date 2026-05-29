package com.nurtlina.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.nurtlina.app.data.local.dao.BabyDao
import com.nurtlina.app.data.local.dao.BottleDao
import com.nurtlina.app.data.local.dao.DiaperLogDao
import com.nurtlina.app.data.local.dao.FeedLogDao
import com.nurtlina.app.data.local.dao.SleepLogDao
import com.nurtlina.app.data.local.entity.BabyEntity
import com.nurtlina.app.data.local.entity.BottleEntity
import com.nurtlina.app.data.local.entity.DiaperLogEntity
import com.nurtlina.app.data.local.entity.FeedLogEntity
import com.nurtlina.app.data.local.entity.SleepLogEntity
import com.nurtlina.app.data.remote.FirebaseAuthSource
import com.nurtlina.app.data.remote.FirestoreSource
import com.nurtlina.app.data.remote.dto.RemoteBabyDto
import com.nurtlina.app.data.remote.dto.RemoteBottleDto
import com.nurtlina.app.data.remote.dto.RemoteDiaperLogDto
import com.nurtlina.app.data.remote.dto.RemoteFeedLogDto
import com.nurtlina.app.data.remote.dto.RemoteSleepLogDto
import com.nurtlina.app.domain.model.SyncMetadata
import com.nurtlina.app.domain.model.SyncState
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
 * Eventual-consistency sync: push local changes up, pull remote changes down.
 *
 * Conflict resolution: last-updatedAt-wins. If a remote record has a higher
 * updatedAt than the local one, the local record is overwritten. The Firestore
 * security rule enforces the same policy server-side.
 *
 * Safety constraint: this class must never block or invalidate the bottle timer
 * flow. All sync operations wrap in runCatching and degrade gracefully.
 */
@Singleton
class FirebaseSyncRepository @Inject constructor(
    private val authSource: FirebaseAuthSource,
    private val firestoreSource: FirestoreSource,
    private val babyDao: BabyDao,
    private val bottleDao: BottleDao,
    private val feedLogDao: FeedLogDao,
    private val diaperLogDao: DiaperLogDao,
    private val sleepLogDao: SleepLogDao,
    private val dataStore: DataStore<Preferences>,
) : SyncRepository {

    private val _syncState = MutableStateFlow(SyncState(null, false, null))

    override fun observeSyncState(): Flow<SyncState> = _syncState.asStateFlow()

    override suspend fun syncAll(): Result<Unit> = runCatching {
        val userId = authSource.currentUserId() ?: return@runCatching
        val familyId = firestoreSource.fetchFamilyId(userId) ?: return@runCatching

        _syncState.update { it.copy(isSyncing = true, lastError = null) }

        val sinceMillis = readLastSyncedAt()
        val meta = buildMeta(userId, familyId)

        pushBabies(familyId, sinceMillis, meta)
        pushBottles(familyId, sinceMillis, meta)
        pushFeedLogs(familyId, sinceMillis, meta)
        pushDiaperLogs(familyId, sinceMillis, meta)
        pushSleepLogs(familyId, sinceMillis, meta)

        pullBabies(familyId, sinceMillis)
        pullBottles(familyId, sinceMillis)
        pullFeedLogs(familyId, sinceMillis)
        pullDiaperLogs(familyId, sinceMillis)
        pullSleepLogs(familyId, sinceMillis)

        val nowMillis = Instant.now().toEpochMilli()
        writeLastSyncedAt(nowMillis)
        _syncState.update {
            it.copy(isSyncing = false, lastSyncedAt = Instant.ofEpochMilli(nowMillis))
        }
    }.onFailure { e ->
        _syncState.update { it.copy(isSyncing = false, lastError = e.message) }
    }

    override suspend fun syncRecord(collectionName: String, id: String): Result<Unit> =
        runCatching {
            val userId = authSource.currentUserId() ?: return@runCatching
            val familyId = firestoreSource.fetchFamilyId(userId) ?: return@runCatching
            val meta = buildMeta(userId, familyId)

            when (collectionName) {
                FirestoreSource.BOTTLES -> {
                    bottleDao.getById(id)?.toDomain()?.let { bottle ->
                        firestoreSource.upsertBottle(
                            familyId,
                            RemoteBottleDto.fromDomain(bottle, meta),
                        )
                    }
                }
                // Other collections follow the same pattern; add as needed.
                else -> Unit
            }
        }

    override suspend fun requestFullSync() {
        writeLastSyncedAt(0L)
    }

    override suspend fun resetSyncState() {
        dataStore.edit { prefs ->
            prefs.remove(LAST_SYNCED_AT_KEY)
        }
        _syncState.value = SyncState(null, false, null)
    }

    // ── Push helpers ──────────────────────────────────────────────────────────

    private suspend fun pushBabies(familyId: String, sinceMillis: Long, meta: SyncMetadata) {
        babyDao.getBabiesUpdatedSince(sinceMillis).forEach { entity ->
            firestoreSource.upsertBaby(familyId, RemoteBabyDto.fromDomain(entity.toDomain(), meta))
        }
    }

    private suspend fun pushBottles(familyId: String, sinceMillis: Long, meta: SyncMetadata) {
        bottleDao.getBottlesUpdatedSince(sinceMillis).forEach { entity ->
            firestoreSource.upsertBottle(familyId, RemoteBottleDto.fromDomain(entity.toDomain(), meta))
        }
    }

    private suspend fun pushFeedLogs(familyId: String, sinceMillis: Long, meta: SyncMetadata) {
        feedLogDao.getFeedLogsUpdatedSince(sinceMillis).forEach { entity ->
            firestoreSource.upsertFeedLog(familyId, RemoteFeedLogDto.fromDomain(entity.toDomain(), meta))
        }
    }

    private suspend fun pushDiaperLogs(familyId: String, sinceMillis: Long, meta: SyncMetadata) {
        diaperLogDao.getDiaperLogsUpdatedSince(sinceMillis).forEach { entity ->
            firestoreSource.upsertDiaperLog(familyId, RemoteDiaperLogDto.fromDomain(entity.toDomain(), meta))
        }
    }

    private suspend fun pushSleepLogs(familyId: String, sinceMillis: Long, meta: SyncMetadata) {
        sleepLogDao.getSleepLogsUpdatedSince(sinceMillis).forEach { entity ->
            firestoreSource.upsertSleepLog(familyId, RemoteSleepLogDto.fromDomain(entity.toDomain(), meta))
        }
    }

    // ── Pull helpers ──────────────────────────────────────────────────────────

    private suspend fun pullBabies(familyId: String, sinceMillis: Long) {
        firestoreSource.fetchBabiesSince(familyId, sinceMillis).forEach { dto ->
            val localEntity = babyDao.getById(dto.id)
            val remoteUpdatedAt = dto.updatedAt
            if (localEntity == null || localEntity.updatedAt < remoteUpdatedAt) {
                if (dto.deletedAt != null) {
                    babyDao.delete(dto.id)
                } else {
                    babyDao.upsert(BabyEntity.fromDomain(dto.toDomain()))
                }
            }
        }
    }

    private suspend fun pullBottles(familyId: String, sinceMillis: Long) {
        firestoreSource.fetchBottlesSince(familyId, sinceMillis).forEach { dto ->
            val localEntity = bottleDao.getById(dto.id)
            val remoteUpdatedAt = dto.updatedAt
            if (localEntity == null || localEntity.updatedAt < remoteUpdatedAt) {
                if (dto.deletedAt != null) {
                    bottleDao.delete(dto.id)
                } else {
                    bottleDao.upsert(BottleEntity.fromDomain(dto.toDomain()))
                }
            }
        }
    }

    private suspend fun pullFeedLogs(familyId: String, sinceMillis: Long) {
        firestoreSource.fetchFeedLogsSince(familyId, sinceMillis).forEach { dto ->
            val localEntity = feedLogDao.getById(dto.id)
            val remoteUpdatedAt = dto.updatedAt
            if (localEntity == null || localEntity.updatedAt < remoteUpdatedAt) {
                if (dto.deletedAt != null) {
                    feedLogDao.delete(dto.id)
                } else {
                    feedLogDao.upsert(FeedLogEntity.fromDomain(dto.toDomain()))
                }
            }
        }
    }

    private suspend fun pullDiaperLogs(familyId: String, sinceMillis: Long) {
        firestoreSource.fetchDiaperLogsSince(familyId, sinceMillis).forEach { dto ->
            val localEntity = diaperLogDao.getById(dto.id)
            val remoteUpdatedAt = dto.updatedAt
            if (localEntity == null || localEntity.updatedAt < remoteUpdatedAt) {
                if (dto.deletedAt != null) {
                    diaperLogDao.delete(dto.id)
                } else {
                    diaperLogDao.upsert(DiaperLogEntity.fromDomain(dto.toDomain()))
                }
            }
        }
    }

    private suspend fun pullSleepLogs(familyId: String, sinceMillis: Long) {
        firestoreSource.fetchSleepLogsSince(familyId, sinceMillis).forEach { dto ->
            val localEntity = sleepLogDao.getById(dto.id)
            val remoteUpdatedAt = dto.updatedAt
            if (localEntity == null || localEntity.updatedAt < remoteUpdatedAt) {
                if (dto.deletedAt != null) {
                    sleepLogDao.delete(dto.id)
                } else {
                    sleepLogDao.upsert(SleepLogEntity.fromDomain(dto.toDomain()))
                }
            }
        }
    }

    // ── DataStore helpers ─────────────────────────────────────────────────────

    private suspend fun readLastSyncedAt(): Long =
        dataStore.data.first()[LAST_SYNCED_AT_KEY] ?: 0L

    private suspend fun writeLastSyncedAt(millis: Long) {
        dataStore.edit { prefs -> prefs[LAST_SYNCED_AT_KEY] = millis }
    }

    private fun buildMeta(userId: String, familyId: String): SyncMetadata = SyncMetadata(
        id = "",
        ownerUserId = userId,
        familyId = familyId,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        deletedAt = null,
        clientId = userId,
        schemaVersion = SyncMetadata.CURRENT_SCHEMA_VERSION,
    )

    companion object {
        private val LAST_SYNCED_AT_KEY = longPreferencesKey("sync_last_synced_at")
    }
}
