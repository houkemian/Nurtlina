package com.nurtlina.app.data.repository

import androidx.room.withTransaction
import com.nurtlina.app.data.local.dao.BottleDao
import com.nurtlina.app.data.local.db.NurtlinaDatabase
import com.nurtlina.app.data.local.entity.BottleEntity
import com.nurtlina.app.data.local.entity.SyncStatus
import com.nurtlina.app.data.sync.SyncOperations
import com.nurtlina.app.data.sync.SyncQueueWriter
import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.repository.BottleRepository
import com.nurtlina.app.domain.repository.SessionRepository
import com.nurtlina.app.domain.repository.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomBottleRepository @Inject constructor(
    private val dao: BottleDao,
    private val database: NurtlinaDatabase,
    private val syncQueueWriter: SyncQueueWriter,
    private val sessionRepository: SessionRepository,
    private val syncManager: SyncManager,
) : BottleRepository {

    override fun observeActive(babyId: String): Flow<List<Bottle>> =
        dao.observeActive(babyId).map { it.map { e -> e.toDomain() } }

    override fun observeAll(babyId: String): Flow<List<Bottle>> =
        dao.observeAll(babyId).map { it.map { e -> e.toDomain() } }

    override fun observeById(id: String): Flow<Bottle?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getById(id: String): Bottle? =
        dao.getById(id)?.toDomain()

    override suspend fun getAllActive(): List<Bottle> =
        dao.getAllActive().map { it.toDomain() }

    override suspend fun upsert(bottle: Bottle) {
        val entity = BottleEntity.fromDomain(bottle).withSyncMetadata()
        database.withTransaction {
            dao.upsert(entity)
            syncQueueWriter.enqueueBottle(entity)
        }
        syncManager.requestSyncSoon()
    }

    override suspend fun updateStatus(id: String, bottle: Bottle) {
        upsert(bottle)
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
            syncQueueWriter.enqueueBottle(entity, SyncOperations.DELETE_BOTTLE)
            dao.delete(id)
        }
        syncManager.requestSyncSoon()
    }

    private suspend fun BottleEntity.withSyncMetadata(): BottleEntity {
        val session = sessionRepository.get()
        return copy(
            familyId = familyId ?: session.defaultFamilyId,
            clientId = clientId ?: session.clientId,
            syncStatus = SyncStatus.PENDING.name,
            lastSyncedAt = null,
        )
    }
}
