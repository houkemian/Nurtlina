package com.nurtlina.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nurtlina.app.data.local.entity.FeedingReminderFeedbackEntity

@Dao
interface FeedingReminderFeedbackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FeedingReminderFeedbackEntity)

    @Query("SELECT * FROM feeding_reminder_feedback WHERE babyId = :babyId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentByBaby(babyId: String, limit: Int = 10): List<FeedingReminderFeedbackEntity>
}
