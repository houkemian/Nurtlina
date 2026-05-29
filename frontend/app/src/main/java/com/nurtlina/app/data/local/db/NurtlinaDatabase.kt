package com.nurtlina.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nurtlina.app.data.local.dao.BabyDao
import com.nurtlina.app.data.local.dao.BottleDao
import com.nurtlina.app.data.local.dao.DiaperLogDao
import com.nurtlina.app.data.local.dao.FeedLogDao
import com.nurtlina.app.data.local.dao.SleepLogDao
import com.nurtlina.app.data.local.entity.BabyEntity
import com.nurtlina.app.data.local.entity.BottleEntity
import com.nurtlina.app.data.local.entity.DiaperLogEntity
import com.nurtlina.app.data.local.entity.FeedLogEntity
import com.nurtlina.app.data.local.entity.SleepLogEntity

@Database(
    entities = [
        BabyEntity::class,
        BottleEntity::class,
        FeedLogEntity::class,
        DiaperLogEntity::class,
        SleepLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NurtlinaDatabase : RoomDatabase() {
    abstract fun babyDao(): BabyDao
    abstract fun bottleDao(): BottleDao
    abstract fun feedLogDao(): FeedLogDao
    abstract fun diaperLogDao(): DiaperLogDao
    abstract fun sleepLogDao(): SleepLogDao

    companion object {
        const val DATABASE_NAME = "nurtlina.db"
    }
}
