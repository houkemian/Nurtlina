package com.nurtlina.app.domain.model

import java.time.Instant

data class DiaperLog(
    val id: String,
    val babyId: String,
    val diaperType: DiaperType,
    val changedAt: Instant,
    val note: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
