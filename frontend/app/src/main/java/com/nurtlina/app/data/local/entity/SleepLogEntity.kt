package com.nurtlina.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nurtlina.app.domain.model.SleepLog
import java.time.Instant

@Entity(
    tableName = "sleep_logs",
    foreignKeys = [
        ForeignKey(
            entity = BabyEntity::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("babyId"), Index("startedAt"), Index("endedAt")]
)
data class SleepLogEntity(
    @PrimaryKey val id: String,
    val babyId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toDomain() = SleepLog(
        id = id,
        babyId = babyId,
        startedAt = Instant.ofEpochMilli(startedAt),
        endedAt = endedAt?.let { Instant.ofEpochMilli(it) },
        note = note,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

    companion object {
        fun fromDomain(log: SleepLog) = SleepLogEntity(
            id = log.id,
            babyId = log.babyId,
            startedAt = log.startedAt.toEpochMilli(),
            endedAt = log.endedAt?.toEpochMilli(),
            note = log.note,
            createdAt = log.createdAt.toEpochMilli(),
            updatedAt = log.updatedAt.toEpochMilli(),
        )
    }
}
