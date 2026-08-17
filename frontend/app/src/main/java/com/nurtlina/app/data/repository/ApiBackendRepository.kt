package com.nurtlina.app.data.repository

import com.nurtlina.app.data.remote.api.BackendApiService
import com.nurtlina.app.data.remote.api.MeInitRequest
import com.nurtlina.app.domain.model.BackendInitResult
import com.nurtlina.app.domain.repository.BackendRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiBackendRepository @Inject constructor(
    private val api: BackendApiService,
) : BackendRepository {

    override suspend fun initMe(clientId: String, appVersion: String): BackendInitResult {
        val response = api.initMe(
            MeInitRequest(
                clientId = clientId,
                appVersion = appVersion,
            ),
        )
        return BackendInitResult(
            userId = response.userId,
            defaultFamilyId = response.defaultFamilyId,
            isNewUser = response.isNewUser,
        )
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        api.deleteAccount()
    }
}
