package com.nurtlina.app.data.repository

import androidx.room.withTransaction
import com.nurtlina.app.data.local.dao.BabyDao
import com.nurtlina.app.data.local.db.NurtlinaDatabase
import com.nurtlina.app.data.local.entity.BabyEntity
import com.nurtlina.app.data.local.entity.SyncStatus
import com.nurtlina.app.data.sync.SyncOperations
import com.nurtlina.app.data.sync.SyncQueueWriter
import com.nurtlina.app.domain.model.Baby
import com.nurtlina.app.domain.repository.BabyRepository
import com.nurtlina.app.domain.repository.SessionRepository
import com.nurtlina.app.domain.repository.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomBabyRepository @Inject constructor(
    private val dao: BabyDao,
    private val database: NurtlinaDatabase,
    private val syncQueueWriter: SyncQueueWriter,
    private val sessionRepository: SessionRepository,
    private val syncManager: SyncManager,
) : BabyRepository {

    override fun observeAll(): Flow<List<Baby>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeById(id: String): Flow<Baby?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun upsert(baby: Baby) {
        val entity = BabyEntity.fromDomain(baby).withSyncMetadata()
        database.withTransaction {
            dao.upsert(entity)
            syncQueueWriter.enqueueBaby(entity)
        }
        syncManager.requestSyncSoon()
    }

    override suspend fun archive(id: String) {
        val existing = dao.getById(id) ?: return
        val now = Instant.now().toEpochMilli()
        val entity = existing.copy(
            archivedAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING.name,
            lastSyncedAt = null,
        ).withSyncMetadata()
        database.withTransaction {
            dao.upsert(entity)
            syncQueueWriter.enqueueBaby(entity)
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
            syncQueueWriter.enqueueBaby(entity, SyncOperations.DELETE_BABY)
            dao.delete(id)
        }
        syncManager.requestSyncSoon()
    }

    override suspend fun count(): Int =
        dao.count()

    private suspend fun BabyEntity.withSyncMetadata(): BabyEntity {
        val session = sessionRepository.get()
        return copy(
            familyId = familyId ?: session.defaultFamilyId,
            clientId = clientId ?: session.clientId,
            syncStatus = SyncStatus.PENDING.name,
            lastSyncedAt = null,
        )
    }
}
