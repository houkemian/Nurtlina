package com.nurtlina.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nurtlina.app.domain.model.Baby
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "babies")
data class BabyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val birthDate: String?,
    val avatarColor: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val archivedAt: Long?,
) {
    fun toDomain() = Baby(
        id = id,
        name = name,
        birthDate = birthDate?.let { LocalDate.parse(it) },
        avatarColor = avatarColor,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
        archivedAt = archivedAt?.let { Instant.ofEpochMilli(it) },
    )

    companion object {
        fun fromDomain(baby: Baby) = BabyEntity(
            id = baby.id,
            name = baby.name,
            birthDate = baby.birthDate?.toString(),
            avatarColor = baby.avatarColor,
            createdAt = baby.createdAt.toEpochMilli(),
            updatedAt = baby.updatedAt.toEpochMilli(),
            archivedAt = baby.archivedAt?.toEpochMilli(),
        )
    }
}
