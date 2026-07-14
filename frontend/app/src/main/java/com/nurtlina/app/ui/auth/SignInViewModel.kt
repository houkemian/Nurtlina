package com.nurtlina.app.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuthException
import com.nurtlina.app.R
import com.nurtlina.app.data.sync.SyncWorker
import com.nurtlina.app.domain.model.UserAccount
import com.nurtlina.app.domain.repository.AuthRepository
import com.nurtlina.app.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val appContext: Context,
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
                error = appContext.getString(R.string.signin_error_microsoft_unavailable),
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
            _uiState.update { it.copy(error = appContext.getString(R.string.signin_error_email_required)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.sendPasswordResetEmail(email.trim())
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = appContext.getString(R.string.signin_password_reset_sent),
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
                        error = appContext.getString(R.string.signin_error_timeout, providerName),
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
            return appContext.getString(R.string.signin_error_with_code, errorCode)
        }

        val msg = message ?: return appContext.getString(R.string.signin_error_generic)
        return when {
            "INVALID_EMAIL" in msg || "invalid-email" in msg ->
                appContext.getString(R.string.signin_error_invalid_email)
            "WRONG_PASSWORD" in msg || "wrong-password" in msg ->
                appContext.getString(R.string.signin_error_wrong_password)
            "USER_NOT_FOUND" in msg || "user-not-found" in msg ->
                appContext.getString(R.string.signin_error_user_not_found)
            "EMAIL_ALREADY_IN_USE" in msg || "email-already-in-use" in msg ->
                appContext.getString(R.string.signin_error_email_in_use)
            "WEAK_PASSWORD" in msg || "weak-password" in msg ->
                appContext.getString(R.string.signin_error_weak_password)
            "NETWORK_ERROR" in msg || "network-request-failed" in msg ->
                appContext.getString(R.string.signin_error_network)
            "CREDENTIAL_ALREADY_IN_USE" in msg || "credential-already-in-use" in msg ->
                appContext.getString(R.string.signin_error_credential_in_use)
            else -> appContext.getString(R.string.signin_error_generic)
        }
    }

    companion object {
        private const val TAG = "NurtlinaAuth"
        private const val MICROSOFT_SIGN_IN_TIMEOUT_MS = 90_000L
    }
}
