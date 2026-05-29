package com.nurtlina.app.domain.usecase.bottle

import com.nurtlina.app.core.notification.BottleNotificationScheduler
import com.nurtlina.app.domain.guideline.BottleStateMachine
import com.nurtlina.app.domain.guideline.DefaultGuidelineRules
import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.BottleTransition
import com.nurtlina.app.domain.model.BottleTransitionResult
import com.nurtlina.app.domain.repository.BottleRepository
import java.time.Instant
import javax.inject.Inject

class TransitionBottleUseCase @Inject constructor(
    private val bottleRepository: BottleRepository,
    private val notificationScheduler: BottleNotificationScheduler,
) {
    suspend operator fun invoke(
        bottle: Bottle,
        transition: BottleTransition,
        now: Instant = Instant.now(),
    ): BottleTransitionResult {
        val rule = DefaultGuidelineRules.forRegionAndType(bottle.guidelineRegion, bottle.milkType)
        val result = BottleStateMachine.transition(bottle, transition, rule, now)
        if (result is BottleTransitionResult.Success) {
            bottleRepository.updateStatus(result.bottle.id, result.bottle)
            notificationScheduler.cancel(bottle.id)
            notificationScheduler.schedule(result.bottle)
        }
        return result
    }
}
