package com.nurtlina.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.model.FeedType
import com.nurtlina.app.domain.model.NursingSide
import java.time.Instant

@Entity(
    tableName = "feed_logs",
    foreignKeys = [
        ForeignKey(
            entity = BabyEntity::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("babyId"), Index("startedAt")]
)
data class FeedLogEntity(
    @PrimaryKey val id: String,
    val babyId: String,
    val bottleId: String?,
    val feedType: String,
    val amountMl: Double?,
    val startedAt: Long,
    val endedAt: Long?,
    val side: String?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun toDomain() = FeedLog(
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

    companion object {
        fun fromDomain(log: FeedLog) = FeedLogEntity(
            id = log.id,
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
        )
    }
}
