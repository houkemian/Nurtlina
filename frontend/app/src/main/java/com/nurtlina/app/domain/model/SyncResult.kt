package com.nurtlina.app.domain.model

data class SyncResult(
    val syncedCount: Int,
    val failedCount: Int,
) {
    val isSuccess: Boolean get() = failedCount == 0
}
