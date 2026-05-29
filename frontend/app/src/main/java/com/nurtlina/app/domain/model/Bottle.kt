package com.nurtlina.app.domain.model

import java.time.Instant

data class Bottle(
    val id: String,
    val babyId: String,
    val milkType: MilkType,
    val amountMl: Double?,
    val preparedAt: Instant,
    val feedingStartedAt: Instant?,
    val refrigeratedAt: Instant?,
    val status: BottleStatus,
    val guidelineRegion: GuidelineRegion,
    val expiresAt: Instant?,
    val discardedAt: Instant?,
    val fedAt: Instant?,
    val note: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

sealed interface BottleTransition {
    data object StartFeeding : BottleTransition
    data object Refrigerate : BottleTransition
    data object MarkFed : BottleTransition
    data object Discard : BottleTransition
    data object Cancel : BottleTransition
    data class EditPreparedAt(val newTime: Instant) : BottleTransition
    data class EditAmount(val newAmountMl: Double?) : BottleTransition
}

sealed interface BottleTransitionResult {
    data class Success(val bottle: Bottle) : BottleTransitionResult
    data class Error(val reason: String) : BottleTransitionResult
}
