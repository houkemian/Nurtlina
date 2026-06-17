package com.nurtlina.app.ui.today

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BabyChangingStation
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.nurtlina.app.R
import com.nurtlina.app.domain.model.Baby
import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.BottleStatus
import com.nurtlina.app.domain.model.MilkType
import com.nurtlina.app.domain.model.TodaySummary
import com.nurtlina.app.domain.model.UnitType
import com.nurtlina.app.ui.NurtlinaDialog
import com.nurtlina.app.ui.theme.BottleStatusColors
import com.nurtlina.app.ui.theme.NurtlinaTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.util.concurrent.TimeUnit

private const val KEEP_QUICK_LOG_FEEDBACK_VISIBLE_FOR_DEBUG = false
private const val ML_PER_OUNCE = 29.5735

// --------------------------------------------------------------------------
// UI state
// --------------------------------------------------------------------------

/**
 * Snapshot of data the Today screen needs to render.
 *
 * Most time-sensitive values are pre-formatted by the route layer. The active
 * sleep timer is the exception so only that card recomposes every second.
 */
data class TodayUiState(
    val babies: List<Baby> = emptyList(),
    val selectedBaby: Baby? = null,
    val feedingStatus: FeedingStatusUiState = FeedingStatusUiState(),
    val activeBottle: Bottle? = null,
    /** Human-readable countdown, e.g. "1h 24m", "45m", or use R.string.today_time_expired. */
    val countdownText: String = "",
    val isExpiringSoon: Boolean = false,
    val todaySummary: TodaySummary = TodaySummary(
        totalFeedCount = 0,
        totalAmountMl = 0.0,
        diaperCount = 0,
        sleepDurationMillis = 0L,
        activeSleepStartedAt = null,
    ),
    val showAds: Boolean = true,
    val nightModeEnabled: Boolean = false,
    val unitType: UnitType = UnitType.ML,
    val showRatingPrompt: Boolean = false,
)

data class FeedingStatusUiState(
    val babyName: String = "",
    val lastFeedTimeText: String? = null,
    val lastFeedAgoText: String? = null,
    val lastFeedAmountText: String? = null,
    val todayFeedCount: Int = 0,
    val todayAmountText: String = "",
    val nextFeedInMillis: Long? = null,
    val isNextFeedDue: Boolean = false,
)

// --------------------------------------------------------------------------
// Root composable
// --------------------------------------------------------------------------

