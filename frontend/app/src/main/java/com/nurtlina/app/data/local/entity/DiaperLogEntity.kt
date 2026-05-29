package com.nurtlina.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nurtlina.app.domain.model.DiaperLog
import com.nurtlina.app.domain.model.DiaperType
import java.time.Instant

@Entity(
    tableName = "diaper_logs",
    foreignKeys = [
        ForeignKey(
            entity = BabyEntity::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("babyId"), Index("changedAt")]
)
data class DiaperLogEntity(
    @PrimaryKey val id: String,
    val babyId: String,
    val diaperType: String,
    val changedAt: Long,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toDomain() = DiaperLog(
        id = id,
        babyId = babyId,
        diaperType = DiaperType.valueOf(diaperType),
        changedAt = Instant.ofEpochMilli(changedAt),
        note = note,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

    companion object {
        fun fromDomain(log: DiaperLog) = DiaperLogEntity(
            id = log.id,
            babyId = log.babyId,
            diaperType = log.diaperType.name,
            changedAt = log.changedAt.toEpochMilli(),
            note = log.note,
            createdAt = log.createdAt.toEpochMilli(),
            updatedAt = log.updatedAt.toEpochMilli(),
        )
    }
}
