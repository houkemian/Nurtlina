package com.nurtlina.app.data.remote.api

import com.nurtlina.app.domain.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthTokenInterceptor @Inject constructor(
    private val authRepository: AuthRepository,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { authRepository.getIdToken(forceRefresh = false) }
        val request = chain.request()
            .newBuilder()
            .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX $token")
            .build()

        val response = chain.proceed(request)
        if (response.code != HTTP_UNAUTHORIZED) return response

        response.close()
        val refreshedToken = runBlocking { authRepository.getIdToken(forceRefresh = true) }
        val retriedRequest = chain.request()
            .newBuilder()
            .header(AUTHORIZATION_HEADER, "$BEARER_PREFIX $refreshedToken")
            .build()

        return chain.proceed(retriedRequest)
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer"
        private const val HTTP_UNAUTHORIZED = 401
    }
}
