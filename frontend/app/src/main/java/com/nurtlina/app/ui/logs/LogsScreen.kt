package com.nurtlina.app.ui.logs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nurtlina.app.R
import com.nurtlina.app.domain.model.*
import com.nurtlina.app.ui.theme.NurtlinaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// ---------------------------------------------------------------------------
// Public data types used by callers
// ---------------------------------------------------------------------------

// LogFilter is defined in LogsViewModel.kt (same package): ALL, BOTTLE, FEED, DIAPER, SLEEP

/** Sealed union of every loggable entry type shown in the timeline. */
sealed interface LogEntry {
    val id: String
    val timestampInstant: Instant

    data class Feed(val log: FeedLog) : LogEntry {
        override val id get() = log.id
        override val timestampInstant get() = log.startedAt
    }

    data class Diaper(val log: DiaperLog) : LogEntry {
        override val id get() = log.id
        override val timestampInstant get() = log.changedAt
    }

    data class Sleep(val log: SleepLog) : LogEntry {
        override val id get() = log.id
        override val timestampInstant get() = log.startedAt
    }

    data class Bottle(val bottle: com.nurtlina.app.domain.model.Bottle) : LogEntry {
        override val id get() = bottle.id
        override val timestampInstant get() = bottle.preparedAt
    }
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

/**
 * Stateless Logs screen. All state is hoisted to the caller (ViewModel).
 *
 * @param selectedDate      The date currently shown.
 * @param entries           All log entries for [selectedDate], sorted newest-first.
 * @param activeFilter      Currently selected [LogFilter].
 * @param useOz             Whether to display amounts in oz (false = ml).
 * @param onPrevDay         Navigate to the previous day.
 * @param onNextDay         Navigate to the next day.
 * @param onPickDate        Open a date-picker dialog.
 * @param onFilterSelected  User tapped a filter chip.
 * @param onEntryClick      User tapped a log entry (open edit sheet).
 * @param onEntryDelete     User confirmed deleting a log entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    selectedDate: LocalDate,
    entries: List<LogEntry>,
    activeFilter: LogFilter,
    useOz: Boolean,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit,
    onFilterSelected: (LogFilter) -> Unit,
    onEntryClick: (LogEntry) -> Unit,
    onEntryDelete: (LogEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val filteredEntries = remember(entries, activeFilter) {
        when (activeFilter) {
            LogFilter.ALL -> entries
            LogFilter.BOTTLE -> entries.filterIsInstance<LogEntry.Bottle>()
            LogFilter.FEED -> entries.filterIsInstance<LogEntry.Feed>()
            LogFilter.DIAPER -> entries.filterIsInstance<LogEntry.Diaper>()
            LogFilter.SLEEP -> entries.filterIsInstance<LogEntry.Sleep>()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.logs_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ---- Date navigation bar ----
            DateNavigationBar(
                selectedDate = selectedDate,
                today = today,
                onPrevDay = onPrevDay,
                onNextDay = onNextDay,
                onPickDate = onPickDate,
            )

            // ---- Filter chips ----
            FilterChipRow(
                activeFilter = activeFilter,
                onFilterSelected = onFilterSelected,
            )

            HorizontalDivider()

            // ---- Log list or empty state ----
            if (filteredEntries.isEmpty()) {
                LogsEmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LogTimelineList(
                    entries = filteredEntries,
                    useOz = useOz,
                    onEntryClick = onEntryClick,
                    onEntryDelete = onEntryDelete,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Date navigation bar
// ---------------------------------------------------------------------------

@Composable
private fun DateNavigationBar(
    selectedDate: LocalDate,
    today: LocalDate,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit,
) {
    val dateLabel = when (selectedDate) {
        today -> stringResource(R.string.logs_today)
        today.minusDays(1) -> stringResource(R.string.logs_yesterday)
        else -> selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }

    val prevDayDesc = stringResource(R.string.logs_prev_day_cd)
    val nextDayDesc = stringResource(R.string.logs_next_day_cd)
    val pickDateDesc = stringResource(R.string.logs_pick_date_cd)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPrevDay,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = prevDayDesc },
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = null)
        }

        TextButton(
            onClick = onPickDate,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = pickDateDesc },
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        IconButton(
            onClick = onNextDay,
            enabled = selectedDate < today,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = nextDayDesc },
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

// ---------------------------------------------------------------------------
// Filter chip row
// ---------------------------------------------------------------------------

@Composable
private fun FilterChipRow(
    activeFilter: LogFilter,
    onFilterSelected: (LogFilter) -> Unit,
) {
    val filters = remember {
        listOf(
            LogFilter.ALL to R.string.logs_filter_all,
            LogFilter.BOTTLE to R.string.logs_filter_bottles,
            LogFilter.FEED to R.string.logs_filter_feeds,
            LogFilter.DIAPER to R.string.logs_filter_diapers,
            LogFilter.SLEEP to R.string.logs_filter_sleep,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { (filter, labelRes) ->
            FilterChip(
                selected = activeFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(stringResource(labelRes)) },
                modifier = Modifier.height(36.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Timeline list
// ---------------------------------------------------------------------------

@Composable
private fun LogTimelineList(
    entries: List<LogEntry>,
    useOz: Boolean,
    onEntryClick: (LogEntry) -> Unit,
    onEntryDelete: (LogEntry) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<LogEntry?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(
            items = entries,
            key = { it.id },
        ) { entry ->
            var visible by remember { mutableStateOf(true) }

            AnimatedVisibility(
                visible = visible,
                exit = shrinkVertically(tween(300)) + fadeOut(tween(300)),
            ) {
                LogEntryCard(
                    entry = entry,
                    useOz = useOz,
                    onClick = { onEntryClick(entry) },
                    onDelete = { pendingDelete = entry },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }

    // Confirmation dialog
    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.logs_delete_dialog_title)) },
            text = { Text(stringResource(R.string.logs_delete_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onEntryDelete(entry)
                    pendingDelete = null
                }) {
                    Text(
                        text = stringResource(R.string.logs_confirm_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Log entry card
// ---------------------------------------------------------------------------

@Composable
private fun LogEntryCard(
    entry: LogEntry,
    useOz: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val zone = ZoneId.systemDefault()

    val timeLabel = remember(entry.timestampInstant) {
        entry.timestampInstant.atZone(zone).format(timeFormatter)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = timeLabel
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Type icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = entry.typeIcon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.primaryLabel(useOz),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                entry.secondaryLabel(useOz)?.let { secondary ->
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// LogEntry display helpers
// ---------------------------------------------------------------------------

@Composable
private fun LogEntry.typeIcon() = when (this) {
    is LogEntry.Feed -> when (log.feedType) {
        FeedType.NURSING -> Icons.Outlined.ChildCare
        else -> Icons.Outlined.LocalDrink
    }
    is LogEntry.Diaper -> Icons.Outlined.BabyChangingStation
    is LogEntry.Sleep -> Icons.Outlined.Bedtime
    is LogEntry.Bottle -> Icons.Outlined.Science
}

@Composable
private fun LogEntry.primaryLabel(useOz: Boolean): String = when (this) {
    is LogEntry.Feed -> when (log.feedType) {
        FeedType.FORMULA -> stringResource(R.string.log_feed_type_formula)
        FeedType.BREAST_MILK -> stringResource(R.string.log_feed_type_breast_milk)
        FeedType.MIXED -> stringResource(R.string.log_feed_type_mixed)
        FeedType.NURSING -> stringResource(R.string.log_feed_type_nursing)
        FeedType.OTHER -> stringResource(R.string.log_feed_type_other)
    }
    is LogEntry.Diaper -> when (log.diaperType) {
        DiaperType.WET -> stringResource(R.string.log_diaper_type_wet)
        DiaperType.DIRTY -> stringResource(R.string.log_diaper_type_dirty)
        DiaperType.MIXED -> stringResource(R.string.log_diaper_type_mixed)
        DiaperType.DRY -> stringResource(R.string.log_diaper_type_dry)
    }
    is LogEntry.Sleep -> if (log.isActive) {
        stringResource(R.string.log_sleep_active)
    } else {
        val millis = log.durationMillis() ?: 0L
        val h = millis / 3_600_000
        val m = (millis % 3_600_000) / 60_000
        if (h > 0) "%dh %02dm".format(h, m) else "%dm".format(m)
    }
    is LogEntry.Bottle -> when (bottle.status) {
        BottleStatus.NOT_STARTED -> stringResource(R.string.log_bottle_status_not_started)
        BottleStatus.FEEDING_STARTED -> stringResource(R.string.log_bottle_status_feeding_started)
        BottleStatus.REFRIGERATED -> stringResource(R.string.log_bottle_status_refrigerated)
        BottleStatus.EXPIRED -> stringResource(R.string.log_bottle_status_expired)
        BottleStatus.FED -> stringResource(R.string.log_bottle_status_fed)
        BottleStatus.DISCARDED -> stringResource(R.string.log_bottle_status_discarded)
        BottleStatus.CANCELED -> stringResource(R.string.log_bottle_status_canceled)
    }
}

@Composable
private fun LogEntry.secondaryLabel(useOz: Boolean): String? = when (this) {
    is LogEntry.Feed -> log.amountMl?.let { ml ->
        if (useOz) "%.1f oz".format(ml / 29.5735) else "%.0f ml".format(ml)
    }
    is LogEntry.Sleep -> null
    is LogEntry.Diaper -> null
    is LogEntry.Bottle -> null
}

// ---------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------

@Composable
private fun LogsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.EventNote,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.logs_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.logs_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Logs – light", showBackground = true)
@Composable
private fun LogsScreenLightPreview() {
    NurtlinaTheme {
        val now = Instant.now()
        val babyId = "b1"
        LogsScreen(
            selectedDate = LocalDate.now(),
            entries = listOf(
                LogEntry.Feed(
                    FeedLog(
                        id = "f1", babyId = babyId, bottleId = null,
                        feedType = FeedType.FORMULA, amountMl = 120.0,
                        startedAt = now.minusSeconds(3600), endedAt = now.minusSeconds(3300),
                        side = null, note = null,
                        createdAt = now, updatedAt = now,
                    ),
                ),
                LogEntry.Diaper(
                    DiaperLog(
                        id = "d1", babyId = babyId, diaperType = DiaperType.WET,
                        changedAt = now.minusSeconds(7200), note = null,
                        createdAt = now, updatedAt = now,
                    ),
                ),
                LogEntry.Sleep(
                    SleepLog(
                        id = "s1", babyId = babyId,
                        startedAt = now.minusSeconds(5400), endedAt = now.minusSeconds(1800),
                        note = null, createdAt = now, updatedAt = now,
                    ),
                ),
            ),
            activeFilter = LogFilter.ALL,
            useOz = false,
            onPrevDay = {}, onNextDay = {}, onPickDate = {},
            onFilterSelected = {}, onEntryClick = {}, onEntryDelete = {},
        )
    }
}

@Preview(name = "Logs – empty state", showBackground = true)
@Composable
private fun LogsScreenEmptyPreview() {
    NurtlinaTheme {
        LogsScreen(
            selectedDate = LocalDate.now(),
            entries = emptyList(),
            activeFilter = LogFilter.ALL,
            useOz = false,
            onPrevDay = {}, onNextDay = {}, onPickDate = {},
            onFilterSelected = {}, onEntryClick = {}, onEntryDelete = {},
        )
    }
}

@Preview(name = "Logs – dark", showBackground = true)
@Composable
private fun LogsScreenDarkPreview() {
    NurtlinaTheme(darkTheme = true) {
        LogsScreen(
            selectedDate = LocalDate.now(),
            entries = emptyList(),
            activeFilter = LogFilter.FEED,
            useOz = true,
            onPrevDay = {}, onNextDay = {}, onPickDate = {},
            onFilterSelected = {}, onEntryClick = {}, onEntryDelete = {},
        )
    }
}
