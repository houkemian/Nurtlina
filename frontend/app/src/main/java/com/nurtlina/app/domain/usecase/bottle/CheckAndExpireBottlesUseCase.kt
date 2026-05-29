package com.nurtlina.app.domain.usecase.bottle

import com.nurtlina.app.core.notification.BottleNotificationScheduler
import com.nurtlina.app.domain.guideline.ExpiryCalculator
import com.nurtlina.app.domain.model.BottleStatus
import com.nurtlina.app.domain.repository.BottleRepository
import java.time.Instant
import javax.inject.Inject

/**
 * Scans all active bottles and marks expired ones as EXPIRED in the database.
 * Should be called on app foreground and after device restart.
 */
class CheckAndExpireBottlesUseCase @Inject constructor(
    private val bottleRepository: BottleRepository,
    private val notificationScheduler: BottleNotificationScheduler,
) {
    suspend operator fun invoke() {
        val now = Instant.now()
        val activeBottles = bottleRepository.getAllActive()
        activeBottles.forEach { bottle ->
            if (!bottle.status.isTerminal &&
                bottle.status != BottleStatus.EXPIRED &&
                ExpiryCalculator.isExpired(bottle.expiresAt, now)
            ) {
                val expired = bottle.copy(
                    status = BottleStatus.EXPIRED,
                    updatedAt = now,
                )
                bottleRepository.updateStatus(expired.id, expired)
                notificationScheduler.cancel(bottle.id)
            }
        }
    }
}
