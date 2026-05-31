package com.nurtlina.app.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.BottleStatus
import com.nurtlina.app.domain.model.GuidelineRegion
import com.nurtlina.app.domain.model.MilkType
import java.time.Instant

@Entity(
    tableName = "bottles",
    foreignKeys = [
        ForeignKey(
            entity = BabyEntity::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("babyId"), Index("status"), Index("expiresAt")]
)
data class BottleEntity(
    @PrimaryKey val id: String,
    val babyId: String,
    val milkType: String,
    val amountMl: Double?,
    val preparedAt: Long,
    val feedingStartedAt: Long?,
    val refrigeratedAt: Long?,
    val status: String,
    val guidelineRegion: String,
    val expiresAt: Long?,
    val discardedAt: Long?,
    val fedAt: Long?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val familyId: String? = null,
    val deletedAt: Long? = null,
    @ColumnInfo(defaultValue = "'PENDING'") val syncStatus: String = SyncStatus.PENDING.name,
    @ColumnInfo(defaultValue = "1") val syncVersion: Int = 1,
    val clientId: String? = null,
    val lastSyncedAt: Long? = null,
) {
    fun toDomain() = Bottle(
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

    companion object {
        fun fromDomain(bottle: Bottle) = BottleEntity(
            id = bottle.id,
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
        )
    }
}
