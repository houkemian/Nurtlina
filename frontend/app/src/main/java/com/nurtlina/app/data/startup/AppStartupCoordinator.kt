package com.nurtlina.app.data.startup

import android.util.Log
import com.nurtlina.app.BuildConfig
import com.nurtlina.app.data.billing.EntitlementManager
import com.nurtlina.app.domain.repository.AuthRepository
import com.nurtlina.app.domain.repository.BackendRepository
import com.nurtlina.app.domain.repository.SessionRepository
import com.nurtlina.app.domain.repository.SyncManager
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStartupCoordinator @Inject constructor(
    private val authRepository: AuthRepository,
    private val backendRepository: BackendRepository,
    private val entitlementManager: EntitlementManager,
    private val sessionRepository: SessionRepository,
    private val syncManager: SyncManager,
) {

    suspend fun start() {
        runCatching {
            val session = sessionRepository.get()
            val authUser = authRepository.ensureSignedIn()
            entitlementManager.identify(authUser.uid)
            val initResult = backendRepository.initMe(
                clientId = session.clientId,
                appVersion = BuildConfig.VERSION_NAME,
            )
            sessionRepository.saveBackendSession(
                backendUserId = initResult.userId,
                defaultFamilyId = initResult.defaultFamilyId,
                lastInitAt = Instant.now(),
            )
        }.onFailure { throwable ->
            val cachedSession = runCatching { sessionRepository.get() }.getOrNull()
            if (authRepository.isSignedIn() && cachedSession?.hasBackendSession == true) {
                Log.i(TAG, "Starting with cached session; backend init will retry later.")
            } else {
                Log.w(TAG, "Startup auth/session init failed.", throwable)
            }
        }

        syncManager.requestSyncSoon()
    }

    companion object {
        private const val TAG = "NurtlinaStartup"
    }
}