/**
 * Today screen — the primary home tab.
 *
 * Stateless: all mutable state is owned by [TodayViewModel] and passed in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    state: TodayUiState,
    onSelectBaby: (Baby) -> Unit,
    onAddBaby: () -> Unit,
    onNewBottle: () -> Unit,
    onStartFeeding: (Bottle) -> Unit,
    onRefrigerate: (Bottle) -> Unit,
    onDiscard: (Bottle) -> Unit,
    onMarkFed: (Bottle) -> Unit,
    onBottleDetail: (Bottle) -> Unit,
    onQuickDiaper: () -> Unit,
    onQuickSleep: () -> Unit,
    onRatingPromptRate: () -> Unit,
    onRatingPromptMaybeLater: () -> Unit,
    onRatingPromptNoThanks: () -> Unit,
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-3940256099942544/6300978111", // AdMob test banner ID
) {
    var quickLogFeedback by remember { mutableStateOf<QuickLogFeedback?>(null) }
    var isQuickLogFeedbackVisible by remember { mutableStateOf(false) }
    val showFeedback: (QuickLogFeedbackType) -> Unit = { type ->
        quickLogFeedback = QuickLogFeedback(type = type, token = System.nanoTime())
        isQuickLogFeedbackVisible = true
    }

    LaunchedEffect(quickLogFeedback) {
        if (quickLogFeedback != null && !KEEP_QUICK_LOG_FEEDBACK_VISIBLE_FOR_DEBUG) {
            delay(2_400)
            isQuickLogFeedbackVisible = false
        }
    }

    LaunchedEffect(
        state.feedingStatus.isNextFeedDue,
        state.feedingStatus.lastFeedTimeText,
    ) {
        if (state.feedingStatus.isNextFeedDue) {
            showFeedback(QuickLogFeedbackType.NEXT_FEED_DUE)
        }
    }

    if (state.showRatingPrompt) {
        RatingPromptDialog(
            onRate = onRatingPromptRate,
            onMaybeLater = onRatingPromptMaybeLater,
            onNoThanks = onRatingPromptNoThanks,
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.today_title),
                            style = if (state.nightModeEnabled)
                                MaterialTheme.typography.headlineMedium
                            else
                                MaterialTheme.typography.headlineSmall,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
            bottomBar = {
                if (state.showAds) {
                    AdBannerView(
                        adUnitId = adUnitId,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                    )
                }
            },
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                BabySwitcher(
                    babies = state.babies,
                    selectedBaby = state.selectedBaby,
                    onSelectBaby = onSelectBaby,
                    onAddBaby = onAddBaby,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(16.dp))

                FeedingStatusCard(
                    state = state.feedingStatus,
                    nightModeEnabled = state.nightModeEnabled,
                    onNewBottle = onNewBottle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                if (state.activeBottle != null) {
                    Spacer(Modifier.height(16.dp))
                    SectionHeader(
                        title = stringResource(R.string.today_active_bottle_timer_title),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    ActiveBottleCard(
                        bottle = state.activeBottle,
                        countdownText = state.countdownText,
                        isExpiringSoon = state.isExpiringSoon,
                        unitType = state.unitType,
                        nightModeEnabled = state.nightModeEnabled,
                        onStartFeeding = onStartFeeding,
                        onRefrigerate = onRefrigerate,
                        onDiscard = onDiscard,
                        onMarkFed = onMarkFed,
                        onCardClick = onBottleDetail,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }

                if (state.todaySummary.activeSleepStartedAt != null) {
                    Spacer(Modifier.height(16.dp))
                    ActiveSleepCard(
                        startedAtMillis = state.todaySummary.activeSleepStartedAt,
                        nightModeEnabled = state.nightModeEnabled,
                        onStopSleep = {
                            onQuickSleep()
                            showFeedback(QuickLogFeedbackType.SLEEP_STOPPED)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }

                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    title = stringResource(R.string.today_quick_actions_title),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
                QuickLogRow(
                    nightModeEnabled = state.nightModeEnabled,
                    onQuickFeed = onNewBottle,
                    onQuickDiaper = {
                        onQuickDiaper()
                        showFeedback(QuickLogFeedbackType.DIAPER)
                    },
                    onQuickSleep = {
                        val wasSleepActive = state.todaySummary.activeSleepStartedAt != null
                        onQuickSleep()
                        showFeedback(
                            if (wasSleepActive)
                                QuickLogFeedbackType.SLEEP_STOPPED
                            else
                                QuickLogFeedbackType.SLEEP_STARTED,
                        )
                    },
                    isSleepActive = state.todaySummary.activeSleepStartedAt != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    title = stringResource(R.string.today_summary_section_title),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(8.dp))
                TodaySummarySection(
                    summary = state.todaySummary,
                    unitType = state.unitType,
                    nightModeEnabled = state.nightModeEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(80.dp))
                }
            }
        }

        PlayfulQuickLogFeedback(
            feedback = quickLogFeedback,
            visible = isQuickLogFeedbackVisible,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(1f)
                .padding(horizontal = 16.dp),
        )
    }
}

private data class QuickLogFeedback(
    val type: QuickLogFeedbackType,
    val token: Long,
)

private enum class QuickLogFeedbackType {
    DIAPER,
    SLEEP_STARTED,
    SLEEP_STOPPED,
    NEXT_FEED_DUE,
}

@Composable
private fun RatingPromptDialog(
    onRate: () -> Unit,
    onMaybeLater: () -> Unit,
    onNoThanks: () -> Unit,
) {
    NurtlinaDialog(
        onDismissRequest = onMaybeLater,
        title = stringResource(R.string.rating_prompt_title),
        icon = Icons.Default.Star,
    ) {
        Text(
            text = stringResource(R.string.rating_prompt_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRate,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.rating_prompt_rate))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onMaybeLater,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.rating_prompt_maybe_later))
        }
        TextButton(
            onClick = onNoThanks,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.rating_prompt_no_thanks))
        }
    }
}

@Composable
private fun PlayfulQuickLogFeedback(
    feedback: QuickLogFeedback?,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible && feedback != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier,
    ) {
        feedback?.let { visibleFeedback ->
            val title = when (visibleFeedback.type) {
                QuickLogFeedbackType.DIAPER -> stringResource(R.string.today_feedback_diaper_logged)
                QuickLogFeedbackType.SLEEP_STARTED -> stringResource(R.string.today_feedback_sleep_started)
                QuickLogFeedbackType.SLEEP_STOPPED -> stringResource(R.string.today_feedback_sleep_stopped)
                QuickLogFeedbackType.NEXT_FEED_DUE -> stringResource(R.string.today_feedback_next_feed_due)
            }
            val body = when (visibleFeedback.type) {
                QuickLogFeedbackType.DIAPER -> stringResource(R.string.today_feedback_diaper_body)
                QuickLogFeedbackType.SLEEP_STARTED -> stringResource(R.string.today_feedback_sleep_started_body)
                QuickLogFeedbackType.SLEEP_STOPPED -> stringResource(R.string.today_feedback_sleep_stopped_body)
                QuickLogFeedbackType.NEXT_FEED_DUE -> stringResource(R.string.today_feedback_next_feed_due_body)
            }
            val icon = when (visibleFeedback.type) {
                QuickLogFeedbackType.DIAPER -> Icons.Default.BabyChangingStation
                QuickLogFeedbackType.SLEEP_STARTED,
                QuickLogFeedbackType.SLEEP_STOPPED -> Icons.Default.Bedtime
                QuickLogFeedbackType.NEXT_FEED_DUE -> Icons.Default.LocalDrink
            }
            val containerColor = when (visibleFeedback.type) {
                QuickLogFeedbackType.NEXT_FEED_DUE -> MaterialTheme.colorScheme.primaryContainer
                QuickLogFeedbackType.DIAPER -> MaterialTheme.colorScheme.tertiaryContainer
                QuickLogFeedbackType.SLEEP_STARTED,
                QuickLogFeedbackType.SLEEP_STOPPED -> MaterialTheme.colorScheme.secondaryContainer
            }
            val contentColor = when (visibleFeedback.type) {
                QuickLogFeedbackType.NEXT_FEED_DUE -> MaterialTheme.colorScheme.onPrimaryContainer
                QuickLogFeedbackType.DIAPER -> MaterialTheme.colorScheme.onTertiaryContainer
                QuickLogFeedbackType.SLEEP_STARTED,
                QuickLogFeedbackType.SLEEP_STOPPED -> MaterialTheme.colorScheme.onSecondaryContainer
            }
            val cdFeedback = "$title. $body"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = cdFeedback },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = contentColor.copy(alpha = 0.12f),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = contentColor,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.82f),
                        )
                    }
                    PlayfulDots(color = contentColor)
                }
            }
        }
    }
}

@Composable
private fun PlayfulDots(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val dotSize = if (index == 1) 8.dp else 6.dp
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.32f)),
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.fillMaxWidth(),
    )
}

// --------------------------------------------------------------------------
// Baby switcher
// --------------------------------------------------------------------------

@Composable
private fun BabySwitcher(
    babies: List<Baby>,
    selectedBaby: Baby?,
    onSelectBaby: (Baby) -> Unit,
    onAddBaby: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(babies, key = { it.id }) { baby ->
            val isSelected = selectedBaby?.id == baby.id
            val cdSelect = stringResource(R.string.cd_select_baby, baby.name)
            AssistChip(
                onClick = { onSelectBaby(baby) },
                label = {
                    Text(
                        text = baby.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface,
                    labelColor = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.semantics { contentDescription = cdSelect },
            )
        }
        item {
            val cdAddBaby = stringResource(R.string.cd_add_baby)
            AssistChip(
                onClick = onAddBaby,
                label = { Text(stringResource(R.string.today_add_baby_pro)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
                modifier = Modifier.semantics { contentDescription = cdAddBaby },
            )
        }
    }
}

// --------------------------------------------------------------------------
// Feeding status card
// --------------------------------------------------------------------------

@Composable
private fun FeedingStatusCard(
    state: FeedingStatusUiState,
    nightModeEnabled: Boolean,
    onNewBottle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.babyName.ifBlank { stringResource(R.string.today_no_baby_selected) },
                    style = if (nightModeEnabled)
                        MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    else
                        MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                val cdNewBottle = stringResource(R.string.cd_new_bottle_fab)
                FilledTonalButton(
                    onClick = onNewBottle,
                    modifier = Modifier
                        .height(if (nightModeEnabled) 48.dp else 40.dp)
                        .semantics { contentDescription = cdNewBottle },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.today_new_bottle),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            FeedingStatusBlock(
                label = stringResource(R.string.today_last_feed_label),
                value = if (state.lastFeedAgoText != null) {
                    val amount = state.lastFeedAmountText
                        ?: stringResource(R.string.today_feed_amount_not_recorded)
                    stringResource(R.string.today_last_feed_value, state.lastFeedAgoText, amount)
                } else {
                    stringResource(R.string.today_no_feed_logged)
                },
                detail = state.lastFeedTimeText?.let {
                    stringResource(R.string.today_last_feed_time, it)
                },
            )

            Spacer(Modifier.height(16.dp))

            FeedingStatusBlock(
                label = stringResource(R.string.today_feed_today_label),
                value = stringResource(
                    R.string.today_feed_today_value,
                    state.todayFeedCount,
                    state.todayAmountText,
                ),
            )

            Spacer(Modifier.height(16.dp))

            FeedingStatusBlock(
                label = stringResource(R.string.today_next_feed_label),
                value = when {
                    state.isNextFeedDue -> stringResource(R.string.today_next_feed_now)
                    state.nextFeedInMillis != null -> stringResource(
                        R.string.today_next_feed_watch_later,
                        state.nextFeedInMillis.formatNextFeedCountdown(),
                    )
                    else -> stringResource(R.string.today_next_feed_after_first)
                },
            )
        }
    }
}

@Composable
private fun FeedingStatusBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        if (detail != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f),
            )
        }
    }
}

// --------------------------------------------------------------------------
// Active sleep card
// --------------------------------------------------------------------------

@Composable
private fun ActiveSleepCard(
    startedAtMillis: Long,
    nightModeEnabled: Boolean,
    onStopSleep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var elapsedText by remember(startedAtMillis) {
        mutableStateOf(formatSleepElapsedClock(startedAtMillis))
    }

    LaunchedEffect(startedAtMillis) {
        while (true) {
            elapsedText = formatSleepElapsedClock(startedAtMillis)
            delay(1_000L)
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f),
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(28.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.today_sleep_active_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.today_sleep_elapsed_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.76f),
                )
                Text(
                    text = elapsedText,
                    style = if (nightModeEnabled)
                        MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                    else
                        MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(R.string.today_sleep_stop_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.76f),
                )
            }
            Spacer(Modifier.width(12.dp))
            FilledTonalButton(
                onClick = onStopSleep,
                modifier = Modifier.height(if (nightModeEnabled) 52.dp else 44.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.today_stop_sleep),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun formatSleepElapsedClock(startedAtMillis: Long): String {
    val elapsedMillis = (System.currentTimeMillis() - startedAtMillis).coerceAtLeast(0L)
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

// --------------------------------------------------------------------------
// Active bottle card
// --------------------------------------------------------------------------

@Composable
private fun ActiveBottleCard(
    bottle: Bottle,
    countdownText: String,
    isExpiringSoon: Boolean,
    unitType: UnitType,
    nightModeEnabled: Boolean,
    onStartFeeding: (Bottle) -> Unit,
    onRefrigerate: (Bottle) -> Unit,
    onDiscard: (Bottle) -> Unit,
    onMarkFed: (Bottle) -> Unit,
    onCardClick: (Bottle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColors = BottleStatusColors.colorsFor(bottle.status)
    val cdStatus = stringResource(R.string.cd_bottle_status, bottle.status.name)
    val cdCountdown = stringResource(R.string.cd_countdown_timer, countdownText)

    ElevatedCard(
        onClick = { onCardClick(bottle) },
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = statusColors.containerColor,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = bottle.milkType.icon(),
                    contentDescription = null,
                    tint = statusColors.contentColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = bottle.milkType.label(),
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColors.contentColor,
                )
                Spacer(Modifier.weight(1f))
                StatusBadge(
                    status = bottle.status,
                    modifier = Modifier.semantics { contentDescription = cdStatus },
                )
            }

            Spacer(Modifier.height(16.dp))

            if (bottle.status == BottleStatus.EXPIRED) {
                Text(
                    text = stringResource(R.string.today_time_expired),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColors.contentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = cdCountdown },
                )
            } else if (!bottle.status.isTerminal) {
                Column {
                    Text(
                        text = stringResource(R.string.today_expires_in),
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColors.contentColor.copy(alpha = 0.7f),
                    )
                    Text(
                        text = countdownText,
                        style = if (nightModeEnabled)
                            MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
                        else
                            MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = statusColors.contentColor,
                        modifier = Modifier.semantics { contentDescription = cdCountdown },
                    )
                }
            }

            bottle.amountMl?.let { ml ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = ml.formatAmount(unitType),
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColors.contentColor.copy(alpha = 0.8f),
                )
            }

            if (!bottle.status.isTerminal) {
                Spacer(Modifier.height(16.dp))
                BottleActionButtons(
                    status = bottle.status,
                    nightModeEnabled = nightModeEnabled,
                    onStartFeeding = { onStartFeeding(bottle) },
                    onRefrigerate = { onRefrigerate(bottle) },
                    onDiscard = { onDiscard(bottle) },
                    onMarkFed = { onMarkFed(bottle) },
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: BottleStatus,
    modifier: Modifier = Modifier,
) {
    val statusColors = BottleStatusColors.colorsFor(status)
    Surface(
        shape = MaterialTheme.shapes.small,
        color = statusColors.contentColor.copy(alpha = 0.15f),
        modifier = modifier,
    ) {
        Text(
            text = status.label(),
            style = MaterialTheme.typography.labelSmall,
            color = statusColors.contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun BottleActionButtons(
    status: BottleStatus,
    nightModeEnabled: Boolean,
    onStartFeeding: () -> Unit,
    onRefrigerate: () -> Unit,
    onDiscard: () -> Unit,
    onMarkFed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val btnHeight = if (nightModeEnabled) 52.dp else 44.dp

    when (status) {
        BottleStatus.NOT_STARTED -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = onStartFeeding,
                    modifier = Modifier
                        .weight(1f)
                        .height(btnHeight),
                ) {
                    Text(
                        text = stringResource(R.string.action_start_feeding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(
                    onClick = onRefrigerate,
                    modifier = Modifier
                        .weight(1f)
                        .height(btnHeight),
                ) {
                    Text(
                        text = stringResource(R.string.action_refrigerate),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(btnHeight),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.action_discard))
            }
        }
        BottleStatus.REFRIGERATED -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = onStartFeeding,
                    modifier = Modifier
                        .weight(1f)
                        .height(btnHeight),
                ) {
                    Text(
                        text = stringResource(R.string.action_start_feeding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(
                    onClick = onDiscard,
                    modifier = Modifier
                        .weight(1f)
                        .height(btnHeight),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.action_discard),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        BottleStatus.FEEDING_STARTED -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = onMarkFed,
                    modifier = Modifier
                        .weight(1f)
                        .height(btnHeight),
                ) {
                    Text(
                        text = stringResource(R.string.action_mark_fed),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(
                    onClick = onDiscard,
                    modifier = Modifier
                        .weight(1f)
                        .height(btnHeight),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.action_discard),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        BottleStatus.EXPIRED -> {
            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(btnHeight),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.action_discard))
            }
        }
        else -> Unit
    }
}

// --------------------------------------------------------------------------
// Empty state card
// --------------------------------------------------------------------------

@Composable
private fun EmptyBottleCard(
    onNewBottle: () -> Unit,
    nightModeEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.LocalDrink,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.today_no_active_bottle),
                style = if (nightModeEnabled)
                    MaterialTheme.typography.titleLarge
                else
                    MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.today_no_active_bottle_cta),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            FilledTonalButton(
                onClick = onNewBottle,
                modifier = Modifier.height(if (nightModeEnabled) 52.dp else 44.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.today_new_bottle))
            }
        }
    }
}

// --------------------------------------------------------------------------
// Quick log row
// --------------------------------------------------------------------------

@Composable
private fun QuickLogRow(
    nightModeEnabled: Boolean,
    onQuickFeed: () -> Unit,
    onQuickDiaper: () -> Unit,
    onQuickSleep: () -> Unit,
    isSleepActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val btnHeight = if (nightModeEnabled) 56.dp else 48.dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickLogButton(
            label = stringResource(R.string.today_quick_feed),
            icon = Icons.Default.LocalDrink,
            onClick = onQuickFeed,
            modifier = Modifier
                .weight(1f)
                .height(btnHeight),
        )
        QuickLogButton(
            label = stringResource(R.string.today_quick_diaper),
            icon = Icons.Default.BabyChangingStation,
            onClick = onQuickDiaper,
            modifier = Modifier
                .weight(1f)
                .height(btnHeight),
        )
        QuickLogButton(
            label = if (isSleepActive)
                stringResource(R.string.today_stop_sleep)
            else
                stringResource(R.string.today_quick_sleep),
            icon = Icons.Default.Bedtime,
            onClick = onQuickSleep,
            modifier = Modifier
                .weight(1f)
                .height(btnHeight),
        )
    }
}

@Composable
private fun QuickLogButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionContentColor = MaterialTheme.colorScheme.primary
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = actionContentColor.copy(alpha = 0.72f),
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = actionContentColor,
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// --------------------------------------------------------------------------
// Today summary
// --------------------------------------------------------------------------

@Composable
private fun TodaySummarySection(
    summary: TodaySummary,
    unitType: UnitType,
    nightModeEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val feedAmountText = summary.totalAmountMl.formatAmount(unitType)
    val sleepText = summary.sleepDurationMillis.formatDuration()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SummaryCard(
            title = stringResource(R.string.today_summary_feeds),
            value = summary.totalFeedCount.toString(),
            subtitle = feedAmountText,
            nightModeEnabled = nightModeEnabled,
            modifier = Modifier.weight(1f),
        )
        SummaryCard(
            title = stringResource(R.string.today_summary_diapers),
            value = summary.diaperCount.toString(),
            nightModeEnabled = nightModeEnabled,
            modifier = Modifier.weight(1f),
        )
        SummaryCard(
            title = stringResource(R.string.today_summary_sleep),
            value = if (summary.activeSleepStartedAt != null)
                stringResource(R.string.today_active_sleep)
            else
                sleepText,
            nightModeEnabled = nightModeEnabled,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    nightModeEnabled: Boolean = false,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = if (nightModeEnabled)
                    MaterialTheme.typography.titleLarge
                else
                    MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// --------------------------------------------------------------------------
// AdMob banner
// --------------------------------------------------------------------------

@Composable
fun AdBannerView(
    adUnitId: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}

// --------------------------------------------------------------------------
// Extension helpers (UI layer only — formatting for display)
// --------------------------------------------------------------------------

@Composable
private fun MilkType.label(): String = when (this) {
    MilkType.FORMULA -> stringResource(R.string.milk_type_formula)
    MilkType.BREAST_MILK -> stringResource(R.string.milk_type_breast_milk)
    MilkType.CUSTOM -> stringResource(R.string.milk_type_custom)
}

private fun MilkType.icon(): ImageVector = when (this) {
    MilkType.FORMULA -> Icons.Default.LocalDrink
    MilkType.BREAST_MILK -> Icons.Default.WaterDrop
    MilkType.CUSTOM -> Icons.Default.LocalDrink
}

@Composable
private fun BottleStatus.label(): String = when (this) {
    BottleStatus.NOT_STARTED -> stringResource(R.string.bottle_status_not_started)
    BottleStatus.FEEDING_STARTED -> stringResource(R.string.bottle_status_feeding_started)
    BottleStatus.REFRIGERATED -> stringResource(R.string.bottle_status_refrigerated)
    BottleStatus.EXPIRED -> stringResource(R.string.bottle_status_expired)
    BottleStatus.FED -> stringResource(R.string.bottle_status_fed)
    BottleStatus.DISCARDED -> stringResource(R.string.bottle_status_discarded)
    BottleStatus.CANCELED -> stringResource(R.string.bottle_status_canceled)
}

internal fun Double.formatAmount(unit: UnitType): String = when (unit) {
    UnitType.ML -> "%.0f ml".format(this)
    UnitType.OZ -> "%.1f oz".format(this / ML_PER_OUNCE)
}

private fun Long.formatNextFeedCountdown(): String {
    if (this <= 0) return "0s"
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this)
    if (seconds <= 60) return "${seconds}s"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this)
    return if (minutes < 60) "${minutes}m" else formatDuration()
}

internal fun Long.formatDuration(): String {
    if (this <= 0) return "0m"
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

// --------------------------------------------------------------------------
// Previews
// --------------------------------------------------------------------------

private fun previewBaby() = Baby(
    id = "1",
    name = "Sunshine",
    birthDate = null,
    avatarColor = "TEAL",
    createdAt = Instant.now(),
    updatedAt = Instant.now(),
    archivedAt = null,
)

private fun previewFeedingStatus(
    lastFeedAgoText: String? = "2h 10m",
    lastFeedAmountText: String? = "120 ml",
    lastFeedTimeText: String? = "8:20 AM",
    nextFeedInMillis: Long? = 30 * 60 * 1000L,
) = FeedingStatusUiState(
    babyName = "Sunshine",
    lastFeedTimeText = lastFeedTimeText,
    lastFeedAgoText = lastFeedAgoText,
    lastFeedAmountText = lastFeedAmountText,
    todayFeedCount = 5,
    todayAmountText = "480 ml",
    nextFeedInMillis = nextFeedInMillis,
)

private fun previewBottle(status: BottleStatus = BottleStatus.NOT_STARTED) = Bottle(
    id = "b1",
    babyId = "1",
    milkType = MilkType.FORMULA,
    amountMl = 120.0,
    preparedAt = Instant.now(),
    feedingStartedAt = null,
    refrigeratedAt = null,
    status = status,
    guidelineRegion = com.nurtlina.app.domain.model.GuidelineRegion.US,
    expiresAt = Instant.now().plusSeconds(5400),
    discardedAt = null,
    fedAt = null,
    note = null,
    createdAt = Instant.now(),
    updatedAt = Instant.now(),
)

@Preview(name = "Today – with active bottle (Light)", showBackground = true, showSystemUi = true)
@Composable
private fun PreviewTodayWithBottle() {
    NurtlinaTheme {
        TodayScreen(
            state = TodayUiState(
                babies = listOf(previewBaby()),
                selectedBaby = previewBaby(),
                feedingStatus = previewFeedingStatus(),
                activeBottle = previewBottle(),
                countdownText = "1h 30m",
                todaySummary = TodaySummary(3, 360.0, 5, 12_600_000L, null),
                showAds = false,
            ),
            onSelectBaby = {},
            onAddBaby = {},
            onNewBottle = {},
            onStartFeeding = {},
            onRefrigerate = {},
            onDiscard = {},
            onMarkFed = {},
            onBottleDetail = {},
            onQuickDiaper = {},
            onQuickSleep = {},
            onRatingPromptRate = {},
            onRatingPromptMaybeLater = {},
            onRatingPromptNoThanks = {},
        )
    }
}

@Preview(name = "Today – empty state (Dark)", showBackground = true, showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewTodayEmptyDark() {
    NurtlinaTheme(darkTheme = true) {
        TodayScreen(
            state = TodayUiState(
                babies = listOf(previewBaby()),
                selectedBaby = previewBaby(),
                feedingStatus = previewFeedingStatus(
                    lastFeedAgoText = null,
                    lastFeedAmountText = null,
                    lastFeedTimeText = null,
                    nextFeedInMillis = null,
                ),
                showAds = false,
            ),
            onSelectBaby = {},
            onAddBaby = {},
            onNewBottle = {},
            onStartFeeding = {},
            onRefrigerate = {},
            onDiscard = {},
            onMarkFed = {},
            onBottleDetail = {},
            onQuickDiaper = {},
            onQuickSleep = {},
            onRatingPromptRate = {},
            onRatingPromptMaybeLater = {},
            onRatingPromptNoThanks = {},
        )
    }
}

@Preview(name = "Today – Night Mode", showBackground = true, showSystemUi = true)
@Composable
private fun PreviewTodayNightMode() {
    NurtlinaTheme(darkTheme = true) {
        TodayScreen(
            state = TodayUiState(
                babies = listOf(previewBaby()),
                selectedBaby = previewBaby(),
                feedingStatus = previewFeedingStatus(),
                activeBottle = previewBottle(BottleStatus.FEEDING_STARTED),
                countdownText = "45m",
                todaySummary = TodaySummary(3, 360.0, 5, 12_600_000L, Instant.now().toEpochMilli()),
                nightModeEnabled = true,
                showAds = false,
            ),
            onSelectBaby = {},
            onAddBaby = {},
            onNewBottle = {},
            onStartFeeding = {},
            onRefrigerate = {},
            onDiscard = {},
            onMarkFed = {},
            onBottleDetail = {},
            onQuickDiaper = {},
            onQuickSleep = {},
            onRatingPromptRate = {},
            onRatingPromptMaybeLater = {},
            onRatingPromptNoThanks = {},
        )
    }
}
