package com.nurtlina.app.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nurtlina.app.domain.model.DiaperLog
import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.model.SleepLog
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
    ALL, FEED, DIAPER, SLEEP;
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class LogsViewModel @Inject constructor(
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
            feedLogRepository.observeByBabyAndRange(babyId, from, to),
            diaperLogRepository.observeByBabyAndRange(babyId, from, to),
            sleepLogRepository.observeByBabyAndRange(babyId, from, to),
        ) { feeds, diapers, sleeps ->
            buildList {
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

    fun deleteFeedLog(feedLogId: String) {
        viewModelScope.launch { feedLogRepository.delete(feedLogId) }
    }

    fun deleteDiaperLog(diaperLogId: String) {
        viewModelScope.launch { diaperLogRepository.delete(diaperLogId) }
    }

    fun deleteSleepLog(sleepLogId: String) {
        viewModelScope.launch { sleepLogRepository.delete(sleepLogId) }
    }

    fun goToDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun updateEntry(target: LogEditTarget) {
        viewModelScope.launch {
            when (target) {
                is LogEditTarget.Feed -> {
                    val draft = target.draft
                    val amountMl = draft.amountMl.toDoubleOrNull()
                    feedLogRepository.upsert(
                        target.original.copy(
                            feedType = draft.feedType,
                            amountMl = amountMl,
                            startedAt = draft.time,
                            endedAt = draft.time,
                            note = draft.note.ifBlank { null },
                        )
                    )
                }
                is LogEditTarget.Diaper -> {
                    val draft = target.draft
                    diaperLogRepository.upsert(
                        target.original.copy(
                            diaperType = draft.diaperType,
                            changedAt = draft.time,
                            note = draft.note.ifBlank { null },
                        )
                    )
                }
                is LogEditTarget.Sleep -> {
                    val draft = target.draft
                    sleepLogRepository.upsert(
                        target.original.copy(
                            startedAt = draft.startedAt,
                            endedAt = draft.endedAt,
                            note = draft.note.ifBlank { null },
                        )
                    )
                }
            }
        }
    }

    fun deleteEntry(target: LogEditTarget) {
        viewModelScope.launch {
            when (target) {
                is LogEditTarget.Feed -> feedLogRepository.delete(target.original.id)
                is LogEditTarget.Diaper -> diaperLogRepository.delete(target.original.id)
                is LogEditTarget.Sleep -> sleepLogRepository.delete(target.original.id)
            }
        }
    }
}

// ── Filter predicate ──────────────────────────────────────────────────────────

private fun LogFilter.matches(item: LogItem): Boolean = when (this) {
    LogFilter.ALL -> true
    LogFilter.FEED -> item is LogItem.FeedLogItem
    LogFilter.DIAPER -> item is LogItem.DiaperLogItem
    LogFilter.SLEEP -> item is LogItem.SleepLogItem
}
