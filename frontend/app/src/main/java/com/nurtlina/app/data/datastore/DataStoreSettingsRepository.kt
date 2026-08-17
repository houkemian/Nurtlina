package com.nurtlina.app.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nurtlina.app.domain.model.GuidelineRegion
import com.nurtlina.app.domain.model.ThemeType
import com.nurtlina.app.domain.model.UnitType
import com.nurtlina.app.domain.model.WidgetTheme
import com.nurtlina.app.domain.model.UserSettings
import com.nurtlina.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val UNIT = stringPreferencesKey("unit")
        val GUIDELINE_REGION = stringPreferencesKey("guideline_region")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val REMINDER_BEFORE_EXPIRY_MINUTES = intPreferencesKey("reminder_before_expiry_minutes")
        val NIGHT_MODE_ENABLED = booleanPreferencesKey("night_mode_enabled")
        val THEME = stringPreferencesKey("theme")
        val WIDGET_THEME = stringPreferencesKey("widget_theme")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SELECTED_BABY_ID = stringPreferencesKey("selected_baby_id")
        val PRE_EXPIRY_15MIN_ENABLED = booleanPreferencesKey("pre_expiry_15min_enabled")
        val FEEDING_REMINDER_ENABLED = booleanPreferencesKey("feeding_reminder_enabled")
        val FEED_REMINDER_INTERVAL_MINUTES = intPreferencesKey("feed_reminder_interval_minutes")
    }

    override fun observe(): Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            language = prefs[Keys.LANGUAGE] ?: "en",
            unit = prefs[Keys.UNIT]?.let { UnitType.valueOf(it) } ?: UnitType.ML,
            guidelineRegion = prefs[Keys.GUIDELINE_REGION]?.let { GuidelineRegion.valueOf(it) } ?: GuidelineRegion.US,
            notificationEnabled = prefs[Keys.NOTIFICATION_ENABLED] ?: true,
            reminderBeforeExpiryMinutes = prefs[Keys.REMINDER_BEFORE_EXPIRY_MINUTES] ?: 15,
            nightModeEnabled = prefs[Keys.NIGHT_MODE_ENABLED] ?: false,
            theme = prefs[Keys.THEME]?.let { ThemeType.valueOf(it) } ?: ThemeType.SYSTEM,
            widgetTheme = prefs[Keys.WIDGET_THEME]?.let { WidgetTheme.valueOf(it) } ?: WidgetTheme.DEFAULT,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            selectedBabyId = prefs[Keys.SELECTED_BABY_ID],
            preExpiry15MinEnabled = prefs[Keys.PRE_EXPIRY_15MIN_ENABLED] ?: true,
            feedingReminderEnabled = prefs[Keys.FEEDING_REMINDER_ENABLED] ?: true,
            feedReminderIntervalMinutes = prefs[Keys.FEED_REMINDER_INTERVAL_MINUTES] ?: 160,
        )
    }

    override suspend fun get(): UserSettings = observe().first()

    override suspend fun update(settings: UserSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = settings.language
            prefs[Keys.UNIT] = settings.unit.name
            prefs[Keys.GUIDELINE_REGION] = settings.guidelineRegion.name
            prefs[Keys.NOTIFICATION_ENABLED] = settings.notificationEnabled
            prefs[Keys.REMINDER_BEFORE_EXPIRY_MINUTES] = settings.reminderBeforeExpiryMinutes
            prefs[Keys.NIGHT_MODE_ENABLED] = settings.nightModeEnabled
            prefs[Keys.THEME] = settings.theme.name
            prefs[Keys.WIDGET_THEME] = settings.widgetTheme.name
            prefs[Keys.ONBOARDING_COMPLETED] = settings.onboardingCompleted
            if (settings.selectedBabyId != null) {
                prefs[Keys.SELECTED_BABY_ID] = settings.selectedBabyId
            } else {
                prefs.remove(Keys.SELECTED_BABY_ID)
            }
            prefs[Keys.PRE_EXPIRY_15MIN_ENABLED] = settings.preExpiry15MinEnabled
            prefs[Keys.FEEDING_REMINDER_ENABLED] = settings.feedingReminderEnabled
            prefs[Keys.FEED_REMINDER_INTERVAL_MINUTES] = settings.feedReminderIntervalMinutes
        }
    }
}
