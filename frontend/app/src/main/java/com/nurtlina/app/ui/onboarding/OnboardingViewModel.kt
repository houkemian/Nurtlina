package com.nurtlina.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nurtlina.app.core.analytics.Analytics
import com.nurtlina.app.domain.repository.AuthRepository
import com.nurtlina.app.domain.repository.SettingsRepository
import com.nurtlina.app.domain.usecase.baby.ManageBabyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val manageBabyUseCase: ManageBabyUseCase,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val analytics: Analytics,
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep = _currentStep.asStateFlow()

    val totalSteps = 5

    private val _babyName = MutableStateFlow("")
    val babyName = _babyName.asStateFlow()

    private val _birthDate = MutableStateFlow<LocalDate?>(null)
    val birthDate = _birthDate.asStateFlow()

    private val _avatarColor = MutableStateFlow<String>("#4A90D9")
    val avatarColor = _avatarColor.asStateFlow()

    init {
        analytics.logOnboardingStarted(java.util.Locale.getDefault().language)
    }

    fun setBabyName(name: String) { _babyName.value = name }
    fun setBirthDate(date: LocalDate?) { _birthDate.value = date }
    fun setAvatarColor(color: String) { _avatarColor.value = color }

    fun nextStep() { if (_currentStep.value < totalSteps - 1) _currentStep.value++ }
    fun previousStep() { if (_currentStep.value > 0) _currentStep.value-- }

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            val baby = manageBabyUseCase.create(
                name = _babyName.value.ifBlank { "Baby" },
                birthDate = _birthDate.value,
                avatarColor = _avatarColor.value,
            )
            analytics.logBabyCreated()

            val settings = settingsRepository.get()
            settingsRepository.update(
                settings.copy(
                    onboardingCompleted = true,
                    selectedBabyId = baby.id,
                )
            )

            // Sign in anonymously if needed, then provision the family document.
            // Failures are non-fatal — the app works offline without a cloud identity.
            if (!authRepository.isSignedIn()) {
                authRepository.signInAnonymously()
                    .onSuccess { user ->
                        if (user.familyId == null) {
                            authRepository.provisionFamily()
                        }
                    }
            } else if (authRepository.currentUserId() != null) {
                // Already signed in from app startup; ensure family is provisioned.
                authRepository.provisionFamily()
            }

            onComplete()
        }
    }
}
