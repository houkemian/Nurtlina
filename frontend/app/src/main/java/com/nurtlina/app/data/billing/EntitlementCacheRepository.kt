package com.nurtlina.app.data.billing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntitlementCacheRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    fun observe(): Flow<EntitlementCache> = dataStore.data.map { prefs ->
        EntitlementCache(
            isPro = prefs[Keys.IS_PRO] ?: false,
            plan = prefs[Keys.PLAN],
            status = prefs[Keys.STATUS],
            expiresAt = prefs[Keys.EXPIRES_AT]?.let { Instant.ofEpochMilli(it) },
            lastVerifiedAt = prefs[Keys.LAST_VERIFIED_AT]?.let { Instant.ofEpochMilli(it) },
            gracePeriodUntil = prefs[Keys.GRACE_PERIOD_UNTIL]?.let { Instant.ofEpochMilli(it) },
            source = prefs[Keys.SOURCE],
        )
    }

    suspend fun get(): EntitlementCache = observe().first()

    suspend fun save(cache: EntitlementCache) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_PRO] = cache.isPro
            setStringOrRemove(prefs, Keys.PLAN, cache.plan)
            setStringOrRemove(prefs, Keys.STATUS, cache.status)
            setLongOrRemove(prefs, Keys.EXPIRES_AT, cache.expiresAt?.toEpochMilli())
            setLongOrRemove(prefs, Keys.LAST_VERIFIED_AT, cache.lastVerifiedAt?.toEpochMilli())
            setLongOrRemove(prefs, Keys.GRACE_PERIOD_UNTIL, cache.gracePeriodUntil?.toEpochMilli())
            setStringOrRemove(prefs, Keys.SOURCE, cache.source)
        }
    }

    private fun setStringOrRemove(prefs: MutablePreferencesCompat, key: Preferences.Key<String>, value: String?) {
        if (value == null) prefs.remove(key) else prefs[key] = value
    }

    private fun setLongOrRemove(prefs: MutablePreferencesCompat, key: Preferences.Key<Long>, value: Long?) {
        if (value == null) prefs.remove(key) else prefs[key] = value
    }

    private object Keys {
        val IS_PRO = booleanPreferencesKey("entitlement_is_pro")
        val PLAN = stringPreferencesKey("entitlement_plan")
        val STATUS = stringPreferencesKey("entitlement_status")
        val EXPIRES_AT = longPreferencesKey("entitlement_expires_at")
        val LAST_VERIFIED_AT = longPreferencesKey("entitlement_last_verified_at")
        val GRACE_PERIOD_UNTIL = longPreferencesKey("entitlement_grace_period_until")
        val SOURCE = stringPreferencesKey("entitlement_source")
    }
}

private typealias MutablePreferencesCompat = androidx.datastore.preferences.core.MutablePreferences
