package com.nurtlina.app.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.nurtlina.app.data.billing.EntitlementManager
import com.nurtlina.app.data.billing.ProStatus
import com.nurtlina.app.data.sync.SyncWorker
import com.nurtlina.app.domain.model.Baby
import com.nurtlina.app.domain.model.GuidelineRegion
import com.nurtlina.app.domain.model.SyncState
import com.nurtlina.app.domain.model.ThemeType
import com.nurtlina.app.domain.model.UnitType
import com.nurtlina.app.domain.model.UserAccount
import com.nurtlina.app.domain.model.UserSettings
import com.nurtlina.app.domain.repository.AuthRepository
import com.nurtlina.app.domain.repository.BabyRepository
import com.nurtlina.app.domain.repository.SettingsRepository
import com.nurtlina.app.domain.repository.SyncRepository
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
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val entitlementManager: EntitlementManager,
    private val workManager: WorkManager,
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

    // ── Auth / account state ─────────────────────────────────────────────────

    val currentUser: StateFlow<UserAccount?> = authRepository
        .observeCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )

    // ── Pro / entitlement status ─────────────────────────────────────────────

    val proStatus: StateFlow<ProStatus> = entitlementManager.proStatus
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ProStatus.UNKNOWN,
        )

    // ── Sync state ───────────────────────────────────────────────────────────

    val syncState: StateFlow<SyncState> = syncRepository
        .observeSyncState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = SyncState(null, false, null),
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

    // ── Auth actions ──────────────────────────────────────────────────────────

    fun signOut() {
        viewModelScope.launch {
            SyncWorker.cancel(workManager)
            authRepository.signOut()
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken)
                .onSuccess { user ->
                    if (user.familyId == null) {
                        authRepository.provisionFamily()
                    }
                    syncRepository.requestFullSync()
                    SyncWorker.enqueue(workManager)
                }
        }
    }

    // ── Sync / backup actions ──────────────────────────────────────────────────

    /** Immediately triggers an incremental sync push/pull via WorkManager one-time request. */
    fun triggerSync() {
        viewModelScope.launch {
            syncRepository.syncAll()
        }
    }

    /** Forces a full re-sync on the next sync cycle (downloads all cloud data). */
    fun requestFullSync() {
        viewModelScope.launch {
            syncRepository.requestFullSync()
            SyncWorker.enqueue(workManager)
        }
    }

    /** Re-queries the Play Store to restore entitlements after sign-in on a new device. */
    fun restorePurchases() {
        entitlementManager.restorePurchases()
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
