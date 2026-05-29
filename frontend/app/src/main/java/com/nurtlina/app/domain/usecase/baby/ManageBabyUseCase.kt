package com.nurtlina.app.domain.usecase.baby

import com.nurtlina.app.domain.model.Baby
import com.nurtlina.app.domain.repository.BabyRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class ManageBabyUseCase @Inject constructor(
    private val babyRepository: BabyRepository,
) {
    suspend fun create(
        name: String,
        birthDate: LocalDate?,
        avatarColor: String?,
    ): Baby {
        val now = Instant.now()
        val baby = Baby(
            id = UUID.randomUUID().toString(),
            name = name,
            birthDate = birthDate,
            avatarColor = avatarColor,
            createdAt = now,
            updatedAt = now,
            archivedAt = null,
        )
        babyRepository.upsert(baby)
        return baby
    }

    suspend fun update(baby: Baby): Baby {
        val updated = baby.copy(updatedAt = Instant.now())
        babyRepository.upsert(updated)
        return updated
    }

    suspend fun archive(id: String) = babyRepository.archive(id)

    suspend fun countActiveBabies(): Int = babyRepository.count()
}
