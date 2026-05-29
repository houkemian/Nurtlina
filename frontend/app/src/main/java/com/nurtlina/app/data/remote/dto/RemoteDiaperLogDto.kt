package com.nurtlina.app.data.remote.dto

import com.nurtlina.app.domain.model.DiaperLog
import com.nurtlina.app.domain.model.DiaperType
import com.nurtlina.app.domain.model.SyncMetadata
import java.time.Instant

data class RemoteDiaperLogDto(
    val id: String = "",
    val ownerUserId: String = "",
    val familyId: String = "",
    val babyId: String = "",
    val diaperType: String = "",
    val changedAt: Long = 0L,
    val note: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val clientId: String = "",
    val schemaVersion: Int = SyncMetadata.CURRENT_SCHEMA_VERSION,
) {
    fun toDomain(): DiaperLog = DiaperLog(
        id = id,
        babyId = babyId,
        diaperType = DiaperType.valueOf(diaperType),
        changedAt = Instant.ofEpochMilli(changedAt),
        note = note,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "ownerUserId" to ownerUserId,
        "familyId" to familyId,
        "babyId" to babyId,
        "diaperType" to diaperType,
        "changedAt" to changedAt,
        "note" to note,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "deletedAt" to deletedAt,
        "clientId" to clientId,
        "schemaVersion" to schemaVersion,
    )

    companion object {
        fun fromDomain(log: DiaperLog, meta: SyncMetadata): RemoteDiaperLogDto = RemoteDiaperLogDto(
            id = log.id,
            ownerUserId = meta.ownerUserId,
            familyId = meta.familyId,
            babyId = log.babyId,
            diaperType = log.diaperType.name,
            changedAt = log.changedAt.toEpochMilli(),
            note = log.note,
            createdAt = log.createdAt.toEpochMilli(),
            updatedAt = log.updatedAt.toEpochMilli(),
            deletedAt = meta.deletedAt?.toEpochMilli(),
            clientId = meta.clientId,
            schemaVersion = meta.schemaVersion,
        )

        fun fromMap(map: Map<String, Any?>): RemoteDiaperLogDto = RemoteDiaperLogDto(
            id = map["id"] as? String ?: "",
            ownerUserId = map["ownerUserId"] as? String ?: "",
            familyId = map["familyId"] as? String ?: "",
            babyId = map["babyId"] as? String ?: "",
            diaperType = map["diaperType"] as? String ?: "",
            changedAt = (map["changedAt"] as? Number)?.toLong() ?: 0L,
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
