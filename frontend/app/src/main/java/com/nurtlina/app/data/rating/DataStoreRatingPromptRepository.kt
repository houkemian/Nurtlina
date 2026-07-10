package com.nurtlina.app.data.rating

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.nurtlina.app.domain.rating.RatingPromptState
import com.nurtlina.app.domain.repository.RatingPromptRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStoreRatingPromptRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : RatingPromptRepository {

    private object Keys {
        val SHOWN_COUNT = intPreferencesKey("rating_prompt_shown_count")
        val LAST_SHOWN_AT = longPreferencesKey("rating_prompt_last_shown_at")
        val DISMISSED_PERMANENTLY = booleanPreferencesKey("rating_prompt_dismissed_permanently")
        val RATED_AT = longPreferencesKey("rating_prompt_rated_at")
        val POSITIVE_ACTION_COUNT = intPreferencesKey("rating_prompt_positive_action_count")
        val FEED_LOGGED_COUNT = intPreferencesKey("rating_prompt_feed_logged_count")
        val FIRST_LAUNCH_AT = longPreferencesKey("rating_prompt_first_launch_at")
        val LAST_NOTIFICATION_OPEN_AT = longPreferencesKey("rating_prompt_last_notification_open_at")
        val LAST_NEGATIVE_ACTION_AT = longPreferencesKey("rating_prompt_last_negative_action_at")
    }

    override fun observe(): Flow<RatingPromptState> = dataStore.data.map { prefs ->
        RatingPromptState(
            ratingPromptShownCount = prefs[Keys.SHOWN_COUNT] ?: 0,
            ratingPromptLastShownAt = prefs[Keys.LAST_SHOWN_AT]?.toInstant(),
            ratingPromptDismissedPermanently = prefs[Keys.DISMISSED_PERMANENTLY] ?: false,
            ratingPromptRatedAt = prefs[Keys.RATED_AT]?.toInstant(),
            eligiblePositiveActionCount = prefs[Keys.POSITIVE_ACTION_COUNT] ?: 0,
            feedLoggedCount = prefs[Keys.FEED_LOGGED_COUNT] ?: 0,
            firstLaunchAt = prefs[Keys.FIRST_LAUNCH_AT]?.toInstant(),
            lastNotificationOpenAt = prefs[Keys.LAST_NOTIFICATION_OPEN_AT]?.toInstant(),
            lastNegativeActionAt = prefs[Keys.LAST_NEGATIVE_ACTION_AT]?.toInstant(),
        )
    }

    override suspend fun get(): RatingPromptState = observe().first()

    override suspend fun ensureFirstLaunchAt(now: Instant) {
        dataStore.edit { prefs ->
            if (prefs[Keys.FIRST_LAUNCH_AT] == null) {
                prefs[Keys.FIRST_LAUNCH_AT] = now.toEpochMilli()
            }
        }
    }

    override suspend fun incrementFeedLogged() {
        dataStore.edit { prefs ->
            prefs[Keys.FEED_LOGGED_COUNT] = (prefs[Keys.FEED_LOGGED_COUNT] ?: 0) + 1
        }
    }

    override suspend fun incrementPositiveAction() {
        dataStore.edit { prefs ->
            prefs[Keys.POSITIVE_ACTION_COUNT] = (prefs[Keys.POSITIVE_ACTION_COUNT] ?: 0) + 1
        }
    }

    override suspend fun recordShown(now: Instant) {
        dataStore.edit { prefs ->
            prefs[Keys.SHOWN_COUNT] = (prefs[Keys.SHOWN_COUNT] ?: 0) + 1
            prefs[Keys.LAST_SHOWN_AT] = now.toEpochMilli()
        }
    }

    override suspend fun recordMaybeLater(now: Instant) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_SHOWN_AT] = now.toEpochMilli()
        }
    }

    override suspend fun recordNoThanks() {
        dataStore.edit { prefs ->
            prefs[Keys.DISMISSED_PERMANENTLY] = true
        }
    }

    override suspend fun recordRateClicked(now: Instant) {
        dataStore.edit { prefs ->
            prefs[Keys.RATED_AT] = now.toEpochMilli()
        }
    }

    override suspend fun recordNotificationOpened(now: Instant) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_NOTIFICATION_OPEN_AT] = now.toEpochMilli()
        }
    }

    override suspend fun recordNegativeAction(now: Instant) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_NEGATIVE_ACTION_AT] = now.toEpochMilli()
        }
    }

    private fun Long.toInstant(): Instant = Instant.ofEpochMilli(this)
}
