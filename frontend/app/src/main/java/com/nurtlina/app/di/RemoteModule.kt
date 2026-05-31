package com.nurtlina.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.nurtlina.app.BuildConfig
import com.nurtlina.app.data.remote.api.AuthTokenInterceptor
import com.nurtlina.app.data.remote.api.BackendApiService
import com.nurtlina.app.data.repository.ApiBackendRepository
import com.nurtlina.app.data.repository.FirebaseAuthRepository
import com.nurtlina.app.data.repository.FirebaseSyncRepository
import com.nurtlina.app.domain.repository.AuthRepository
import com.nurtlina.app.domain.repository.BackendRepository
import com.nurtlina.app.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RemoteModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance("us-central1")

    @Provides
    @Singleton
    fun provideOkHttpClient(authTokenInterceptor: AuthTokenInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authTokenInterceptor)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideBackendApiService(retrofit: Retrofit): BackendApiService =
        retrofit.create(BackendApiService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteBindingModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: FirebaseSyncRepository): SyncRepository

    @Binds
    @Singleton
    abstract fun bindBackendRepository(impl: ApiBackendRepository): BackendRepository
}
