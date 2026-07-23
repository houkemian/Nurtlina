package com.nurtlina.app.core.notification

import com.nurtlina.app.domain.model.UserSettings
import com.nurtlina.app.domain.repository.BabyRepository
import com.nurtlina.app.domain.repository.FeedLogRepository
import com.nurtlina.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NextFeedReminderCoordinator @Inject constructor(
    private val babyRepository: BabyRepository,
    private val feedLogRepository: FeedLogRepository,
    private val settingsRepository: SettingsRepository,
    private val scheduler: NextFeedNotificationScheduler,
) {
    suspend fun refreshForBaby(babyId: String) {
        refreshForBaby(babyId, settingsRepository.get())
    }

    suspend fun refreshAll() {
        val settings = settingsRepository.get()
        babyRepository.observeAll().first().forEach { baby ->
            refreshForBaby(baby.id, settings)
        }
    }

    private suspend fun refreshForBaby(babyId: String, settings: UserSettings) {
        if (!settings.notificationEnabled ||
            !settings.feedingReminderEnabled ||
            settings.nightModeEnabled
        ) {
            scheduler.cancel(babyId)
            return
        }

        val latestFeed = feedLogRepository.getRecentByBaby(babyId, limit = 1).firstOrNull()
        if (latestFeed == null) {
            scheduler.cancel(babyId)
            return
        }

        scheduler.schedule(
            babyId = babyId,
            lastFeedStartedAt = latestFeed.startedAt,
            intervalMinutes = settings.feedReminderIntervalMinutes,
        )
    }
}
