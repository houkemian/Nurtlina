package com.nurtlina.app.data.startup

import android.app.Activity
import android.util.Log
import com.nurtlina.app.data.billing.EntitlementManager
import com.nurtlina.app.domain.model.AuthState
import com.nurtlina.app.domain.model.AuthUser
import com.nurtlina.app.domain.model.BackendInitResult
import com.nurtlina.app.domain.model.SessionInfo
import com.nurtlina.app.domain.model.SyncResult
import com.nurtlina.app.domain.model.UserAccount
import com.nurtlina.app.domain.repository.AuthRepository
import com.nurtlina.app.domain.repository.BackendRepository
import com.nurtlina.app.domain.repository.SessionRepository
import com.nurtlina.app.domain.repository.SyncManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AppStartupCoordinatorTest {

    @Test
    fun `offline startup with cached session still requests sync later`() = runTest {
        val syncManager = FakeSyncManager()
        val coordinator = AppStartupCoordinator(
            authRepository = FakeAuthRepository(isSignedIn = true),
            backendRepository = FailingBackendRepository(),
            entitlementManager = mockk<EntitlementManager>(relaxed = true),
            sessionRepository = FakeSessionRepository(hasSession = true),
            syncManager = syncManager,
        )

        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        try {
            coordinator.start()
        } finally {
            unmockkStatic(Log::class)
        }

        assertTrue(syncManager.requested)
    }

    private class FakeAuthRepository(private val isSignedIn: Boolean) : AuthRepository {
        override val authState: Flow<AuthState> = flowOf(AuthState.SignedIn("uid", true, null))
        override fun observeCurrentUser(): Flow<UserAccount?> = flowOf(null)
        override suspend fun ensureSignedIn(): AuthUser = AuthUser("uid", true, null)
        override suspend fun getIdToken(forceRefresh: Boolean): String = "token"
        override suspend fun signInAnonymously(): Result<UserAccount> = Result.failure(NotImplementedError())
        override suspend fun signInWithGoogle(idToken: String): Result<UserAccount> = Result.failure(NotImplementedError())
        override suspend fun linkWithGoogle(): AuthUser = AuthUser("uid", true, null)
        override suspend fun signInWithMicrosoft(activity: Activity): Result<UserAccount> = Result.failure(NotImplementedError())
        override suspend fun linkWithMicrosoft(): AuthUser = AuthUser("uid", true, null)
        override suspend fun signInWithEmail(email: String, password: String): Result<UserAccount> = Result.failure(NotImplementedError())
        override suspend fun createAccountWithEmail(email: String, password: String): Result<UserAccount> =
            Result.failure(NotImplementedError())
        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = Result.failure(NotImplementedError())
        override suspend fun signOut() = Unit
        override suspend fun provisionFamily(): Result<String> = Result.failure(NotImplementedError())
        override fun currentUserId(): String? = if (isSignedIn) "uid" else null
        override fun isSignedIn(): Boolean = isSignedIn
    }

    private class FailingBackendRepository : BackendRepository {
        override suspend fun initMe(clientId: String, appVersion: String): BackendInitResult =
            error("Network unavailable")

        override suspend fun deleteAccount(): Result<Unit> =
            Result.failure(NotImplementedError())
    }

    private class FakeSessionRepository(hasSession: Boolean) : SessionRepository {
        private val session = SessionInfo(
            backendUserId = if (hasSession) "user-1" else null,
            defaultFamilyId = if (hasSession) "family-1" else null,
            clientId = "android-test",
            lastInitAt = Instant.parse("2024-01-01T00:00:00Z"),
        )

        override fun observe(): Flow<SessionInfo> = flowOf(session)
        override suspend fun get(): SessionInfo = session
        override suspend fun saveBackendSession(backendUserId: String, defaultFamilyId: String, lastInitAt: Instant) = Unit
        override suspend fun clearBackendSession() = Unit
    }

    private class FakeSyncManager : SyncManager {
        var requested = false
        override fun requestSyncSoon() {
            requested = true
        }

        override suspend fun syncNow(): SyncResult = SyncResult(0, 0)
    }
}
