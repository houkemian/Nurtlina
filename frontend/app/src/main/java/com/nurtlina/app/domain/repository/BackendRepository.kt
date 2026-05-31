package com.nurtlina.app.domain.repository

import com.nurtlina.app.domain.model.BackendInitResult

interface BackendRepository {
    suspend fun initMe(clientId: String, appVersion: String): BackendInitResult
}
