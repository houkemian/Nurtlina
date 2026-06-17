package com.nurtlina.app.ui.bottle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nurtlina.app.core.notification.NextFeedNotificationScheduler
import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.BottleStatus
import com.nurtlina.app.domain.model.BottleTransition
import com.nurtlina.app.domain.model.BottleTransitionResult
import com.nurtlina.app.domain.model.FeedType
import com.nurtlina.app.domain.model.GuidelineRegion
import com.nurtlina.app.domain.model.MilkType
import com.nurtlina.app.domain.repository.BottleRepository
import com.nurtlina.app.domain.repository.RatingPromptRepository
import com.nurtlina.app.domain.usecase.bottle.CreateBottleUseCase
import com.nurtlina.app.domain.usecase.bottle.TransitionBottleUseCase
import com.nurtlina.app.domain.usecase.feed.LogFeedUseCase
import com.nurtlina.app.ui.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

// ── One-shot UI events ───────────────────────────────────────────────────────

sealed interface BottleUiEvent {
    /** Emitted after a successful [BottleViewModel.createBottle] call. */
    data class NavigateToDetail(val bottleId: String) : BottleUiEvent

    /** Emitted when an operation fails; the UI should surface a snackbar/dialog. */
    data class ShowError(val cause: Throwable) : BottleUiEvent
}

// ── Statuses that are terminal (no timer, no countdown) ──────────────────────

private val terminalStatuses = setOf(
    BottleStatus.FED,
    BottleStatus.DISCARDED,
    BottleStatus.CANCELED,
)

@HiltViewModel
class BottleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createBottleUseCase: CreateBottleUseCase,
    private val transitionBottleUseCase: TransitionBottleUseCase,
    private val logFeedUseCase: LogFeedUseCase,
    private val bottleRepository: BottleRepository,
    private val ratingPromptRepository: RatingPromptRepository,
    private val nextFeedNotificationScheduler: NextFeedNotificationScheduler,
) : ViewModel() {

    private val bottleId: String? = savedStateHandle[NavRoutes.BottleDetail.ARG_BOTTLE_ID]

    // ── Current bottle ───────────────────────────────────────────────────────

    val bottle: StateFlow<Bottle?> = if (bottleId != null) {
        bottleRepository.observeById(bottleId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = null,
            )
    } else {
        kotlinx.coroutines.flow.MutableStateFlow(null)
    }

    // ── Countdown ────────────────────────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    val countdown: StateFlow<Duration?> = bottle
        .flatMapLatest { b ->
            if (b == null || b.status in terminalStatuses) {
                flowOf(null)
            } else {
                secondTicker(b.expiresAt)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )

    // ── One-shot UI events ───────────────────────────────────────────────────

    private val _uiEvents = Channel<BottleUiEvent>(Channel.BUFFERED)
    val uiEvents: Flow<BottleUiEvent> = _uiEvents.receiveAsFlow()

    // ── Public actions ───────────────────────────────────────────────────────

    fun createBottle(
        babyId: String,
        milkType: MilkType,
        amountMl: Double?,
        preparedAt: Instant,
        guidelineRegion: GuidelineRegion,
        note: String?,
    ) {
        viewModelScope.launch {
            runCatching {
                createBottleUseCase(
                    babyId = babyId,
                    milkType = milkType,
                    amountMl = amountMl,
                    preparedAt = preparedAt,
                    guidelineRegion = guidelineRegion,
                    note = note,
                )
            }.fold(
                onSuccess = { created ->
                    _uiEvents.send(BottleUiEvent.NavigateToDetail(created.id))
                },
                onFailure = { error ->
                    _uiEvents.send(BottleUiEvent.ShowError(error))
                },
            )
        }
    }

    fun transitionBottle(transition: BottleTransition) {
        val current = bottle.value ?: return
        viewModelScope.launch {
            val result = runCatching {
                transitionBottleUseCase(current, transition)
            }.getOrElse { error ->
                _uiEvents.send(BottleUiEvent.ShowError(error))
                return@launch
            }

            when (result) {
                is BottleTransitionResult.Error -> {
                    _uiEvents.send(BottleUiEvent.ShowError(RuntimeException(result.reason)))
                }
                is BottleTransitionResult.Success -> {
                    when (transition) {
                        is BottleTransition.MarkFed -> {
                            runCatching {
                                logFeedFromBottle(result.bottle)
                                ratingPromptRepository.incrementPositiveAction()
                            }.onFailure { error ->
                                _uiEvents.send(BottleUiEvent.ShowError(error))
                            }
                        }
                        is BottleTransition.Discard,
                        is BottleTransition.Cancel -> ratingPromptRepository.recordNegativeAction(Instant.now())
                        else -> Unit
                    }
                }
            }
        }
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

// ── Private helpers ──────────────────────────────────────────────────────────

private fun secondTicker(expiresAt: Instant?): Flow<Duration> = flow {
    while (true) {
        val remaining = if (expiresAt != null) {
            Duration.between(Instant.now(), expiresAt).let { d ->
                if (d.isNegative) Duration.ZERO else d
            }
        } else {
            Duration.ZERO
        }
        emit(remaining)
        delay(1_000L)
    }
}

private fun MilkType.toFeedType(): FeedType = when (this) {
    MilkType.FORMULA -> FeedType.FORMULA
    MilkType.BREAST_MILK -> FeedType.BREAST_MILK
    MilkType.CUSTOM -> FeedType.OTHER
}
