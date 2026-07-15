package com.nurtlina.app.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.nurtlina.app.R
import com.nurtlina.app.data.billing.EntitlementManager
import com.nurtlina.app.data.billing.ProStatus
import com.nurtlina.app.data.sync.SyncWorker
import com.nurtlina.app.domain.model.Baby
import com.nurtlina.app.domain.model.GuidelineRegion
import com.nurtlina.app.domain.model.SyncState
import com.nurtlina.app.domain.model.UnitType
import com.nurtlina.app.domain.model.UserAccount
import com.nurtlina.app.domain.model.UserSettings
import com.nurtlina.app.domain.repository.AuthRepository
import com.nurtlina.app.domain.repository.BabyRepository
import com.nurtlina.app.domain.repository.DiaperLogRepository
import com.nurtlina.app.domain.repository.FeedLogRepository
import com.nurtlina.app.domain.repository.SettingsRepository
import com.nurtlina.app.domain.repository.SyncRepository
import com.nurtlina.app.domain.repository.SleepLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsRepository: SettingsRepository,
    private val babyRepository: BabyRepository,
    private val feedLogRepository: FeedLogRepository,
    private val diaperLogRepository: DiaperLogRepository,
    private val sleepLogRepository: SleepLogRepository,
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

    fun toggleTestProStatus(): Boolean? = entitlementManager.toggleTestProStatus()

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

    fun refreshNotificationPermission(): Boolean {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        _notificationPermissionGranted.value = granted
        return granted
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

    /** Immediately triggers an incremental sync push/pull through the backend sync API. */
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

    // ── Settings update helpers ───────────────────────────────────────────────

    fun updateGuidelineRegion(region: GuidelineRegion) {
        updateSettings { it.copy(guidelineRegion = region) }
    }

    fun updateUnitType(unitType: UnitType) {
        updateSettings { it.copy(unit = unitType) }
    }

    fun updateNotificationEnabled(enabled: Boolean) {
        updateSettings { it.copy(notificationEnabled = enabled) }
    }

    fun updateNightModeEnabled(enabled: Boolean) {
        updateSettings { it.copy(nightModeEnabled = enabled) }
    }

    fun updateReminderBeforeExpiryMinutes(minutes: Int) {
        updateSettings { it.copy(reminderBeforeExpiryMinutes = minutes) }
    }

    fun updateLanguage(language: String) {
        updateSettings { it.copy(language = language) }
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

    fun updateBaby(babyId: String, name: String, birthDate: java.time.LocalDate?) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val baby = babies.value.firstOrNull { it.id == babyId } ?: return@launch
            babyRepository.upsert(baby.copy(name = trimmed, birthDate = birthDate))
        }
    }

    fun updateFeedReminderInterval(minutes: Int) {
        updateSettings { it.copy(feedReminderIntervalMinutes = minutes) }
    }

    // ── FAQ state ──────────────────────────────────────────────────────────

    private val _showFaq = MutableStateFlow(false)
    val showFaq: StateFlow<Boolean> = _showFaq.asStateFlow()

    fun showFaq() { _showFaq.value = true }
    fun dismissFaq() { _showFaq.value = false }

    // ── CSV export ─────────────────────────────────────────────────────────

    private val _exportFailed = MutableStateFlow(false)
    val exportFailed: StateFlow<Boolean> = _exportFailed.asStateFlow()

    fun dismissExportError() {
        _exportFailed.value = false
    }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            runCatching {
                val babyId = settings.value?.selectedBabyId
                    ?: babies.value.firstOrNull()?.id
                    ?: error("No baby available for export")
                val rangeStart = Instant.ofEpochMilli(Long.MIN_VALUE)
                val rangeEnd = Instant.ofEpochMilli(Long.MAX_VALUE)
                val csv = withContext(Dispatchers.IO) {
                    val feeds = feedLogRepository.getByBabyAndRange(babyId, rangeStart, rangeEnd)
                    val diapers = diaperLogRepository.getByBabyAndRange(babyId, rangeStart, rangeEnd)
                    val sleeps = sleepLogRepository.getByBabyAndRange(babyId, rangeStart, rangeEnd)
                    CareLogCsvExporter.export(feeds, diapers, sleeps)
                }
                val file = withContext(Dispatchers.IO) {
                    val exportDirectory = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
                    java.io.File(exportDirectory, "nurtlina_export_${System.currentTimeMillis()}.csv")
                        .also { it.writeText(csv) }
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                        appContext.getString(R.string.settings_export_csv),
                    )
                )
            }.onFailure {
                _exportFailed.value = true
            }
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
