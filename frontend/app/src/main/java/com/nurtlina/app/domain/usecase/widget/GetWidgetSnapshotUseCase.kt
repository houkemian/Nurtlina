package com.nurtlina.app.domain.usecase.widget

import com.nurtlina.app.data.billing.EntitlementCacheRepository
import com.nurtlina.app.domain.model.WidgetSnapshot
import com.nurtlina.app.domain.model.WidgetTheme
import com.nurtlina.app.domain.repository.BabyRepository
import com.nurtlina.app.domain.repository.FeedLogRepository
import com.nurtlina.app.domain.repository.SettingsRepository
import com.nurtlina.app.domain.usecase.feeding.GenerateFeedingPredictionUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Builds the widget snapshot: selected baby's last feed and next-feed window.
 */
class GetWidgetSnapshotUseCase @Inject constructor(
    private val babyRepository: BabyRepository,
    private val feedLogRepository: FeedLogRepository,
    private val settingsRepository: SettingsRepository,
    private val generateFeedingPredictionUseCase: GenerateFeedingPredictionUseCase,
    private val entitlementCacheRepository: EntitlementCacheRepository,
) {
    suspend operator fun invoke(): WidgetSnapshot {
        val settings = settingsRepository.get()
        val babies = babyRepository.observeAll().first()
        val baby = babies.firstOrNull { it.id == settings.selectedBabyId }
            ?: babies.firstOrNull()
        val babyId = baby?.id ?: return WidgetSnapshot.Empty

        val latestFeed = feedLogRepository.getRecentByBaby(babyId, limit = 1).firstOrNull()
        val prediction = runCatching { generateFeedingPredictionUseCase(babyId) }.getOrNull()
        val nextFeedAt = prediction?.takeUnless { it.isLearning }?.windowStart

        // Widget themes are a Pro feature. Resolve against the persisted
        // entitlement cache (reliable in the widget process) so free users and
        // expired subscriptions always fall back to the default theme.
        val isPro = runCatching { entitlementCacheRepository.get().isPro }.getOrDefault(false)
        val theme = if (isPro) settings.widgetTheme else WidgetTheme.DEFAULT

        return WidgetSnapshot(
            babyName = baby.name,
            lastFeedAt = latestFeed?.startedAt,
            lastFeedAmountMl = latestFeed?.amountMl,
            unit = settings.unit,
            nextFeedAt = nextFeedAt,
            theme = theme,
        )
    }
}
