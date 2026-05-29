package com.nurtlina.app.data.remote.dto

import com.nurtlina.app.domain.model.SleepLog
import com.nurtlina.app.domain.model.SyncMetadata
import java.time.Instant

data class RemoteSleepLogDto(
    val id: String = "",
    val ownerUserId: String = "",
    val familyId: String = "",
    val babyId: String = "",
    val startedAt: Long = 0L,
    val endedAt: Long? = null,
    val note: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val clientId: String = "",
    val schemaVersion: Int = SyncMetadata.CURRENT_SCHEMA_VERSION,
) {
    fun toDomain(): SleepLog = SleepLog(
        id = id,
        babyId = babyId,
        startedAt = Instant.ofEpochMilli(startedAt),
        endedAt = endedAt?.let { Instant.ofEpochMilli(it) },
        note = note,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "ownerUserId" to ownerUserId,
        "familyId" to familyId,
        "babyId" to babyId,
        "startedAt" to startedAt,
        "endedAt" to endedAt,
        "note" to note,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "deletedAt" to deletedAt,
        "clientId" to clientId,
        "schemaVersion" to schemaVersion,
    )

    companion object {
        fun fromDomain(log: SleepLog, meta: SyncMetadata): RemoteSleepLogDto = RemoteSleepLogDto(
            id = log.id,
            ownerUserId = meta.ownerUserId,
            familyId = meta.familyId,
            babyId = log.babyId,
            startedAt = log.startedAt.toEpochMilli(),
            endedAt = log.endedAt?.toEpochMilli(),
            note = log.note,
            createdAt = log.createdAt.toEpochMilli(),
            updatedAt = log.updatedAt.toEpochMilli(),
            deletedAt = meta.deletedAt?.toEpochMilli(),
            clientId = meta.clientId,
            schemaVersion = meta.schemaVersion,
        )

        fun fromMap(map: Map<String, Any?>): RemoteSleepLogDto = RemoteSleepLogDto(
            id = map["id"] as? String ?: "",
            ownerUserId = map["ownerUserId"] as? String ?: "",
            familyId = map["familyId"] as? String ?: "",
            babyId = map["babyId"] as? String ?: "",
            startedAt = (map["startedAt"] as? Number)?.toLong() ?: 0L,
            endedAt = (map["endedAt"] as? Number)?.toLong(),
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
