package com.nurtlina.app.domain.model

data class TodaySummary(
    val totalFeedCount: Int,
    val totalAmountMl: Double,
    val diaperCount: Int,
    val sleepDurationMillis: Long,
    val activeSleepStartedAt: Long?,
)
