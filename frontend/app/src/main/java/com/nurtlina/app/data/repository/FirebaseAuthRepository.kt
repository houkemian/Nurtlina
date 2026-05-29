package com.nurtlina.app.data.repository

import com.nurtlina.app.data.remote.FirebaseAuthSource
import com.nurtlina.app.data.remote.FirestoreSource
import com.nurtlina.app.domain.model.UserAccount
import com.nurtlina.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val authSource: FirebaseAuthSource,
    private val firestoreSource: FirestoreSource,
) : AuthRepository {

    override fun observeCurrentUser(): Flow<UserAccount?> =
        authSource.observeCurrentUser().map { account ->
            account?.let { enrichWithFamilyAndEntitlement(it) }
        }

    override suspend fun signInAnonymously(): Result<UserAccount> = runCatching {
        val account = authSource.signInAnonymously()
        enrichWithFamilyAndEntitlement(account)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<UserAccount> = runCatching {
        // If the user is currently anonymous, try to upgrade to a Google account.
        // If linking fails (e.g., Google account already exists), fall back to direct sign-in.
        val account = if (authSource.isSignedIn()) {
            runCatching { authSource.linkAnonymousWithGoogle(idToken) }
                .getOrElse { authSource.signInWithGoogle(idToken) }
        } else {
            authSource.signInWithGoogle(idToken)
        }
        enrichWithFamilyAndEntitlement(account)
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
