package com.nurtlina.app.data.remote.dto

import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.model.FeedType
import com.nurtlina.app.domain.model.NursingSide
import com.nurtlina.app.domain.model.SyncMetadata
import java.time.Instant

data class RemoteFeedLogDto(
    val id: String = "",
    val ownerUserId: String = "",
    val familyId: String = "",
    val babyId: String = "",
    val bottleId: String? = null,
    val feedType: String = "",
    val amountMl: Double? = null,
    val startedAt: Long = 0L,
    val endedAt: Long? = null,
    val side: String? = null,
    val note: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val clientId: String = "",
    val schemaVersion: Int = SyncMetadata.CURRENT_SCHEMA_VERSION,
) {
    fun toDomain(): FeedLog = FeedLog(
        id = id,
        babyId = babyId,
        bottleId = bottleId,
        feedType = FeedType.valueOf(feedType),
        amountMl = amountMl,
        startedAt = Instant.ofEpochMilli(startedAt),
        endedAt = endedAt?.let { Instant.ofEpochMilli(it) },
        side = side?.let { NursingSide.valueOf(it) },
        note = note,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "ownerUserId" to ownerUserId,
        "familyId" to familyId,
        "babyId" to babyId,
        "bottleId" to bottleId,
        "feedType" to feedType,
        "amountMl" to amountMl,
        "startedAt" to startedAt,
        "endedAt" to endedAt,
        "side" to side,
        "note" to note,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "deletedAt" to deletedAt,
        "clientId" to clientId,
        "schemaVersion" to schemaVersion,
    )

    companion object {
        fun fromDomain(log: FeedLog, meta: SyncMetadata): RemoteFeedLogDto = RemoteFeedLogDto(
            id = log.id,
            ownerUserId = meta.ownerUserId,
            familyId = meta.familyId,
            babyId = log.babyId,
            bottleId = log.bottleId,
            feedType = log.feedType.name,
            amountMl = log.amountMl,
            startedAt = log.startedAt.toEpochMilli(),
            endedAt = log.endedAt?.toEpochMilli(),
            side = log.side?.name,
            note = log.note,
            createdAt = log.createdAt.toEpochMilli(),
            updatedAt = log.updatedAt.toEpochMilli(),
            deletedAt = meta.deletedAt?.toEpochMilli(),
            clientId = meta.clientId,
            schemaVersion = meta.schemaVersion,
        )

        fun fromMap(map: Map<String, Any?>): RemoteFeedLogDto = RemoteFeedLogDto(
            id = map["id"] as? String ?: "",
            ownerUserId = map["ownerUserId"] as? String ?: "",
            familyId = map["familyId"] as? String ?: "",
            babyId = map["babyId"] as? String ?: "",
            bottleId = map["bottleId"] as? String,
            feedType = map["feedType"] as? String ?: "",
            amountMl = (map["amountMl"] as? Number)?.toDouble(),
            startedAt = (map["startedAt"] as? Number)?.toLong() ?: 0L,
            endedAt = (map["endedAt"] as? Number)?.toLong(),
            side = map["side"] as? String,
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
