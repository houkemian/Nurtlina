package com.nurtlina.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nurtlina.app.core.analytics.Analytics
import com.nurtlina.app.core.notification.NextFeedNotificationScheduler
import com.nurtlina.app.domain.model.Baby
import com.nurtlina.app.domain.model.DiaperType
import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.model.FeedType
import com.nurtlina.app.domain.model.MilkType
import com.nurtlina.app.domain.model.TodaySummary
import com.nurtlina.app.domain.rating.RatingPromptBlockedReason
import com.nurtlina.app.domain.rating.RatingPromptDecision
import com.nurtlina.app.domain.rating.RatingPromptEligibility
import com.nurtlina.app.domain.repository.BabyRepository
import com.nurtlina.app.domain.model.FeedingPrediction
import com.nurtlina.app.domain.repository.FeedLogRepository
import com.nurtlina.app.domain.repository.RatingPromptRepository
import com.nurtlina.app.domain.repository.SettingsRepository
import com.nurtlina.app.domain.usecase.feeding.GenerateFeedingPredictionUseCase
import com.nurtlina.app.domain.usecase.summary.GetTodaySummaryUseCase
import com.nurtlina.app.domain.usecase.diaper.LogDiaperUseCase
import com.nurtlina.app.domain.usecase.feed.LogFeedUseCase
import com.nurtlina.app.domain.usecase.baby.ManageBabyUseCase
import com.nurtlina.app.domain.usecase.sleep.SleepUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val babyRepository: BabyRepository,
    private val feedLogRepository: FeedLogRepository,
    private val getTodaySummaryUseCase: GetTodaySummaryUseCase,
    private val logFeedUseCase: LogFeedUseCase,
    private val manageBabyUseCase: ManageBabyUseCase,
    private val logDiaperUseCase: LogDiaperUseCase,
    private val sleepUseCase: SleepUseCase,
    private val settingsRepository: SettingsRepository,
    private val ratingPromptRepository: RatingPromptRepository,
    private val ratingPromptEligibility: RatingPromptEligibility,
    private val nextFeedNotificationScheduler: NextFeedNotificationScheduler,
    private val generateFeedingPredictionUseCase: GenerateFeedingPredictionUseCase,
    private val analytics: Analytics,
) : ViewModel() {

    // ── All babies ──────────────────────────────────────────────────────────

    val babies: StateFlow<List<Baby>> = babyRepository
        .observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    // ── Selected baby ───────────────────────────────────────────────────────

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

    // ── Latest feed ─────────────────────────────────────────────────────────

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

    // ── Today's summary ─────────────────────────────────────────────────────

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

    // ── Feeding prediction ───────────────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    val feedingPrediction: StateFlow<FeedingPrediction?> = selectedBaby
        .flatMapLatest { baby ->
            if (baby == null) {
                flowOf<FeedingPrediction?>(null)
            } else {
                flow { emit(generateFeedingPredictionUseCase(baby.id)) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )

    // ── Error state ─────────────────────────────────────────────────────────

    private val _actionError = MutableStateFlow<Throwable?>(null)
    val actionError: StateFlow<Throwable?> = _actionError.asStateFlow()

    private val _showRatingPrompt = MutableStateFlow(false)
    val showRatingPrompt: StateFlow<Boolean> = _showRatingPrompt.asStateFlow()

    private var ratingPromptShownThisSession = false

    // ── Public actions ──────────────────────────────────────────────────────

    fun selectBaby(babyId: String) {
        viewModelScope.launch {
            val current = settingsRepository.get()
            settingsRepository.update(current.copy(selectedBabyId = babyId))
        }
    }

    fun addBaby(name: String, birthDate: java.time.LocalDate?) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val baby = manageBabyUseCase.create(
                    name = trimmedName,
                    birthDate = birthDate,
                    avatarColor = null,
                )
                val current = settingsRepository.get()
                settingsRepository.update(current.copy(selectedBabyId = baby.id))
                analytics.logBabyCreated()
            }.onFailure { _actionError.value = it }
        }
    }

    fun logFeed(
        milkType: MilkType,
        amountMl: Double?,
        startedAt: Instant,
        note: String?,
        onLogged: () -> Unit = {},
    ) {
        val babyId = selectedBaby.value?.id ?: return
        viewModelScope.launch {
            runCatching {
                val log = logFeedUseCase(
                    babyId = babyId,
                    feedType = milkType.toFeedType(),
                    amountMl = amountMl,
                    startedAt = startedAt,
                    endedAt = startedAt,
                    note = note,
                )
                scheduleNextFeedReminder(log)
                ratingPromptRepository.incrementPositiveAction()
                ratingPromptRepository.incrementFeedLogged()
                onLogged()
            }.onFailure { _actionError.value = it }
        }
    }

    fun quickLogFeed(amountMl: Double) {
        val babyId = selectedBaby.value?.id ?: return

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
                scheduleNextFeedReminder(log)
                ratingPromptRepository.incrementPositiveAction()
                ratingPromptRepository.incrementFeedLogged()
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
        viewModelScope.launch {
            val prediction = generateFeedingPredictionUseCase(feedLog.babyId)
            if (!prediction.isLearning) {
                nextFeedNotificationScheduler.scheduleWindow(
                    babyId = feedLog.babyId,
                    lastFeedStartedAt = feedLog.startedAt,
                    windowStart = prediction.windowStart,
                )
            }
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

    fun maybeShowRatingPrompt(nightModeEnabled: Boolean) {
        viewModelScope.launch {
            val now = Instant.now()
            ratingPromptRepository.ensureFirstLaunchAt(now)
            val state = ratingPromptRepository.get()
            when (val decision = ratingPromptEligibility.evaluate(
                state = state,
                nightModeEnabled = nightModeEnabled,
                alreadyShownThisSession = ratingPromptShownThisSession,
                now = now,
            )) {
                RatingPromptDecision.Eligible -> {
                    analytics.logRatingPromptEligible(RATING_PROMPT_TRIGGER_SOURCE)
                    ratingPromptRepository.recordShown(now)
                    ratingPromptShownThisSession = true
                    _showRatingPrompt.value = true
                    analytics.logRatingPromptShown(
                        triggerSource = RATING_PROMPT_TRIGGER_SOURCE,
                        shownCount = state.ratingPromptShownCount + 1,
                    )
                }
                is RatingPromptDecision.Blocked -> logRatingPromptBlocked(decision.reason)
            }
        }
    }

    private fun logRatingPromptBlocked(reason: RatingPromptBlockedReason) {
        when (reason) {
            RatingPromptBlockedReason.NIGHT_MODE -> analytics.logRatingPromptBlockedNightMode()
            RatingPromptBlockedReason.RECENT_NOTIFICATION_SESSION -> analytics.logRatingPromptBlockedNotificationSession()
            RatingPromptBlockedReason.RECENT_NEGATIVE_ACTION -> analytics.logRatingPromptBlockedNegativeAction()
            else -> Unit
        }
    }

    fun dismissRatingPromptForMaybeLater() {
        viewModelScope.launch {
            ratingPromptRepository.recordMaybeLater(Instant.now())
            _showRatingPrompt.value = false
            analytics.logRatingPromptMaybeLaterClicked()
        }
    }

    fun dismissRatingPromptPermanently() {
        viewModelScope.launch {
            ratingPromptRepository.recordNoThanks()
            _showRatingPrompt.value = false
            analytics.logRatingPromptNoThanksClicked()
        }
    }

    fun markRatingPromptRateClicked() {
        viewModelScope.launch {
            ratingPromptRepository.recordRateClicked(Instant.now())
            _showRatingPrompt.value = false
            analytics.logRatingPromptRateClicked()
        }
    }
}

private const val RATING_PROMPT_TRIGGER_SOURCE = "today_positive_action"

private fun MilkType.toFeedType(): FeedType = when (this) {
    MilkType.FORMULA -> FeedType.FORMULA
    MilkType.BREAST_MILK -> FeedType.BREAST_MILK
    MilkType.CUSTOM -> FeedType.OTHER
}
