package com.nurtlina.app.data.sync

import com.google.gson.Gson
import com.nurtlina.app.data.local.dao.SyncQueueDao
import com.nurtlina.app.data.local.entity.BabyEntity
import com.nurtlina.app.data.local.entity.BottleEntity
import com.nurtlina.app.data.local.entity.DiaperLogEntity
import com.nurtlina.app.data.local.entity.FeedLogEntity
import com.nurtlina.app.data.local.entity.SleepLogEntity
import com.nurtlina.app.data.local.entity.SyncQueueEntity
import com.nurtlina.app.data.remote.api.BabyChangeDto
import com.nurtlina.app.data.remote.api.BottleChangeDto
import com.nurtlina.app.data.remote.api.DiaperLogChangeDto
import com.nurtlina.app.data.remote.api.FeedLogChangeDto
import com.nurtlina.app.data.remote.api.SleepLogChangeDto
import com.nurtlina.app.domain.repository.SessionRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncQueueWriter @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val sessionRepository: SessionRepository,
) {
    private val gson = Gson()

    suspend fun enqueueBaby(entity: BabyEntity, operation: String = SyncOperations.UPSERT_BABY) {
        val session = sessionRepository.get()
        enqueue(
            entityType = SyncEntityTypes.BABY,
            entityId = entity.id,
            operation = operation,
            payload = BabyChangeDto(
                id = entity.id,
                familyId = entity.familyId ?: session.defaultFamilyId.orEmpty(),
                name = entity.name,
                birthDate = entity.birthDate,
                avatarColor = entity.avatarColor,
                clientId = entity.clientId ?: session.clientId,
                schemaVersion = entity.syncVersion,
                createdAt = entity.createdAt.iso(),
                updatedAt = entity.updatedAt.iso(),
                deletedAt = entity.deletedAt.isoOrNull(),
            ),
        )
    }

    suspend fun enqueueBottle(entity: BottleEntity, operation: String = SyncOperations.UPSERT_BOTTLE) {
        val session = sessionRepository.get()
        enqueue(
            entityType = SyncEntityTypes.BOTTLE,
            entityId = entity.id,
            operation = operation,
            payload = BottleChangeDto(
                id = entity.id,
                familyId = entity.familyId ?: session.defaultFamilyId.orEmpty(),
                babyId = entity.babyId,
                milkType = entity.milkType,
                amountMl = entity.amountMl,
                preparedAt = entity.preparedAt.iso(),
                feedingStartedAt = entity.feedingStartedAt.isoOrNull(),
                refrigeratedAt = entity.refrigeratedAt.isoOrNull(),
                status = entity.status,
                guidelineRegion = entity.guidelineRegion,
                ruleVersion = DEFAULT_RULE_VERSION,
                expiresAt = entity.expiresAt.isoOrNull(),
                discardedAt = entity.discardedAt.isoOrNull(),
                fedAt = entity.fedAt.isoOrNull(),
                note = entity.note,
                clientId = entity.clientId ?: session.clientId,
                schemaVersion = entity.syncVersion,
                createdAt = entity.createdAt.iso(),
                updatedAt = entity.updatedAt.iso(),
                deletedAt = entity.deletedAt.isoOrNull(),
            ),
        )
    }

    suspend fun enqueueFeedLog(entity: FeedLogEntity, operation: String = SyncOperations.UPSERT_FEED_LOG) {
        val session = sessionRepository.get()
        enqueue(
            entityType = SyncEntityTypes.FEED_LOG,
            entityId = entity.id,
            operation = operation,
            payload = FeedLogChangeDto(
                id = entity.id,
                familyId = entity.familyId ?: session.defaultFamilyId.orEmpty(),
                babyId = entity.babyId,
                bottleId = entity.bottleId,
                feedType = entity.feedType,
                amountMl = entity.amountMl,
                startedAt = entity.startedAt.iso(),
                endedAt = entity.endedAt.isoOrNull(),
                note = entity.note,
                clientId = entity.clientId ?: session.clientId,
                schemaVersion = entity.syncVersion,
                createdAt = entity.createdAt.iso(),
                updatedAt = entity.updatedAt.iso(),
                deletedAt = entity.deletedAt.isoOrNull(),
            ),
        )
    }

    suspend fun enqueueDiaperLog(entity: DiaperLogEntity, operation: String = SyncOperations.UPSERT_DIAPER_LOG) {
        val session = sessionRepository.get()
        enqueue(
            entityType = SyncEntityTypes.DIAPER_LOG,
            entityId = entity.id,
            operation = operation,
            payload = DiaperLogChangeDto(
                id = entity.id,
                familyId = entity.familyId ?: session.defaultFamilyId.orEmpty(),
                babyId = entity.babyId,
                diaperType = entity.diaperType,
                changedAt = entity.changedAt.iso(),
                note = entity.note,
                clientId = entity.clientId ?: session.clientId,
                schemaVersion = entity.syncVersion,
                createdAt = entity.createdAt.iso(),
                updatedAt = entity.updatedAt.iso(),
                deletedAt = entity.deletedAt.isoOrNull(),
            ),
        )
    }

    suspend fun enqueueSleepLog(entity: SleepLogEntity, operation: String = SyncOperations.UPSERT_SLEEP_LOG) {
        val session = sessionRepository.get()
        enqueue(
            entityType = SyncEntityTypes.SLEEP_LOG,
            entityId = entity.id,
            operation = operation,
            payload = SleepLogChangeDto(
                id = entity.id,
                familyId = entity.familyId ?: session.defaultFamilyId.orEmpty(),
                babyId = entity.babyId,
                startedAt = entity.startedAt.iso(),
                endedAt = entity.endedAt.isoOrNull(),
                note = entity.note,
                clientId = entity.clientId ?: session.clientId,
                schemaVersion = entity.syncVersion,
                createdAt = entity.createdAt.iso(),
                updatedAt = entity.updatedAt.iso(),
                deletedAt = entity.deletedAt.isoOrNull(),
            ),
        )
    }

    private suspend fun enqueue(
        entityType: String,
        entityId: String,
        operation: String,
        payload: Any,
    ) {
        val now = Instant.now().toEpochMilli()
        syncQueueDao.upsert(
            SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payloadJson = gson.toJson(payload),
                createdAt = now,
                retryCount = 0,
                nextRetryAt = now,
                lastError = null,
            ),
        )
    }

    private fun Long.iso(): String = Instant.ofEpochMilli(this).toString()

    private fun Long?.isoOrNull(): String? = this?.let { Instant.ofEpochMilli(it).toString() }

    companion object {
        private const val DEFAULT_RULE_VERSION = "default_v1"
    }
}
