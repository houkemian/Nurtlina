package com.nurtlina.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nurtlina.app.domain.model.FeedingReminderFeedback
import com.nurtlina.app.domain.model.FeedbackType
import java.time.Instant

@Entity(
    tableName = "feeding_reminder_feedback",
    foreignKeys = [
        ForeignKey(
            entity = BabyEntity::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("babyId")]
)
data class FeedingReminderFeedbackEntity(
    @PrimaryKey val id: String,
    val babyId: String,
    val reminderTime: Long,
    val feedbackType: String,
    val createdAt: Long,
    val familyId: String? = null,
    val clientId: String? = null,
    val syncStatus: String = SyncStatus.PENDING.name,
    val syncVersion: Int = 1,
    val lastSyncedAt: Long? = null,
) {
    fun toDomain() = FeedingReminderFeedback(
        id = id,
        babyId = babyId,
        reminderTime = Instant.ofEpochMilli(reminderTime),
        feedbackType = FeedbackType.valueOf(feedbackType),
        createdAt = Instant.ofEpochMilli(createdAt),
    )

    companion object {
        fun fromDomain(feedback: FeedingReminderFeedback) = FeedingReminderFeedbackEntity(
            id = feedback.id,
            babyId = feedback.babyId,
            reminderTime = feedback.reminderTime.toEpochMilli(),
            feedbackType = feedback.feedbackType.name,
            createdAt = feedback.createdAt.toEpochMilli(),
        )
    }
}
