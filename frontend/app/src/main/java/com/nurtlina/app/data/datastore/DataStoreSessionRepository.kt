package com.nurtlina.app.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nurtlina.app.domain.model.SessionInfo
import com.nurtlina.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreSessionRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SessionRepository {

    override fun observe(): Flow<SessionInfo> = dataStore.data.map { prefs ->
        SessionInfo(
            backendUserId = prefs[Keys.BACKEND_USER_ID],
            defaultFamilyId = prefs[Keys.DEFAULT_FAMILY_ID],
            clientId = prefs[Keys.CLIENT_ID] ?: DEFAULT_CLIENT_PREFIX + UUID.randomUUID(),
            lastInitAt = prefs[Keys.LAST_INIT_AT]?.let { Instant.ofEpochMilli(it) },
        )
    }

    override suspend fun get(): SessionInfo {
        ensureClientId()
        return observe().first()
    }

    override suspend fun saveBackendSession(
        backendUserId: String,
        defaultFamilyId: String,
        lastInitAt: Instant,
    ) {
        dataStore.edit { prefs ->
            if (prefs[Keys.CLIENT_ID] == null) {
                prefs[Keys.CLIENT_ID] = DEFAULT_CLIENT_PREFIX + UUID.randomUUID()
            }
            prefs[Keys.BACKEND_USER_ID] = backendUserId
            prefs[Keys.DEFAULT_FAMILY_ID] = defaultFamilyId
            prefs[Keys.LAST_INIT_AT] = lastInitAt.toEpochMilli()
        }
    }

    override suspend fun clearBackendSession() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.BACKEND_USER_ID)
            prefs.remove(Keys.DEFAULT_FAMILY_ID)
            prefs.remove(Keys.LAST_INIT_AT)
        }
    }

    private suspend fun ensureClientId() {
        dataStore.edit { prefs ->
            if (prefs[Keys.CLIENT_ID] == null) {
                prefs[Keys.CLIENT_ID] = DEFAULT_CLIENT_PREFIX + UUID.randomUUID()
            }
        }
    }

    private object Keys {
        val BACKEND_USER_ID = stringPreferencesKey("backend_user_id")
        val DEFAULT_FAMILY_ID = stringPreferencesKey("default_family_id")
        val CLIENT_ID = stringPreferencesKey("client_id")
        val LAST_INIT_AT = longPreferencesKey("last_init_at")
    }

    companion object {
        private const val DEFAULT_CLIENT_PREFIX = "android_"
    }
}
