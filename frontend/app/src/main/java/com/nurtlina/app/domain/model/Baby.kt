package com.nurtlina.app.domain.model

import java.time.Instant
import java.time.LocalDate

data class Baby(
    val id: String,
    val name: String,
    val birthDate: LocalDate?,
    val avatarColor: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val archivedAt: Instant?,
)
