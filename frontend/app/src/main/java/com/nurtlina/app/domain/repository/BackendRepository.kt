package com.nurtlina.app.domain.repository

import com.nurtlina.app.domain.model.BackendInitResult

interface BackendRepository {
    suspend fun initMe(clientId: String, appVersion: String): BackendInitResult

    /**
     * Requests deletion of the signed-in user's cloud account and synced data.
     * The backend soft-deletes all owned families/records and revokes Firebase
     * credentials. Local device records are intentionally left untouched.
     */
    suspend fun deleteAccount(): Result<Unit>
}
