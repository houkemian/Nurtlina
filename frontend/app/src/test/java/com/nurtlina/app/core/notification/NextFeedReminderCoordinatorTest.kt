package com.nurtlina.app.core.notification

import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.model.Baby
import com.nurtlina.app.domain.model.UserSettings
import com.nurtlina.app.domain.repository.BabyRepository
import com.nurtlina.app.domain.repository.FeedLogRepository
import com.nurtlina.app.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import java.time.Instant

class NextFeedReminderCoordinatorTest {
    private val babyRepository = mockk<BabyRepository>()
    private val feedLogRepository = mockk<FeedLogRepository>()
    private val settingsRepository = mockk<SettingsRepository>()
    private val scheduler = mockk<NextFeedNotificationScheduler>(relaxed = true)
    private val coordinator = NextFeedReminderCoordinator(
        babyRepository = babyRepository,
        feedLogRepository = feedLogRepository,
        settingsRepository = settingsRepository,
        scheduler = scheduler,
    )

    @Test
    fun `disabled notifications cancel without reading feeds`() = runTest {
        coEvery { settingsRepository.get() } returns UserSettings(notificationEnabled = false)

        coordinator.refreshForBaby(BABY_ID)

        verify { scheduler.cancel(BABY_ID) }
        coVerify(exactly = 0) { feedLogRepository.getRecentByBaby(any(), any()) }
    }

    @Test
    fun `night mode cancels pending reminder`() = runTest {
        coEvery { settingsRepository.get() } returns UserSettings(nightModeEnabled = true)

        coordinator.refreshForBaby(BABY_ID)

        verify { scheduler.cancel(BABY_ID) }
    }

    @Test
    fun `latest feed uses configured reminder interval`() = runTest {
        val startedAt = Instant.parse("2026-07-23T10:00:00Z")
        val feed = mockk<FeedLog>()
        every { feed.startedAt } returns startedAt
        coEvery { settingsRepository.get() } returns UserSettings(feedReminderIntervalMinutes = 210)
        coEvery { feedLogRepository.getRecentByBaby(BABY_ID, 1) } returns listOf(feed)

        coordinator.refreshForBaby(BABY_ID)

        verify {
            scheduler.schedule(
                babyId = BABY_ID,
                lastFeedStartedAt = startedAt,
                intervalMinutes = 210,
            )
        }
    }

    @Test
    fun `refresh all restores reminder for each baby after reboot`() = runTest {
        val baby = mockk<Baby>()
        val feed = mockk<FeedLog>()
        val startedAt = Instant.parse("2026-07-23T10:00:00Z")
        every { baby.id } returns BABY_ID
        every { feed.startedAt } returns startedAt
        every { babyRepository.observeAll() } returns flowOf(listOf(baby))
        coEvery { settingsRepository.get() } returns UserSettings(feedReminderIntervalMinutes = 180)
        coEvery { feedLogRepository.getRecentByBaby(BABY_ID, 1) } returns listOf(feed)

        coordinator.refreshAll()

        verify { scheduler.schedule(BABY_ID, startedAt, 180) }
    }

    private companion object {
        const val BABY_ID = "baby-1"
    }
}
