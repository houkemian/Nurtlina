package com.nurtlina.app.ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nurtlina.app.R
import com.nurtlina.app.domain.model.*
import com.nurtlina.app.ui.theme.NurtlinaTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// ---------------------------------------------------------------------------
// Edit state types
// ---------------------------------------------------------------------------

/** Editable draft state for a feed log. */
data class FeedEditState(
    val feedType: FeedType,
    val amountMl: String,
    val nursingSide: NursingSide?,
    val time: Instant,
    val note: String,
)

/** Editable draft state for a diaper log. */
data class DiaperEditState(
    val diaperType: DiaperType,
    val time: Instant,
    val note: String,
)

/** Editable draft state for a sleep log. */
data class SleepEditState(
    val startedAt: Instant,
    val endedAt: Instant?,
    val note: String,
)

/** Sealed union passed into the sheet to indicate which log type is being edited. */
sealed interface LogEditTarget {
    data class Feed(val original: FeedLog, val draft: FeedEditState) : LogEditTarget
    data class Diaper(val original: DiaperLog, val draft: DiaperEditState) : LogEditTarget
    data class Sleep(val original: SleepLog, val draft: SleepEditState) : LogEditTarget
}

// ---------------------------------------------------------------------------
// Sheet
// ---------------------------------------------------------------------------

/**
 * Bottom sheet for editing any log entry type.
 *
 * Stateless: all mutable draft state is owned by the caller.
 *
 * @param target          The log being edited and its current draft state.
 * @param useOz           Display amount field in oz (false = ml).
 * @param onSave          User confirmed edits; receives the updated [LogEditTarget].
 * @param onDelete        User confirmed deletion of this entry.
 * @param onDismiss       User dismissed the sheet without saving.
 * @param onFeedDraftChanged  Feed draft fields changed (only relevant when target is Feed).
 * @param onDiaperDraftChanged Diaper draft fields changed.
 * @param onSleepDraftChanged Sleep draft fields changed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogEditSheet(
    target: LogEditTarget,
    useOz: Boolean,
    onSave: (LogEditTarget) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onFeedDraftChanged: (FeedEditState) -> Unit = {},
    onDiaperDraftChanged: (DiaperEditState) -> Unit = {},
    onSleepDraftChanged: (SleepEditState) -> Unit = {},
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Sheet title
            Text(
                text = when (target) {
                    is LogEditTarget.Feed -> stringResource(R.string.log_edit_title_feed)
                    is LogEditTarget.Diaper -> stringResource(R.string.log_edit_title_diaper)
                    is LogEditTarget.Sleep -> stringResource(R.string.log_edit_title_sleep)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            HorizontalDivider()

            // Type-specific fields
            when (target) {
                is LogEditTarget.Feed -> FeedEditFields(
                    draft = target.draft,
                    useOz = useOz,
                    onChanged = onFeedDraftChanged,
                )
                is LogEditTarget.Diaper -> DiaperEditFields(
                    draft = target.draft,
                    onChanged = onDiaperDraftChanged,
                )
                is LogEditTarget.Sleep -> SleepEditFields(
                    draft = target.draft,
                    onChanged = onSleepDraftChanged,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Save button
            Button(
                onClick = { onSave(target) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = stringResource(R.string.common_save),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Delete button
            OutlinedButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.log_edit_delete_entry),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.logs_delete_dialog_title)) },
            text = { Text(stringResource(R.string.logs_delete_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text(
                        text = stringResource(R.string.logs_confirm_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Feed edit fields
// ---------------------------------------------------------------------------

@Composable
private fun FeedEditFields(
    draft: FeedEditState,
    useOz: Boolean,
    onChanged: (FeedEditState) -> Unit,
) {
    // Feed type picker
    LabeledSection(label = stringResource(R.string.log_edit_feed_type_label)) {
        FeedTypeSelector(
            selected = draft.feedType,
            onSelected = { onChanged(draft.copy(feedType = it)) },
        )
    }

    // Amount field (shown for non-nursing types)
    if (draft.feedType != FeedType.NURSING) {
        LabeledSection(label = stringResource(R.string.log_edit_amount_label)) {
            OutlinedTextField(
                value = draft.amountMl,
                onValueChange = { onChanged(draft.copy(amountMl = it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        if (useOz) stringResource(R.string.log_edit_amount_hint_oz)
                        else stringResource(R.string.log_edit_amount_hint_ml),
                    )
                },
                suffix = { Text(if (useOz) "oz" else "ml") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
    }

    // Nursing side picker
    if (draft.feedType == FeedType.NURSING) {
        LabeledSection(label = stringResource(R.string.log_edit_nursing_side_label)) {
            NursingSideSelector(
                selected = draft.nursingSide,
                onSelected = { onChanged(draft.copy(nursingSide = it)) },
            )
        }
    }

    // Time field
    TimePickerRow(
        label = stringResource(R.string.log_edit_time_label),
        instant = draft.time,
        onChanged = { onChanged(draft.copy(time = it)) },
    )

    // Note field
    NoteField(note = draft.note, onChanged = { onChanged(draft.copy(note = it)) })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedTypeSelector(selected: FeedType, onSelected: (FeedType) -> Unit) {
    val types = listOf(
        FeedType.FORMULA to R.string.log_feed_type_formula,
        FeedType.BREAST_MILK to R.string.log_feed_type_breast_milk,
        FeedType.MIXED to R.string.log_feed_type_mixed,
        FeedType.NURSING to R.string.log_feed_type_nursing,
        FeedType.OTHER to R.string.log_feed_type_other,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        types.forEach { (type, labelRes) ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelected(type) },
                label = { Text(stringResource(labelRes)) },
            )
        }
    }
}

@Composable
private fun NursingSideSelector(selected: NursingSide?, onSelected: (NursingSide) -> Unit) {
    val sides = listOf(
        NursingSide.LEFT to R.string.log_feed_nursing_left,
        NursingSide.RIGHT to R.string.log_feed_nursing_right,
        NursingSide.BOTH to R.string.log_feed_nursing_both,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        sides.forEach { (side, labelRes) ->
            FilterChip(
                selected = selected == side,
                onClick = { onSelected(side) },
                label = { Text(stringResource(labelRes)) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Diaper edit fields
// ---------------------------------------------------------------------------

@Composable
private fun DiaperEditFields(
    draft: DiaperEditState,
    onChanged: (DiaperEditState) -> Unit,
) {
    LabeledSection(label = stringResource(R.string.log_edit_diaper_type_label)) {
        DiaperTypeSelector(
            selected = draft.diaperType,
            onSelected = { onChanged(draft.copy(diaperType = it)) },
        )
    }

    TimePickerRow(
        label = stringResource(R.string.log_edit_time_label),
        instant = draft.time,
        onChanged = { onChanged(draft.copy(time = it)) },
    )

    NoteField(note = draft.note, onChanged = { onChanged(draft.copy(note = it)) })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiaperTypeSelector(selected: DiaperType, onSelected: (DiaperType) -> Unit) {
    val types = listOf(
        DiaperType.WET to R.string.log_diaper_type_wet,
        DiaperType.DIRTY to R.string.log_diaper_type_dirty,
        DiaperType.MIXED to R.string.log_diaper_type_mixed,
        DiaperType.DRY to R.string.log_diaper_type_dry,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        types.forEach { (type, labelRes) ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelected(type) },
                label = { Text(stringResource(labelRes)) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Sleep edit fields
// ---------------------------------------------------------------------------

@Composable
private fun SleepEditFields(
    draft: SleepEditState,
    onChanged: (SleepEditState) -> Unit,
) {
    TimePickerRow(
        label = stringResource(R.string.log_edit_sleep_start_label),
        instant = draft.startedAt,
        onChanged = { onChanged(draft.copy(startedAt = it)) },
    )

    // End time row with "still sleeping" toggle
    LabeledSection(label = stringResource(R.string.log_edit_sleep_end_label)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val hasEnd = draft.endedAt != null
            Checkbox(
                checked = !hasEnd,
                onCheckedChange = { stillSleeping ->
                    onChanged(draft.copy(endedAt = if (stillSleeping) null else Instant.now()))
                },
            )
            Text(
                text = stringResource(R.string.log_edit_sleep_end_active),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        if (draft.endedAt != null) {
            InstantTimeDisplay(
                instant = draft.endedAt,
                onChanged = { onChanged(draft.copy(endedAt = it)) },
            )
        }
    }

    NoteField(note = draft.note, onChanged = { onChanged(draft.copy(note = it)) })
}

// ---------------------------------------------------------------------------
// Shared UI helpers
// ---------------------------------------------------------------------------

@Composable
private fun LabeledSection(
    label: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun NoteField(note: String, onChanged: (String) -> Unit) {
    OutlinedTextField(
        value = note,
        onValueChange = onChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.common_note_hint)) },
        maxLines = 3,
        leadingIcon = {
            Icon(Icons.Outlined.Notes, contentDescription = null)
        },
    )
}

/**
 * Displays an [Instant] as a readable time string and calls [onChanged] when
 * the user would want to change it. In a real implementation wire this up to
 * a TimePickerDialog; here we show a read-only chip as a placeholder.
 */
