package com.nurtlina.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.functions.FirebaseFunctions
import com.nurtlina.app.domain.model.UserAccount
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
) {
    fun observeCurrentUser(): Flow<UserAccount?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toUserAccount())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInAnonymously(): UserAccount {
        val result = auth.signInAnonymously().await()
        return result.user?.toUserAccount()
            ?: error("Anonymous sign-in returned null user")
    }

    suspend fun signInWithGoogle(idToken: String): UserAccount {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user?.toUserAccount()
            ?: error("Google sign-in returned null user")
    }

    suspend fun linkAnonymousWithGoogle(idToken: String): UserAccount {
        val currentUser = auth.currentUser ?: error("No signed-in user to link")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = currentUser.linkWithCredential(credential).await()
        return result.user?.toUserAccount()
            ?: error("Account linking returned null user")
    }

    fun signOut() {
        auth.signOut()
    }

    fun currentUserId(): String? = auth.currentUser?.uid

    fun isSignedIn(): Boolean = auth.currentUser != null

    /**
     * Calls the Cloud Function to provision a family document for this user.
     * Returns the familyId string.
     */
    suspend fun provisionFamily(): String {
        val result = functions
            .getHttpsCallable("provisionFamily")
            .call()
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = result.getData() as? Map<String, Any>
            ?: error("provisionFamily returned unexpected data")

        return data["familyId"] as? String
            ?: error("provisionFamily response missing familyId")
    }

    private fun com.google.firebase.auth.FirebaseUser.toUserAccount() = UserAccount(
        uid = uid,
        email = email,
        isAnonymous = isAnonymous,
        familyId = null, // resolved separately from Firestore
        isProActive = false, // resolved separately from entitlements
    )
}
