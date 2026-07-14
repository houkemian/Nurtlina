package com.nurtlina.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nurtlina.app.data.local.dao.BabyDao
import com.nurtlina.app.data.local.dao.DiaperLogDao
import com.nurtlina.app.data.local.dao.FeedLogDao
import com.nurtlina.app.data.local.dao.FeedingReminderFeedbackDao
import com.nurtlina.app.data.local.dao.SleepLogDao
import com.nurtlina.app.data.local.dao.SyncQueueDao
import com.nurtlina.app.data.local.entity.BabyEntity
import com.nurtlina.app.data.local.entity.DiaperLogEntity
import com.nurtlina.app.data.local.entity.FeedLogEntity
import com.nurtlina.app.data.local.entity.FeedingReminderFeedbackEntity
import com.nurtlina.app.data.local.entity.SleepLogEntity
import com.nurtlina.app.data.local.entity.SyncQueueEntity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BabyEntity::class,
        FeedLogEntity::class,
        DiaperLogEntity::class,
        SleepLogEntity::class,
        SyncQueueEntity::class,
        FeedingReminderFeedbackEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class NurtlinaDatabase : RoomDatabase() {
    abstract fun babyDao(): BabyDao
    abstract fun feedLogDao(): FeedLogDao
    abstract fun diaperLogDao(): DiaperLogDao
    abstract fun sleepLogDao(): SleepLogDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun feedingReminderFeedbackDao(): FeedingReminderFeedbackDao

    companion object {
        const val DATABASE_NAME = "nurtlina.db"

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS feeding_reminder_feedback (
                        id TEXT PRIMARY KEY NOT NULL,
                        babyId TEXT NOT NULL,
                        reminderTime INTEGER NOT NULL,
                        feedbackType TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        familyId TEXT,
                        clientId TEXT,
                        syncStatus TEXT NOT NULL DEFAULT 'PENDING',
                        syncVersion INTEGER NOT NULL DEFAULT 1,
                        lastSyncedAt INTEGER,
                        FOREIGN KEY (babyId) REFERENCES babies(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_feeding_reminder_feedback_babyId ON feeding_reminder_feedback(babyId)")
            }
        }
    }
}
