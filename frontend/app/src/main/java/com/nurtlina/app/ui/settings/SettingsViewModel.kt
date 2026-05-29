package com.nurtlina.app.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nurtlina.app.domain.model.Baby
import com.nurtlina.app.domain.model.GuidelineRegion
import com.nurtlina.app.domain.model.ThemeType
import com.nurtlina.app.domain.model.UnitType
import com.nurtlina.app.domain.model.UserSettings
import com.nurtlina.app.domain.repository.BabyRepository
import com.nurtlina.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsRepository: SettingsRepository,
    private val babyRepository: BabyRepository,
) : ViewModel() {

    // ── Settings state ───────────────────────────────────────────────────────

    val settings: StateFlow<UserSettings?> = settingsRepository
        .observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )

    // ── Baby list (for multi-baby management in Pro) ─────────────────────────

    val babies: StateFlow<List<Baby>> = babyRepository
        .observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    // ── Notification permission ──────────────────────────────────────────────

    private val _notificationPermissionGranted = MutableStateFlow(false)
    val notificationPermissionGranted: StateFlow<Boolean> =
        _notificationPermissionGranted.asStateFlow()

    fun refreshNotificationPermission() {
        _notificationPermissionGranted.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // ── Settings update helpers ───────────────────────────────────────────────

    fun updateGuidelineRegion(region: GuidelineRegion) {
        updateSettings { it.copy(guidelineRegion = region) }
    }

    fun updateUnitType(unitType: UnitType) {
        updateSettings { it.copy(unit = unitType) }
    }

    fun updateTheme(theme: ThemeType) {
        updateSettings { it.copy(theme = theme) }
    }

    fun updateNotificationEnabled(enabled: Boolean) {
        updateSettings { it.copy(notificationEnabled = enabled) }
    }

    fun updatePreExpiry15MinEnabled(enabled: Boolean) {
        updateSettings { it.copy(preExpiry15MinEnabled = enabled) }
    }

    fun updateFeedingReminderEnabled(enabled: Boolean) {
        updateSettings { it.copy(feedingReminderEnabled = enabled) }
    }

    fun updateSelectedBaby(babyId: String?) {
        viewModelScope.launch {
            val current = settingsRepository.get()
            settingsRepository.update(current.copy(selectedBabyId = babyId))
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private fun updateSettings(transform: (UserSettings) -> UserSettings) {
        val current = settings.value ?: return
        viewModelScope.launch {
            settingsRepository.update(transform(current))
        }
    }
}
