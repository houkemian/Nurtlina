package com.nurtlina.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nurtlina.app.core.notification.NextFeedNotificationScheduler
import com.nurtlina.app.domain.model.Baby
import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.BottleStatus
import com.nurtlina.app.domain.model.BottleTransition
import com.nurtlina.app.domain.model.BottleTransitionResult
import com.nurtlina.app.domain.model.DiaperType
import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.model.FeedType
import com.nurtlina.app.domain.model.MilkType
import com.nurtlina.app.domain.model.TodaySummary
import com.nurtlina.app.domain.repository.BabyRepository
import com.nurtlina.app.domain.repository.BottleRepository
import com.nurtlina.app.domain.repository.FeedLogRepository
import com.nurtlina.app.domain.repository.SettingsRepository
import com.nurtlina.app.domain.usecase.bottle.GetTodaySummaryUseCase
import com.nurtlina.app.domain.usecase.bottle.TransitionBottleUseCase
import com.nurtlina.app.domain.usecase.diaper.LogDiaperUseCase
import com.nurtlina.app.domain.usecase.feed.LogFeedUseCase
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
    private val feedLogRepository: FeedLogRepository,
    private val getTodaySummaryUseCase: GetTodaySummaryUseCase,
    private val transitionBottleUseCase: TransitionBottleUseCase,
    private val logFeedUseCase: LogFeedUseCase,
    private val logDiaperUseCase: LogDiaperUseCase,
    private val sleepUseCase: SleepUseCase,
    private val settingsRepository: SettingsRepository,
    private val nextFeedNotificationScheduler: NextFeedNotificationScheduler,
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

    // ── Latest feed for the selected baby ───────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    val latestFeed: StateFlow<FeedLog?> = selectedBaby
        .flatMapLatest { baby ->
            if (baby == null) {
                flowOf(null)
            } else {
                feedLogRepository.observeByBaby(baby.id)
                    .map { feeds -> feeds.maxByOrNull { it.startedAt } }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
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
            val result = runCatching {
                transitionBottleUseCase(bottle, transition)
            }.getOrElse { error ->
                _actionError.value = error
                return@launch
            }

            when (result) {
                is BottleTransitionResult.Error -> {
                    _actionError.value = IllegalStateException(result.reason)
                }
                is BottleTransitionResult.Success -> {
                    if (transition is BottleTransition.MarkFed) {
                        runCatching {
                            logFeedFromBottle(result.bottle)
                        }.onFailure { error ->
                            _actionError.value = error
                        }
                    }
                }
            }
        }
    }

    fun quickLogFeed(amountMl: Double) {
        val babyId = selectedBaby.value?.id ?: return
        val activeBottle = activeBottles.value.firstOrNull { bottle ->
            bottle.status == BottleStatus.FEEDING_STARTED || bottle.status == BottleStatus.NOT_STARTED
        }
        if (activeBottle != null) {
            transitionBottle(activeBottle.copy(amountMl = amountMl), BottleTransition.MarkFed)
            return
        }

        val now = Instant.now()
        viewModelScope.launch {
            runCatching {
                val log = logFeedUseCase(
                    babyId = babyId,
                    feedType = FeedType.FORMULA,
                    amountMl = amountMl,
                    startedAt = now,
                    endedAt = now,
                )
                nextFeedNotificationScheduler.schedule(log.babyId, log.startedAt)
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

    fun scheduleNextFeedReminder(feedLog: FeedLog) {
        nextFeedNotificationScheduler.schedule(feedLog.babyId, feedLog.startedAt)
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

    private suspend fun logFeedFromBottle(bottle: Bottle) {
        val fedAt = bottle.fedAt ?: Instant.now()
        val log = logFeedUseCase(
            babyId = bottle.babyId,
            feedType = bottle.milkType.toFeedType(),
            amountMl = bottle.amountMl,
            startedAt = bottle.feedingStartedAt ?: fedAt,
            endedAt = fedAt,
            bottleId = bottle.id,
        )
        nextFeedNotificationScheduler.schedule(log.babyId, log.startedAt)
    }
}

private fun MilkType.toFeedType(): FeedType = when (this) {
    MilkType.FORMULA -> FeedType.FORMULA
    MilkType.BREAST_MILK -> FeedType.BREAST_MILK
    MilkType.CUSTOM -> FeedType.OTHER
}
