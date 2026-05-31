package com.nurtlina.app.domain.repository

import com.nurtlina.app.domain.model.SessionInfo
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface SessionRepository {
    fun observe(): Flow<SessionInfo>
    suspend fun get(): SessionInfo
    suspend fun saveBackendSession(
        backendUserId: String,
        defaultFamilyId: String,
        lastInitAt: Instant,
    )
    suspend fun clearBackendSession()
}
