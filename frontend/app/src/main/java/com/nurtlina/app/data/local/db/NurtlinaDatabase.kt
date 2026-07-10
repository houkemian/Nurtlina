package com.nurtlina.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nurtlina.app.data.local.dao.BabyDao
import com.nurtlina.app.data.local.dao.DiaperLogDao
import com.nurtlina.app.data.local.dao.FeedLogDao
import com.nurtlina.app.data.local.dao.SleepLogDao
import com.nurtlina.app.data.local.dao.SyncQueueDao
import com.nurtlina.app.data.local.entity.BabyEntity
import com.nurtlina.app.data.local.entity.DiaperLogEntity
import com.nurtlina.app.data.local.entity.FeedLogEntity
import com.nurtlina.app.data.local.entity.SleepLogEntity
import com.nurtlina.app.data.local.entity.SyncQueueEntity

@Database(
    entities = [
        BabyEntity::class,
        FeedLogEntity::class,
        DiaperLogEntity::class,
        SleepLogEntity::class,
        SyncQueueEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class NurtlinaDatabase : RoomDatabase() {
    abstract fun babyDao(): BabyDao
    abstract fun feedLogDao(): FeedLogDao
    abstract fun diaperLogDao(): DiaperLogDao
    abstract fun sleepLogDao(): SleepLogDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        const val DATABASE_NAME = "nurtlina.db"
    }
}
