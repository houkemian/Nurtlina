package com.nurtlina.app.data.remote.dto

import com.nurtlina.app.domain.model.Baby
import com.nurtlina.app.domain.model.SyncMetadata
import java.time.Instant
import java.time.LocalDate

/**
 * Firestore document DTO for the babies sub-collection.
 * Uses epoch-millis longs for all timestamps so they survive Firestore
 * number serialisation without timezone ambiguity.
 */
data class RemoteBabyDto(
    val id: String = "",
    val ownerUserId: String = "",
    val familyId: String = "",
    val name: String = "",
    val birthDateEpochDay: Long? = null,
    val avatarColor: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val clientId: String = "",
    val schemaVersion: Int = SyncMetadata.CURRENT_SCHEMA_VERSION,
) {
    fun toDomain(): Baby = Baby(
        id = id,
        name = name,
        birthDate = birthDateEpochDay?.let { LocalDate.ofEpochDay(it) },
        avatarColor = avatarColor,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
        archivedAt = archivedAt?.let { Instant.ofEpochMilli(it) },
    )

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "ownerUserId" to ownerUserId,
        "familyId" to familyId,
        "name" to name,
        "birthDateEpochDay" to birthDateEpochDay,
        "avatarColor" to avatarColor,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "archivedAt" to archivedAt,
        "deletedAt" to deletedAt,
        "clientId" to clientId,
        "schemaVersion" to schemaVersion,
    )

    companion object {
        fun fromDomain(baby: Baby, meta: SyncMetadata): RemoteBabyDto = RemoteBabyDto(
            id = baby.id,
            ownerUserId = meta.ownerUserId,
            familyId = meta.familyId,
            name = baby.name,
            birthDateEpochDay = baby.birthDate?.toEpochDay(),
            avatarColor = baby.avatarColor,
            createdAt = baby.createdAt.toEpochMilli(),
            updatedAt = baby.updatedAt.toEpochMilli(),
            archivedAt = baby.archivedAt?.toEpochMilli(),
            deletedAt = meta.deletedAt?.toEpochMilli(),
            clientId = meta.clientId,
            schemaVersion = meta.schemaVersion,
        )

        fun fromMap(map: Map<String, Any?>): RemoteBabyDto = RemoteBabyDto(
            id = map["id"] as? String ?: "",
            ownerUserId = map["ownerUserId"] as? String ?: "",
            familyId = map["familyId"] as? String ?: "",
            name = map["name"] as? String ?: "",
            birthDateEpochDay = (map["birthDateEpochDay"] as? Number)?.toLong(),
            avatarColor = map["avatarColor"] as? String,
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
            updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L,
            archivedAt = (map["archivedAt"] as? Number)?.toLong(),
            deletedAt = (map["deletedAt"] as? Number)?.toLong(),
            clientId = map["clientId"] as? String ?: "",
            schemaVersion = (map["schemaVersion"] as? Number)?.toInt()
                ?: SyncMetadata.CURRENT_SCHEMA_VERSION,
        )
    }
}
