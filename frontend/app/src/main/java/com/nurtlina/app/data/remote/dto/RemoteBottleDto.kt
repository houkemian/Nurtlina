package com.nurtlina.app.data.remote.dto

import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.BottleStatus
import com.nurtlina.app.domain.model.GuidelineRegion
import com.nurtlina.app.domain.model.MilkType
import com.nurtlina.app.domain.model.SyncMetadata
import java.time.Instant

data class RemoteBottleDto(
    val id: String = "",
    val ownerUserId: String = "",
    val familyId: String = "",
    val babyId: String = "",
    val milkType: String = "",
    val amountMl: Double? = null,
    val preparedAt: Long = 0L,
    val feedingStartedAt: Long? = null,
    val refrigeratedAt: Long? = null,
    val status: String = "",
    val guidelineRegion: String = "",
    val expiresAt: Long? = null,
    val discardedAt: Long? = null,
    val fedAt: Long? = null,
    val note: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val clientId: String = "",
    val schemaVersion: Int = SyncMetadata.CURRENT_SCHEMA_VERSION,
) {
    fun toDomain(): Bottle = Bottle(
        id = id,
        babyId = babyId,
        milkType = MilkType.valueOf(milkType),
        amountMl = amountMl,
        preparedAt = Instant.ofEpochMilli(preparedAt),
        feedingStartedAt = feedingStartedAt?.let { Instant.ofEpochMilli(it) },
        refrigeratedAt = refrigeratedAt?.let { Instant.ofEpochMilli(it) },
        status = BottleStatus.valueOf(status),
        guidelineRegion = GuidelineRegion.valueOf(guidelineRegion),
        expiresAt = expiresAt?.let { Instant.ofEpochMilli(it) },
        discardedAt = discardedAt?.let { Instant.ofEpochMilli(it) },
        fedAt = fedAt?.let { Instant.ofEpochMilli(it) },
        note = note,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "ownerUserId" to ownerUserId,
        "familyId" to familyId,
        "babyId" to babyId,
        "milkType" to milkType,
        "amountMl" to amountMl,
        "preparedAt" to preparedAt,
        "feedingStartedAt" to feedingStartedAt,
        "refrigeratedAt" to refrigeratedAt,
        "status" to status,
        "guidelineRegion" to guidelineRegion,
        "expiresAt" to expiresAt,
        "discardedAt" to discardedAt,
        "fedAt" to fedAt,
        "note" to note,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "deletedAt" to deletedAt,
        "clientId" to clientId,
        "schemaVersion" to schemaVersion,
    )

    companion object {
        fun fromDomain(bottle: Bottle, meta: SyncMetadata): RemoteBottleDto = RemoteBottleDto(
            id = bottle.id,
            ownerUserId = meta.ownerUserId,
            familyId = meta.familyId,
            babyId = bottle.babyId,
            milkType = bottle.milkType.name,
            amountMl = bottle.amountMl,
            preparedAt = bottle.preparedAt.toEpochMilli(),
            feedingStartedAt = bottle.feedingStartedAt?.toEpochMilli(),
            refrigeratedAt = bottle.refrigeratedAt?.toEpochMilli(),
            status = bottle.status.name,
            guidelineRegion = bottle.guidelineRegion.name,
            expiresAt = bottle.expiresAt?.toEpochMilli(),
            discardedAt = bottle.discardedAt?.toEpochMilli(),
            fedAt = bottle.fedAt?.toEpochMilli(),
            note = bottle.note,
            createdAt = bottle.createdAt.toEpochMilli(),
            updatedAt = bottle.updatedAt.toEpochMilli(),
            deletedAt = meta.deletedAt?.toEpochMilli(),
            clientId = meta.clientId,
            schemaVersion = meta.schemaVersion,
        )

        fun fromMap(map: Map<String, Any?>): RemoteBottleDto = RemoteBottleDto(
            id = map["id"] as? String ?: "",
            ownerUserId = map["ownerUserId"] as? String ?: "",
            familyId = map["familyId"] as? String ?: "",
            babyId = map["babyId"] as? String ?: "",
            milkType = map["milkType"] as? String ?: "",
            amountMl = (map["amountMl"] as? Number)?.toDouble(),
            preparedAt = (map["preparedAt"] as? Number)?.toLong() ?: 0L,
            feedingStartedAt = (map["feedingStartedAt"] as? Number)?.toLong(),
            refrigeratedAt = (map["refrigeratedAt"] as? Number)?.toLong(),
            status = map["status"] as? String ?: "",
            guidelineRegion = map["guidelineRegion"] as? String ?: "",
            expiresAt = (map["expiresAt"] as? Number)?.toLong(),
            discardedAt = (map["discardedAt"] as? Number)?.toLong(),
            fedAt = (map["fedAt"] as? Number)?.toLong(),
            note = map["note"] as? String,
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
            updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L,
            deletedAt = (map["deletedAt"] as? Number)?.toLong(),
            clientId = map["clientId"] as? String ?: "",
            schemaVersion = (map["schemaVersion"] as? Number)?.toInt()
                ?: SyncMetadata.CURRENT_SCHEMA_VERSION,
        )
    }
}
