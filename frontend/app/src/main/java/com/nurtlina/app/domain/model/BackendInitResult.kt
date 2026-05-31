package com.nurtlina.app.domain.model

data class BackendInitResult(
    val userId: String,
    val defaultFamilyId: String,
    val isNewUser: Boolean,
)
