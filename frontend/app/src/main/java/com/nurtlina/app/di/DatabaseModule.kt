package com.nurtlina.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nurtlina.app.data.local.dao.BabyDao
import com.nurtlina.app.data.local.dao.DiaperLogDao
import com.nurtlina.app.data.local.dao.FeedLogDao
import com.nurtlina.app.data.local.dao.SleepLogDao
import com.nurtlina.app.data.local.dao.SyncQueueDao
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideBabyDao(db: NurtlinaDatabase): BabyDao = db.babyDao()

    @Provides
    fun provideFeedLogDao(db: NurtlinaDatabase): FeedLogDao = db.feedLogDao()

    @Provides
    fun provideDiaperLogDao(db: NurtlinaDatabase): DiaperLogDao = db.diaperLogDao()

    @Provides
    fun provideSleepLogDao(db: NurtlinaDatabase): SleepLogDao = db.sleepLogDao()

    @Provides
    fun provideSyncQueueDao(db: NurtlinaDatabase): SyncQueueDao = db.syncQueueDao()

    /** v1 → v2: add sync/family columns to core tables + create sync_queue table. */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf("babies", "bottles", "feed_logs", "diaper_logs", "sleep_logs").forEach { table ->
                db.execSQL("ALTER TABLE $table ADD COLUMN familyId TEXT")
                db.execSQL("ALTER TABLE $table ADD COLUMN deletedAt INTEGER")
                db.execSQL("ALTER TABLE $table ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING'")
                db.execSQL("ALTER TABLE $table ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE $table ADD COLUMN clientId TEXT")
                db.execSQL("ALTER TABLE $table ADD COLUMN lastSyncedAt INTEGER")
            }
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS sync_queue (
                    id TEXT NOT NULL PRIMARY KEY,
                    entityType TEXT NOT NULL,
                    entityId TEXT NOT NULL,
                    operation TEXT NOT NULL,
                    payloadJson TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    retryCount INTEGER NOT NULL,
                    nextRetryAt INTEGER NOT NULL,
                    lastError TEXT
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_nextRetryAt ON sync_queue(nextRetryAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_entityType_entityId ON sync_queue(entityType, entityId)")
        }
    }

    /** v2 → v3: drop the bottles table (Bottle entity removed in v2.0). */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS bottles")
        }
    }
}
