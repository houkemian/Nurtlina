package com.nurtlina.app.di

import com.nurtlina.app.data.datastore.DataStoreSettingsRepository
import com.nurtlina.app.data.datastore.DataStoreSessionRepository
import com.nurtlina.app.data.repository.RoomBabyRepository
import com.nurtlina.app.data.repository.RoomBottleRepository
import com.nurtlina.app.data.repository.RoomDiaperLogRepository
import com.nurtlina.app.data.repository.RoomFeedLogRepository
import com.nurtlina.app.data.repository.RoomSleepLogRepository
import com.nurtlina.app.data.sync.WorkManagerSyncManager
import com.nurtlina.app.domain.repository.BabyRepository
import com.nurtlina.app.domain.repository.BottleRepository
import com.nurtlina.app.domain.repository.DiaperLogRepository
import com.nurtlina.app.domain.repository.FeedLogRepository
import com.nurtlina.app.domain.repository.SettingsRepository
import com.nurtlina.app.domain.repository.SessionRepository
import com.nurtlina.app.domain.repository.SleepLogRepository
import com.nurtlina.app.domain.repository.SyncManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBabyRepository(impl: RoomBabyRepository): BabyRepository

    @Binds
    @Singleton
    abstract fun bindBottleRepository(impl: RoomBottleRepository): BottleRepository

    @Binds
    @Singleton
    abstract fun bindFeedLogRepository(impl: RoomFeedLogRepository): FeedLogRepository

    @Binds
    @Singleton
    abstract fun bindDiaperLogRepository(impl: RoomDiaperLogRepository): DiaperLogRepository

    @Binds
    @Singleton
    abstract fun bindSleepLogRepository(impl: RoomSleepLogRepository): SleepLogRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: DataStoreSessionRepository): SessionRepository

    @Binds
    @Singleton
    abstract fun bindSyncManager(impl: WorkManagerSyncManager): SyncManager
}
