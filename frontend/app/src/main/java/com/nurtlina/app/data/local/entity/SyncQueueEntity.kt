package com.nurtlina.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_queue",
    indices = [Index("nextRetryAt"), Index("entityType", "entityId")],
)
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payloadJson: String,
    val createdAt: Long,
    val retryCount: Int,
    val nextRetryAt: Long,
    val lastError: String?,
)
