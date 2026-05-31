package com.nurtlina.app.data.remote.api

import android.app.Activity
import com.nurtlina.app.domain.model.AuthState
import com.nurtlina.app.domain.model.AuthUser
import com.nurtlina.app.domain.model.UserAccount
import com.nurtlina.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthTokenInterceptorTest {

    @Test
    fun `401 refreshes token and retries once`() {
        val authRepository = FakeAuthRepository()
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        server.start()

        try {
            val client = OkHttpClient.Builder()
                .addInterceptor(AuthTokenInterceptor(authRepository))
                .build()
            val request = Request.Builder()
                .url(server.url("/api/v1/me/init"))
                .build()

            val response = client.newCall(request).execute()

            assertEquals(200, response.code)
            assertEquals("Bearer initial-token", server.takeRequest().getHeader("Authorization"))
            assertEquals("Bearer refreshed-token", server.takeRequest().getHeader("Authorization"))
            assertEquals(listOf(false, true), authRepository.refreshCalls)
        } finally {
            server.shutdown()
        }
    }

    private class FakeAuthRepository : AuthRepository {
        val refreshCalls = mutableListOf<Boolean>()

        override val authState: Flow<AuthState> = flowOf(AuthState.SignedIn("uid", true, null))

        override fun observeCurrentUser(): Flow<UserAccount?> = flowOf(null)

        override suspend fun ensureSignedIn(): AuthUser = AuthUser("uid", true, null)

        override suspend fun getIdToken(forceRefresh: Boolean): String {
            refreshCalls += forceRefresh
            return if (forceRefresh) "refreshed-token" else "initial-token"
        }

        override suspend fun signInAnonymously(): Result<UserAccount> = Result.failure(NotImplementedError())

        override suspend fun signInWithGoogle(idToken: String): Result<UserAccount> = Result.failure(NotImplementedError())

        override suspend fun linkWithGoogle(): AuthUser = AuthUser("uid", true, null)

        override suspend fun signInWithMicrosoft(activity: Activity): Result<UserAccount> = Result.failure(NotImplementedError())

        override suspend fun linkWithMicrosoft(): AuthUser = AuthUser("uid", true, null)

        override suspend fun signInWithEmail(email: String, password: String): Result<UserAccount> =
            Result.failure(NotImplementedError())

        override suspend fun createAccountWithEmail(email: String, password: String): Result<UserAccount> =
            Result.failure(NotImplementedError())

        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = Result.failure(NotImplementedError())

        override suspend fun signOut() = Unit

        override suspend fun provisionFamily(): Result<String> = Result.failure(NotImplementedError())

        override fun currentUserId(): String? = "uid"

        override fun isSignedIn(): Boolean = true
    }
}