@Composable
private fun TimePickerRow(
    label: String,
    instant: Instant,
    onChanged: (Instant) -> Unit,
) {
    LabeledSection(label = label) {
        InstantTimeDisplay(instant = instant, onChanged = onChanged)
    }
}

@Composable
private fun InstantTimeDisplay(instant: Instant, onChanged: (Instant) -> Unit) {
    val zone = ZoneId.systemDefault()
    val formatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val timeLabel = remember(instant) { instant.atZone(zone).toLocalTime().format(formatter) }
    val dateLabel = remember(instant) {
        instant.atZone(zone).toLocalDate()
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }

    // Placeholder surface; real impl shows TimePickerDialog on click
    Surface(
        onClick = { /* open time picker */ },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "$dateLabel  $timeLabel",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "LogEditSheet – feed, light", showBackground = true)
@Composable
private fun LogEditSheetFeedLightPreview() {
    val now = Instant.now()
    NurtlinaTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Edit feed (sheet content)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(16.dp))
                FeedEditFields(
                    draft = FeedEditState(
                        feedType = FeedType.FORMULA,
                        amountMl = "120",
                        nursingSide = null,
                        time = now,
                        note = "",
                    ),
                    useOz = false,
                    onChanged = {},
                )
            }
        }
    }
}

@Preview(name = "LogEditSheet – diaper, light", showBackground = true)
@Composable
private fun LogEditSheetDiaperLightPreview() {
    val now = Instant.now()
    NurtlinaTheme {
        Surface(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Edit diaper (sheet content)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(16.dp))
                DiaperEditFields(
                    draft = DiaperEditState(
                        diaperType = DiaperType.WET,
                        time = now,
                        note = "",
                    ),
                    onChanged = {},
                )
            }
        }
    }
}

@Preview(name = "LogEditSheet – sleep, dark", showBackground = true)
@Composable
private fun LogEditSheetSleepDarkPreview() {
    val now = Instant.now()
    NurtlinaTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Edit sleep (sheet content)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(16.dp))
                SleepEditFields(
                    draft = SleepEditState(
                        startedAt = now.minusSeconds(5400),
                        endedAt = now,
                        note = "Good nap",
                    ),
                    onChanged = {},
                )
            }
        }
    }
}
