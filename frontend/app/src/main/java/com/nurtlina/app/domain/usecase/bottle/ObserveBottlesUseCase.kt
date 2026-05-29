package com.nurtlina.app.domain.usecase.bottle

import com.nurtlina.app.domain.guideline.DefaultGuidelineRules
import com.nurtlina.app.domain.guideline.ExpiryCalculator
import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.BottleStatus
import com.nurtlina.app.domain.repository.BottleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class ObserveBottlesUseCase @Inject constructor(
    private val bottleRepository: BottleRepository,
) {
    /**
     * Observe active bottles, auto-marking expired ones in the UI stream.
     * Actual database expiry update should be triggered when user opens the app.
     */
    operator fun invoke(babyId: String): Flow<List<Bottle>> =
        bottleRepository.observeActive(babyId).map { bottles ->
            val now = Instant.now()
            bottles.map { bottle ->
                val shouldBeExpired = bottle.status != BottleStatus.EXPIRED &&
                    !bottle.status.isTerminal &&
                    ExpiryCalculator.isExpired(bottle.expiresAt, now)
                if (shouldBeExpired) {
                    bottle.copy(status = BottleStatus.EXPIRED)
                } else {
                    bottle
                }
            }
        }

    fun observeAll(babyId: String): Flow<List<Bottle>> =
        bottleRepository.observeAll(babyId)
}
