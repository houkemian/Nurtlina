package com.nurtlina.app.domain.model

data class AuthUser(
    val uid: String,
    val isAnonymous: Boolean,
    val email: String?,
    val displayName: String? = null,
    val familyId: String? = null,
    val isProActive: Boolean = false,
) {
    fun toUserAccount() = UserAccount(
        uid = uid,
        email = email,
        displayName = displayName,
        isAnonymous = isAnonymous,
        familyId = familyId,
        isProActive = isProActive,
    )
}

sealed interface AuthState {
    data object Loading : AuthState

    data class SignedIn(
        val uid: String,
        val isAnonymous: Boolean,
        val email: String?,
    ) : AuthState

    data class SignedOut(
        val reason: String? = null,
    ) : AuthState

    data class Error(
        val message: String,
        val throwable: Throwable? = null,
    ) : AuthState
}

fun UserAccount.toAuthUser() = AuthUser(
    uid = uid,
    isAnonymous = isAnonymous,
    email = email,
    displayName = displayName,
    familyId = familyId,
    isProActive = isProActive,
)
