package com.nurtlina.app.di

import android.content.Context
import androidx.room.Room
import com.nurtlina.app.data.local.dao.BabyDao
import com.nurtlina.app.data.local.dao.BottleDao
import com.nurtlina.app.data.local.dao.DiaperLogDao
import com.nurtlina.app.data.local.dao.FeedLogDao
import com.nurtlina.app.data.local.dao.SleepLogDao
import com.nurtlina.app.data.local.db.NurtlinaDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NurtlinaDatabase =
        Room.databaseBuilder(context, NurtlinaDatabase::class.java, NurtlinaDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideBabyDao(db: NurtlinaDatabase): BabyDao = db.babyDao()

    @Provides
    fun provideBottleDao(db: NurtlinaDatabase): BottleDao = db.bottleDao()

    @Provides
    fun provideFeedLogDao(db: NurtlinaDatabase): FeedLogDao = db.feedLogDao()

    @Provides
    fun provideDiaperLogDao(db: NurtlinaDatabase): DiaperLogDao = db.diaperLogDao()

    @Provides
    fun provideSleepLogDao(db: NurtlinaDatabase): SleepLogDao = db.sleepLogDao()
}
