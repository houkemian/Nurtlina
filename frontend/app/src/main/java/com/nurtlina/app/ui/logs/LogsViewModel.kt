package com.nurtlina.app.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.DiaperLog
import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.model.SleepLog
import com.nurtlina.app.domain.repository.BottleRepository
import com.nurtlina.app.domain.repository.DiaperLogRepository
import com.nurtlina.app.domain.repository.FeedLogRepository
import com.nurtlina.app.domain.repository.SettingsRepository
import com.nurtlina.app.domain.repository.SleepLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

// ── Log item model ────────────────────────────────────────────────────────────

sealed interface LogItem {
    val timestamp: Instant

    data class BottleLog(val bottle: Bottle) : LogItem {
        override val timestamp: Instant get() = bottle.preparedAt
    }

    data class FeedLogItem(val feedLog: FeedLog) : LogItem {
        override val timestamp: Instant get() = feedLog.startedAt
    }

    data class DiaperLogItem(val diaperLog: DiaperLog) : LogItem {
        override val timestamp: Instant get() = diaperLog.changedAt
    }

    data class SleepLogItem(val sleepLog: SleepLog) : LogItem {
        override val timestamp: Instant get() = sleepLog.startedAt
    }
}

// ── Log filter ────────────────────────────────────────────────────────────────

enum class LogFilter {
    ALL, BOTTLE, FEED, DIAPER, SLEEP;
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val bottleRepository: BottleRepository,
    private val feedLogRepository: FeedLogRepository,
    private val diaperLogRepository: DiaperLogRepository,
    private val sleepLogRepository: SleepLogRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    // ── Date selection ───────────────────────────────────────────────────────

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    fun goToPreviousDay() {
        _selectedDate.update { it.minusDays(1) }
    }

    fun goToNextDay() {
        _selectedDate.update { it.plusDays(1) }
    }

    // ── Type filter ──────────────────────────────────────────────────────────

    private val _selectedFilter = MutableStateFlow(LogFilter.ALL)
    val selectedFilter: StateFlow<LogFilter> = _selectedFilter.asStateFlow()

    fun setFilter(filter: LogFilter) {
        _selectedFilter.value = filter
    }

    // ── Selected baby (from persisted settings) ──────────────────────────────

    private val currentBabyId: StateFlow<String?> = settingsRepository
        .observe()
        .map { it.selectedBabyId }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )

    // ── Combined log items ───────────────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    val logItems: StateFlow<List<LogItem>> = combine(
        currentBabyId,
        _selectedDate,
        _selectedFilter,
    ) { babyId, date, filter ->
        Triple(babyId, date, filter)
    }.flatMapLatest { (babyId, date, filter) ->
        if (babyId == null) {
            return@flatMapLatest flowOf(emptyList())
        }
        val zone = ZoneId.systemDefault()
        val from: Instant = date.atStartOfDay(zone).toInstant()
        val to: Instant = date.plusDays(1).atStartOfDay(zone).toInstant()

        combine(
            bottleRepository.observeAll(babyId).map { bottles ->
                bottles.filter { b -> b.preparedAt >= from && b.preparedAt < to }
            },
            feedLogRepository.observeByBabyAndRange(babyId, from, to),
            diaperLogRepository.observeByBabyAndRange(babyId, from, to),
            sleepLogRepository.observeByBabyAndRange(babyId, from, to),
        ) { bottles, feeds, diapers, sleeps ->
            buildList {
                addAll(bottles.map { LogItem.BottleLog(it) })
                addAll(feeds.map { LogItem.FeedLogItem(it) })
                addAll(diapers.map { LogItem.DiaperLogItem(it) })
                addAll(sleeps.map { LogItem.SleepLogItem(it) })
            }
                .filter { item -> filter.matches(item) }
                .sortedByDescending { it.timestamp }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList(),
    )

    // ── Delete actions ───────────────────────────────────────────────────────

    fun deleteBottle(bottleId: String) {
        viewModelScope.launch { bottleRepository.delete(bottleId) }
    }

    fun deleteFeedLog(feedLogId: String) {
        viewModelScope.launch { feedLogRepository.delete(feedLogId) }
    }

    fun deleteDiaperLog(diaperLogId: String) {
        viewModelScope.launch { diaperLogRepository.delete(diaperLogId) }
    }

    fun deleteSleepLog(sleepLogId: String) {
        viewModelScope.launch { sleepLogRepository.delete(sleepLogId) }
    }
}

// ── Filter predicate ──────────────────────────────────────────────────────────

private fun LogFilter.matches(item: LogItem): Boolean = when (this) {
    LogFilter.ALL -> true
    LogFilter.BOTTLE -> item is LogItem.BottleLog
    LogFilter.FEED -> item is LogItem.FeedLogItem
    LogFilter.DIAPER -> item is LogItem.DiaperLogItem
    LogFilter.SLEEP -> item is LogItem.SleepLogItem
}
