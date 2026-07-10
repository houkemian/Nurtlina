package com.nurtlina.app.domain.repository

import com.nurtlina.app.domain.rating.RatingPromptState
import java.time.Instant
import kotlinx.coroutines.flow.Flow

interface RatingPromptRepository {
    fun observe(): Flow<RatingPromptState>
    suspend fun get(): RatingPromptState
    suspend fun ensureFirstLaunchAt(now: Instant)
    suspend fun incrementFeedLogged()
    suspend fun incrementPositiveAction()
    suspend fun recordShown(now: Instant)
    suspend fun recordMaybeLater(now: Instant)
    suspend fun recordNoThanks()
    suspend fun recordRateClicked(now: Instant)
    suspend fun recordNotificationOpened(now: Instant)
    suspend fun recordNegativeAction(now: Instant)
}
