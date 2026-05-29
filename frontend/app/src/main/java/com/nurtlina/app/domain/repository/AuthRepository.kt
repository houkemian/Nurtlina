package com.nurtlina.app.domain.repository

import com.nurtlina.app.domain.model.UserAccount
import kotlinx.coroutines.flow.Flow

/**
 * Manages Firebase Auth state and family provisioning.
 * Offline flows must never depend on this completing successfully.
 */
interface AuthRepository {

    /** Emits the current signed-in account, or null if signed out. */
    fun observeCurrentUser(): Flow<UserAccount?>

    /**
     * Signs in anonymously. Creates a stable identity for backup/sync without
     * requiring the user to create an account up front.
     */
    suspend fun signInAnonymously(): Result<UserAccount>

    /**
     * Upgrades an anonymous account or signs in with a Google credential token.
     * Returns the linked account on success.
     */
    suspend fun signInWithGoogle(idToken: String): Result<UserAccount>

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
