package com.nurtlina.app.ui.auth

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuthException
import com.nurtlina.app.data.sync.SyncWorker
import com.nurtlina.app.domain.model.UserAccount
import com.nurtlina.app.domain.repository.AuthRepository
import com.nurtlina.app.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

data class SignInUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val snackbarMessage: String? = null,
    val isCreateMode: Boolean = false,
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val workManager: WorkManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<Unit>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    // ── Google (token acquired by Composable via Credential Manager) ──────────

    fun signInWithGoogle(idToken: String) {
        launchSignIn {
            authRepository.signInWithGoogle(idToken)
        }
    }

    fun onGoogleSignInError(message: String) {
        _uiState.update { it.copy(isLoading = false, error = message) }
    }

    // ── Microsoft (Activity required for OAuth web flow) ──────────────────────

    fun signInWithMicrosoft(activity: Activity) {
        Log.i(TAG, "Microsoft sign-in requested")
        launchSignIn(providerName = "Microsoft", timeoutMillis = MICROSOFT_SIGN_IN_TIMEOUT_MS) {
            authRepository.signInWithMicrosoft(activity)
        }
    }

    fun onMicrosoftSignInUnavailable() {
        Log.w(TAG, "Microsoft sign-in could not start: Activity is null")
        _uiState.update {
            it.copy(
                isLoading = false,
                error = "Microsoft sign-in could not start. Please try again.",
            )
        }
    }

    // ── Email / Password ──────────────────────────────────────────────────────

    fun signInWithEmail(email: String, password: String) {
        launchSignIn {
            authRepository.signInWithEmail(email, password)
        }
    }

    fun createAccountWithEmail(email: String, password: String) {
        launchSignIn {
            authRepository.createAccountWithEmail(email, password)
        }
    }

    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Enter your email address first.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.sendPasswordResetEmail(email.trim())
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = "Password reset email sent. Check your inbox.",
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.toUserMessage()) }
                }
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    fun toggleCreateMode() {
        _uiState.update { it.copy(isCreateMode = !it.isCreateMode, error = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun launchSignIn(
        providerName: String = "Sign-in",
        timeoutMillis: Long? = null,
        block: suspend () -> Result<UserAccount>,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = try {
                if (timeoutMillis == null) {
                    block()
                } else {
                    withTimeout(timeoutMillis) { block() }
                }
            } catch (_: TimeoutCancellationException) {
                Log.w(TAG, "$providerName sign-in timed out after $timeoutMillis ms")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "$providerName sign-in did not respond. Check the Firebase provider setup and try again.",
                    )
                }
                return@launch
            }

            result
                .onSuccess { user ->
                    Log.i(TAG, "$providerName sign-in succeeded: uid=${user.uid}, anonymous=${user.isAnonymous}")
                    if (user.familyId == null) {
                        authRepository.provisionFamily()
                    }
                    syncRepository.requestFullSync()
                    SyncWorker.enqueue(workManager)
                    _uiState.update { it.copy(isLoading = false) }
                    _navigationEvents.send(Unit)
                }
                .onFailure { e ->
                    Log.e(TAG, "$providerName sign-in failed", e)
                    _uiState.update { it.copy(isLoading = false, error = e.toUserMessage()) }
                }
        }
    }

    private fun Throwable.toUserMessage(): String {
        if (this is FirebaseAuthException) {
            return "Sign-in failed: $errorCode. ${message ?: "Please try again."}"
        }

        val msg = message ?: return "Sign-in failed. Please try again."
        return when {
            "INVALID_EMAIL" in msg || "invalid-email" in msg ->
                "Please enter a valid email address."
            "WRONG_PASSWORD" in msg || "wrong-password" in msg ->
                "Incorrect password. Try again or reset it."
            "USER_NOT_FOUND" in msg || "user-not-found" in msg ->
                "No account found with this email."
            "EMAIL_ALREADY_IN_USE" in msg || "email-already-in-use" in msg ->
                "This email is already in use."
            "WEAK_PASSWORD" in msg || "weak-password" in msg ->
                "Password must be at least 6 characters."
            "NETWORK_ERROR" in msg || "network-request-failed" in msg ->
                "Network error. Check your connection and try again."
            "CREDENTIAL_ALREADY_IN_USE" in msg || "credential-already-in-use" in msg ->
                "This account is already linked to another profile."
            else -> "Sign-in failed. Please try again."
        }
    }

    companion object {
        private const val TAG = "NurtlinaAuth"
        private const val MICROSOFT_SIGN_IN_TIMEOUT_MS = 90_000L
    }
}
