package com.nurtlina.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nurtlina.app.domain.model.Baby
import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.BottleStatus
import com.nurtlina.app.domain.model.BottleTransition
import com.nurtlina.app.domain.model.BottleTransitionResult
import com.nurtlina.app.domain.model.DiaperType
import com.nurtlina.app.domain.model.TodaySummary
import com.nurtlina.app.domain.repository.BabyRepository
import com.nurtlina.app.domain.repository.BottleRepository
import com.nurtlina.app.domain.repository.SettingsRepository
import com.nurtlina.app.domain.usecase.diaper.LogDiaperUseCase
import com.nurtlina.app.domain.usecase.bottle.GetTodaySummaryUseCase
import com.nurtlina.app.domain.usecase.bottle.TransitionBottleUseCase
import com.nurtlina.app.domain.usecase.sleep.SleepUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** Terminal statuses that should not appear in the active-bottle list. */
private val terminalStatuses = setOf(
    BottleStatus.FED,
    BottleStatus.DISCARDED,
    BottleStatus.CANCELED,
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val babyRepository: BabyRepository,
    private val bottleRepository: BottleRepository,
    private val getTodaySummaryUseCase: GetTodaySummaryUseCase,
    private val transitionBottleUseCase: TransitionBottleUseCase,
    private val logDiaperUseCase: LogDiaperUseCase,
    private val sleepUseCase: SleepUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    // ── All babies (for the switcher UI) ────────────────────────────────────

    val babies: StateFlow<List<Baby>> = babyRepository
        .observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    // ── Selected baby ────────────────────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedBaby: StateFlow<Baby?> = settingsRepository
        .observe()
        .map { it.selectedBabyId }
        .flatMapLatest { selectedId ->
            when {
                selectedId != null -> babyRepository.observeById(selectedId)
                else -> babyRepository.observeAll().map { it.firstOrNull() }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )

    // ── Active bottles for the selected baby ─────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeBottles: StateFlow<List<Bottle>> = selectedBaby
        .flatMapLatest { baby ->
            if (baby == null) {
                flowOf(emptyList())
            } else {
                bottleRepository.observeActive(baby.id)
                    .map { bottles -> bottles.filter { it.status !in terminalStatuses } }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    // ── Today's summary ──────────────────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    val todaySummary: StateFlow<TodaySummary?> = selectedBaby
        .flatMapLatest { baby ->
            if (baby == null) flowOf(null) else getTodaySummaryUseCase(baby.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )

    // ── Error state for quick-action failures ────────────────────────────────

    private val _actionError = MutableStateFlow<Throwable?>(null)
    val actionError: StateFlow<Throwable?> = _actionError.asStateFlow()

    // ── Public actions ───────────────────────────────────────────────────────

    fun selectBaby(babyId: String) {
        viewModelScope.launch {
            val current = settingsRepository.get()
            settingsRepository.update(current.copy(selectedBabyId = babyId))
        }
    }

    fun transitionBottle(bottle: Bottle, transition: BottleTransition) {
        viewModelScope.launch {
            runCatching {
                transitionBottleUseCase(bottle, transition)
            }.onSuccess { result ->
                if (result is BottleTransitionResult.Error) {
                    _actionError.value = IllegalStateException(result.reason)
                }
            }.onFailure { _actionError.value = it }
        }
    }

    fun quickLogDiaper(type: DiaperType) {
        val babyId = selectedBaby.value?.id ?: return
        viewModelScope.launch {
            runCatching {
                logDiaperUseCase(
                    babyId = babyId,
                    diaperType = type,
                )
            }.onFailure { _actionError.value = it }
        }
    }

    fun startSleep() {
        val babyId = selectedBaby.value?.id ?: return
        viewModelScope.launch {
            runCatching {
                sleepUseCase.startSleep(babyId = babyId, startedAt = Instant.now())
            }.onFailure { _actionError.value = it }
        }
    }

    fun endSleep() {
        val babyId = selectedBaby.value?.id ?: return
        viewModelScope.launch {
            runCatching {
                sleepUseCase.endSleep(babyId = babyId, endedAt = Instant.now())
            }.onFailure { _actionError.value = it }
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }
}
