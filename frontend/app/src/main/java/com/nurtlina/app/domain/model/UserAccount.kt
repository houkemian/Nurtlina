package com.nurtlina.app.domain.model

data class UserAccount(
    val uid: String,
    val email: String?,
    val displayName: String? = null,
    val isAnonymous: Boolean,
    val familyId: String?,
    val isProActive: Boolean,
)
