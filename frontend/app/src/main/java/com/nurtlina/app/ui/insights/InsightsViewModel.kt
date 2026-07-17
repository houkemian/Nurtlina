package com.nurtlina.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nurtlina.app.domain.model.Baby
import com.nurtlina.app.domain.repository.BabyRepository
import com.nurtlina.app.domain.repository.SettingsRepository
import com.nurtlina.app.domain.usecase.insights.GetMultiDaySummaryUseCase
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
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val babyRepository: BabyRepository,
    private val settingsRepository: SettingsRepository,
    private val getMultiDaySummaryUseCase: GetMultiDaySummaryUseCase,
) : ViewModel() {

    // ── Selected baby ─────────────────────────────────────────────────────────

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

    // ── 7-Day trend data (displayed for Pro users) ────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    val weeklySummary: StateFlow<InsightsTrendData?> = selectedBaby
        .flatMapLatest { baby ->
            if (baby == null) flowOf(null) else getMultiDaySummaryUseCase(baby.id, 7)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = null,
        )

    // ── Date range (Pro users can switch to 14d / 30d) ────────────────────────

    private val _selectedRange = MutableStateFlow(InsightsDateRange.SEVEN)
    val selectedRange: StateFlow<InsightsDateRange> = _selectedRange.asStateFlow()

    fun setRange(range: InsightsDateRange) {
        _selectedRange.value = range
    }
}
