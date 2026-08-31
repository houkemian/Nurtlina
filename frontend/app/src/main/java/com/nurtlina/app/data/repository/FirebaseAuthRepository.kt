package com.nurtlina.app.data.repository

import android.app.Activity
import com.nurtlina.app.data.remote.FirebaseAuthSource
import com.nurtlina.app.data.remote.FirestoreSource
import com.nurtlina.app.domain.model.AuthState
import com.nurtlina.app.domain.model.AuthUser
import com.nurtlina.app.domain.model.UserAccount
import com.nurtlina.app.domain.model.toAuthUser
import com.nurtlina.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val authSource: FirebaseAuthSource,
    private val firestoreSource: FirestoreSource,
) : AuthRepository {

    override val authState: Flow<AuthState> = authSource.observeCurrentUser()
        .map { account ->
            if (account == null) {
                AuthState.SignedOut()
            } else {
                AuthState.SignedIn(
                    uid = account.uid,
                    isAnonymous = account.isAnonymous,
                    email = account.email,
                )
            }
        }
        .catch { e -> emit(AuthState.Error(e.message ?: "Authentication failed", e)) }

    override fun observeCurrentUser(): Flow<UserAccount?> =
        authSource.observeCurrentUser().map { account ->
            account?.let { enrichWithFamilyAndEntitlement(it) }
        }

    override suspend fun ensureSignedIn(): AuthUser {
        val existing = authSource.currentUser()
        if (existing != null) return enrichWithFamilyAndEntitlement(existing).toAuthUser()

        val signedIn = authSource.signInAnonymously()
        return enrichWithFamilyAndEntitlement(signedIn).toAuthUser()
    }

    override suspend fun getIdToken(forceRefresh: Boolean): String =
        authSource.getIdToken(forceRefresh)

    override suspend fun signInAnonymously(): Result<UserAccount> = runCatching {
        val account = authSource.signInAnonymously()
        enrichWithFamilyAndEntitlement(account)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<UserAccount> = runCatching {
        val account = if (authSource.isSignedIn()) {
            runCatching { authSource.linkAnonymousWithGoogle(idToken) }
                .getOrElse { authSource.signInWithGoogle(idToken) }
        } else {
            authSource.signInWithGoogle(idToken)
        }
        enrichWithFamilyAndEntitlement(account)
    }

    override suspend fun linkWithGoogle(): AuthUser =
        ensureSignedIn()

    override suspend fun signInWithMicrosoft(activity: Activity): Result<UserAccount> = runCatching {
        val account = authSource.signInWithMicrosoft(activity)
        enrichWithFamilyAndEntitlement(account)
    }

    override suspend fun linkWithMicrosoft(): AuthUser =
        ensureSignedIn()

    override suspend fun signInWithEmail(email: String, password: String): Result<UserAccount> = runCatching {
        val account = if (authSource.isSignedIn()) {
            runCatching { authSource.linkAnonymousWithEmail(email, password) }
                .getOrElse { authSource.signInWithEmail(email, password) }
        } else {
            authSource.signInWithEmail(email, password)
        }
        enrichWithFamilyAndEntitlement(account)
    }

    override suspend fun createAccountWithEmail(email: String, password: String): Result<UserAccount> = runCatching {
        val account = if (authSource.isSignedIn()) {
            runCatching { authSource.linkAnonymousWithEmail(email, password) }
                .getOrElse { authSource.createAccountWithEmail(email, password) }
        } else {
            authSource.createAccountWithEmail(email, password)
        }
        enrichWithFamilyAndEntitlement(account)
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        authSource.sendPasswordResetEmail(email)
    }

    override suspend fun signOut() {
        authSource.signOut()
    }

    override suspend fun provisionFamily(): Result<String> = runCatching {
        authSource.provisionFamily()
    }

    override fun currentUserId(): String? = authSource.currentUserId()

    override fun isSignedIn(): Boolean = authSource.isSignedIn()

    // ── Helpers ───────────────────────────────────────────────────────────────

    private suspend fun enrichWithFamilyAndEntitlement(account: UserAccount): UserAccount {
        val familyId = runCatching {
            firestoreSource.fetchFamilyId(account.uid)
        }.getOrNull()

        val isProActive = if (familyId != null) {
            runCatching {
                firestoreSource.fetchEntitlement(account.uid)
            }.getOrDefault(false)
        } else {
            false
        }

        return account.copy(familyId = familyId, isProActive = isProActive)
    }
}
