package com.nurtlina.app.domain.model

import java.time.Instant

data class UserSettings(
    val language: String = "en",
    val unit: UnitType = UnitType.ML,
    val guidelineRegion: GuidelineRegion = GuidelineRegion.US,
    val notificationEnabled: Boolean = true,
    val reminderBeforeExpiryMinutes: Int = 15,
    val nightModeEnabled: Boolean = false,
    val theme: ThemeType = ThemeType.SYSTEM,
    val onboardingCompleted: Boolean = false,
    val selectedBabyId: String? = null,
    val preExpiry15MinEnabled: Boolean = true,
    val feedingReminderEnabled: Boolean = true,
)
