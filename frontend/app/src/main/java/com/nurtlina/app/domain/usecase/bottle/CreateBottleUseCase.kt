package com.nurtlina.app.domain.usecase.bottle

import com.nurtlina.app.core.notification.BottleNotificationScheduler
import com.nurtlina.app.domain.guideline.DefaultGuidelineRules
import com.nurtlina.app.domain.guideline.ExpiryCalculator
import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.BottleStatus
import com.nurtlina.app.domain.model.GuidelineRegion
import com.nurtlina.app.domain.model.MilkType
import com.nurtlina.app.domain.repository.BottleRepository
import com.nurtlina.app.domain.repository.RatingPromptRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class CreateBottleUseCase @Inject constructor(
    private val bottleRepository: BottleRepository,
    private val notificationScheduler: BottleNotificationScheduler,
    private val ratingPromptRepository: RatingPromptRepository,
) {
    suspend operator fun invoke(
        babyId: String,
        milkType: MilkType,
        amountMl: Double?,
        preparedAt: Instant,
        guidelineRegion: GuidelineRegion,
        note: String?,
    ): Bottle {
        val rule = DefaultGuidelineRules.forRegionAndType(guidelineRegion, milkType)
        val now = Instant.now()
        val bottle = Bottle(
            id = UUID.randomUUID().toString(),
            babyId = babyId,
            milkType = milkType,
            amountMl = amountMl,
            preparedAt = preparedAt,
            feedingStartedAt = null,
            refrigeratedAt = null,
            status = BottleStatus.NOT_STARTED,
            guidelineRegion = guidelineRegion,
            expiresAt = null,
            discardedAt = null,
            fedAt = null,
            note = note,
            createdAt = now,
            updatedAt = now,
        ).let { it.copy(expiresAt = ExpiryCalculator.calculate(it, rule)) }

        bottleRepository.upsert(bottle)
        ratingPromptRepository.incrementBottleTimerCreated()
        notificationScheduler.schedule(bottle)
        return bottle
    }
}
