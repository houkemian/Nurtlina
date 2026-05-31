package com.nurtlina.app.data.remote

import android.app.Activity
import android.util.Log
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
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

    fun currentUser(): UserAccount? = auth.currentUser?.toUserAccount()

    suspend fun signInAnonymously(): UserAccount {
        val result = auth.signInAnonymously().await()
        return result.user?.toUserAccount()
            ?: error("Anonymous sign-in returned null user")
    }

    suspend fun getIdToken(forceRefresh: Boolean = false): String {
        val currentUser = auth.currentUser ?: error("No signed-in Firebase user")
        return currentUser.getIdToken(forceRefresh).await().token
            ?: error("Firebase returned an empty ID token")
    }

    // ── Google ────────────────────────────────────────────────────────────────

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
            ?: error("Google account linking returned null user")
    }

    // ── Microsoft ─────────────────────────────────────────────────────────────

    /**
     * Launches the Microsoft OAuth web flow via a Chrome Custom Tab.
     * Requires an [Activity] reference to start the provider activity.
     */
    suspend fun signInWithMicrosoft(activity: Activity): UserAccount {
        val provider = OAuthProvider.newBuilder("microsoft.com")
            .setScopes(listOf("openid", "profile", "email"))
            .build()
        Log.i(TAG, "Starting Microsoft sign-in with Firebase OAuthProvider")
        val result = startMicrosoftSignIn(activity, provider)
        Log.i(TAG, "Microsoft sign-in returned user=${result.user?.uid}")
        return result.user?.toUserAccount()
            ?: error("Microsoft sign-in returned null user")
    }

    suspend fun linkAnonymousWithMicrosoft(activity: Activity): UserAccount {
        val currentUser = auth.currentUser ?: error("No signed-in user to link")
        val provider = OAuthProvider.newBuilder("microsoft.com")
            .setScopes(listOf("openid", "profile", "email"))
            .build()
        Log.i(TAG, "Starting Microsoft link with Firebase OAuthProvider for uid=${currentUser.uid}")
        val result = currentUser.startActivityForLinkWithProvider(activity, provider).await()
        Log.i(TAG, "Microsoft link returned user=${result.user?.uid}")
        return result.user?.toUserAccount()
            ?: error("Microsoft account linking returned null user")
    }

    // ── Email / Password ──────────────────────────────────────────────────────

    suspend fun signInWithEmail(email: String, password: String): UserAccount {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user?.toUserAccount()
            ?: error("Email sign-in returned null user")
    }

    suspend fun createAccountWithEmail(email: String, password: String): UserAccount {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user?.toUserAccount()
            ?: error("Account creation returned null user")
    }

    suspend fun linkAnonymousWithEmail(email: String, password: String): UserAccount {
        val currentUser = auth.currentUser ?: error("No signed-in user to link")
        val credential = EmailAuthProvider.getCredential(email, password)
        val result = currentUser.linkWithCredential(credential).await()
        return result.user?.toUserAccount()
            ?: error("Email account linking returned null user")
    }

    suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    private suspend fun startMicrosoftSignIn(
        activity: Activity,
        provider: OAuthProvider,
    ): AuthResult {
        val pendingResult = auth.pendingAuthResult
        return if (pendingResult != null) {
            Log.i(TAG, "Using pending Microsoft auth result")
            pendingResult.await()
        } else {
            Log.i(TAG, "No pending Microsoft auth result; launching provider activity")
            auth.startActivityForSignInWithProvider(activity, provider).await()
        }
    }

    // ── Sign out ──────────────────────────────────────────────────────────────

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
        displayName = displayName,
        isAnonymous = isAnonymous,
        familyId = null,
        isProActive = false,
    )

    companion object {
        private const val TAG = "NurtlinaAuth"
    }
}
