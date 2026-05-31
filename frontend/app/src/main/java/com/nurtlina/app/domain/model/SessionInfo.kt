package com.nurtlina.app.domain.model

import java.time.Instant

data class SessionInfo(
    val backendUserId: String?,
    val defaultFamilyId: String?,
    val clientId: String,
    val lastInitAt: Instant?,
) {
    val hasBackendSession: Boolean
        get() = backendUserId != null && defaultFamilyId != null
}
