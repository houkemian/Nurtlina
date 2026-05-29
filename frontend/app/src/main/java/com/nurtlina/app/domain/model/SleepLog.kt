package com.nurtlina.app.domain.model

import java.time.Instant

data class SleepLog(
    val id: String,
    val babyId: String,
    val startedAt: Instant,
    val endedAt: Instant?,
    val note: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val isActive: Boolean get() = endedAt == null

    fun durationMillis(): Long? = endedAt?.let { it.toEpochMilli() - startedAt.toEpochMilli() }
}
