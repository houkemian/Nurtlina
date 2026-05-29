package com.nurtlina.app.domain.repository

import com.nurtlina.app.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observe(): Flow<UserSettings>
    suspend fun get(): UserSettings
    suspend fun update(settings: UserSettings)
}
