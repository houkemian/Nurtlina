package com.nurtlina.app.domain.model

import java.time.Instant

data class FeedLog(
    val id: String,
    val babyId: String,
    val bottleId: String?,
    val feedType: FeedType,
    val amountMl: Double?,
    val startedAt: Instant,
    val endedAt: Instant?,
    val side: NursingSide?,
    val note: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class NursingSide {
    LEFT,
    RIGHT,
    BOTH,
}
