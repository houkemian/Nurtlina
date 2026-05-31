package com.nurtlina.app.domain.repository

import android.app.Activity
import com.nurtlina.app.domain.model.AuthState
import com.nurtlina.app.domain.model.AuthUser
import com.nurtlina.app.domain.model.UserAccount
import kotlinx.coroutines.flow.Flow

/**
 * Manages Firebase Auth state and family provisioning.
 * Offline flows must never depend on this completing successfully.
 */
interface AuthRepository {

    val authState: Flow<AuthState>

    /** Emits the current signed-in account, or null if signed out. */
    fun observeCurrentUser(): Flow<UserAccount?>

    /**
     * Ensures there is always a Firebase identity before backend calls are made.
     * If Firebase already has a user, this must not create a new anonymous user.
     */
    suspend fun ensureSignedIn(): AuthUser

    /** Returns a Firebase ID token for authenticated backend API calls. */
    suspend fun getIdToken(forceRefresh: Boolean = false): String

    /**
     * Signs in anonymously. Creates a stable identity for backup/sync without
     * requiring the user to create an account up front.
     */
    suspend fun signInAnonymously(): Result<UserAccount>

    /**
     * Upgrades an anonymous account or signs in with a Google ID token (from Credential Manager).
     * Returns the linked account on success.
     */
    suspend fun signInWithGoogle(idToken: String): Result<UserAccount>

    /** Placeholder for provider-driven upgrade flows that are launched outside the repository. */
    suspend fun linkWithGoogle(): AuthUser

    /**
     * Launches the Microsoft OAuth web flow. Upgrades an anonymous account or signs in directly.
     * Requires an [Activity] to start the provider activity.
     */
    suspend fun signInWithMicrosoft(activity: Activity): Result<UserAccount>

    /** Placeholder for provider-driven upgrade flows that are launched outside the repository. */
    suspend fun linkWithMicrosoft(): AuthUser

    /**
     * Signs in with email and password. Upgrades an anonymous account if possible.
     */
    suspend fun signInWithEmail(email: String, password: String): Result<UserAccount>

    /**
     * Creates a new email/password account and links it to the current anonymous session
     * if one exists.
     */
    suspend fun createAccountWithEmail(email: String, password: String): Result<UserAccount>

    /**
     * Sends a password-reset email to the given address.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    /** Signs the current user out. Core timer flows must continue working offline. */
    suspend fun signOut()

    /**
     * Ensures the user has a family document provisioned in Firestore via
     * Cloud Functions. Should be called after sign-in when familyId is null.
     * Returns the resolved familyId.
     */
    suspend fun provisionFamily(): Result<String>

    /** Returns the active user ID, or null if not signed in. */
    fun currentUserId(): String?

    /** Returns true if the user is currently signed in (anonymous or linked). */
    fun isSignedIn(): Boolean
}
