package com.nurtlina.app.domain.repository

import com.nurtlina.app.domain.model.SyncResult

interface SyncManager {
    fun requestSyncSoon()
    suspend fun syncNow(): SyncResult
}
