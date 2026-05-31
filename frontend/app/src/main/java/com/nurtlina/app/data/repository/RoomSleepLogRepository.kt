package com.nurtlina.app.data.repository

import androidx.room.withTransaction
import com.nurtlina.app.data.local.dao.SleepLogDao
import com.nurtlina.app.data.local.db.NurtlinaDatabase
import com.nurtlina.app.data.local.entity.SleepLogEntity
import com.nurtlina.app.data.local.entity.SyncStatus
import com.nurtlina.app.data.sync.SyncOperations
import com.nurtlina.app.data.sync.SyncQueueWriter
import com.nurtlina.app.domain.model.SleepLog
import com.nurtlina.app.domain.repository.SleepLogRepository
import com.nurtlina.app.domain.repository.SessionRepository
import com.nurtlina.app.domain.repository.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomSleepLogRepository @Inject constructor(
    private val dao: SleepLogDao,
    private val database: NurtlinaDatabase,
    private val syncQueueWriter: SyncQueueWriter,
    private val sessionRepository: SessionRepository,
    private val syncManager: SyncManager,
) : SleepLogRepository {

    override fun observeByBaby(babyId: String): Flow<List<SleepLog>> =
        dao.observeByBaby(babyId).map { it.map { e -> e.toDomain() } }

    override fun observeByBabyAndRange(babyId: String, from: Instant, to: Instant): Flow<List<SleepLog>> =
        dao.observeByBabyAndRange(babyId, from.toEpochMilli(), to.toEpochMilli())
            .map { it.map { e -> e.toDomain() } }

    override fun observeActiveSleep(babyId: String): Flow<SleepLog?> =
        dao.observeActiveSleep(babyId).map { it?.toDomain() }

    override suspend fun getByBabyAndRange(babyId: String, from: Instant, to: Instant): List<SleepLog> =
        dao.getByBabyAndRange(babyId, from.toEpochMilli(), to.toEpochMilli()).map { it.toDomain() }

    override suspend fun getActiveSleep(babyId: String): SleepLog? =
        dao.getActiveSleep(babyId)?.toDomain()

    override suspend fun upsert(log: SleepLog) {
        val entity = SleepLogEntity.fromDomain(log).withSyncMetadata()
        database.withTransaction {
            dao.upsert(entity)
            syncQueueWriter.enqueueSleepLog(entity)
        }
        syncManager.requestSyncSoon()
    }

    override suspend fun delete(id: String) {
        val existing = dao.getById(id) ?: return
        val now = Instant.now().toEpochMilli()
        val entity = existing.copy(
            deletedAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING.name,
            lastSyncedAt = null,
        ).withSyncMetadata()
        database.withTransaction {
            syncQueueWriter.enqueueSleepLog(entity, SyncOperations.DELETE_SLEEP_LOG)
            dao.delete(id)
        }
        syncManager.requestSyncSoon()
    }

    private suspend fun SleepLogEntity.withSyncMetadata(): SleepLogEntity {
        val session = sessionRepository.get()
        return copy(
            familyId = familyId ?: session.defaultFamilyId,
            clientId = clientId ?: session.clientId,
            syncStatus = SyncStatus.PENDING.name,
            lastSyncedAt = null,
        )
    }
}
