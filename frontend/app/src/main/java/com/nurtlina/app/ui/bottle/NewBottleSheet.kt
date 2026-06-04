package com.nurtlina.app.ui.bottle

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.annotation.StringRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nurtlina.app.R
import com.nurtlina.app.domain.model.MilkType
import com.nurtlina.app.domain.model.UnitType
import com.nurtlina.app.ui.theme.NurtlinaTheme
import com.nurtlina.app.ui.today.formatAmount
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// --------------------------------------------------------------------------
// UI state
// --------------------------------------------------------------------------

/**
 * State for the New Bottle bottom sheet.
 *
 * [amountMl] is always stored in millilitres. Convert for display only.
 * [isJustNow] controls whether [preparedAt] is treated as "now" or a custom time.
 */
data class NewBottleUiState(
    val milkType: MilkType = MilkType.FORMULA,
    val amountMl: Double? = null,
    val preparedAt: Instant = Instant.now(),
    val isJustNow: Boolean = true,
    val note: String = "",
    val unitType: UnitType = UnitType.ML,
    val isCreating: Boolean = false,
)

private val ML_PRESETS = listOf(60.0, 90.0, 120.0, 150.0, 180.0)
private val OZ_PRESETS_ML = listOf(59.15, 88.72, 118.29, 147.87, 177.44) // 2,3,4,5,6 oz in ml

// --------------------------------------------------------------------------
// Root composable
// --------------------------------------------------------------------------

/**
 * Bottom sheet for creating a new bottle timer.
 *
 * The sheet is shown by the caller; pass [onDismiss] to let it close.
 * All state changes are relayed via callbacks; the composable is stateless.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBottleSheet(
    state: NewBottleUiState,
    onMilkTypeChange: (MilkType) -> Unit,
    onAmountChange: (Double?) -> Unit,
    onPreparedAtChange: (Instant) -> Unit,
    onIsJustNowChange: (Boolean) -> Unit,
    onNoteChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int = R.string.new_bottle_title,
    @StringRes createButtonRes: Int = R.string.action_create_bottle,
    @StringRes disclaimerRes: Int = R.string.new_bottle_disclaimer,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(20.dp))

            // Milk type selector
            MilkTypeSelector(
                selected = state.milkType,
                onSelect = onMilkTypeChange,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))

            // Amount
            Text(
                text = stringResource(R.string.new_bottle_amount_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(8.dp))
            AmountSelector(
                amountMl = state.amountMl,
                unitType = state.unitType,
                onAmountChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))

            // Prepared at
            Text(
                text = stringResource(R.string.new_bottle_prepared_at_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(8.dp))
            PreparedAtSelector(
                isJustNow = state.isJustNow,
                preparedAt = state.preparedAt,
                onIsJustNowChange = onIsJustNowChange,
                onPickTime = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))

            // Note
            OutlinedTextField(
                value = state.note,
                onValueChange = onNoteChange,
                label = { Text(stringResource(R.string.new_bottle_note_label)) },
                placeholder = { Text(stringResource(R.string.new_bottle_note_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
            Spacer(Modifier.height(16.dp))

            // Disclaimer
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(disclaimerRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Create button
            Button(
                onClick = onCreate,
                enabled = !state.isCreating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = stringResource(createButtonRes),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showTimePicker) {
        val localNow = LocalDateTime.ofInstant(state.preparedAt, ZoneId.systemDefault())
        val timePickerState = rememberTimePickerState(
            initialHour = localNow.hour,
            initialMinute = localNow.minute,
            is24Hour = false,
        )
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                val updated = LocalDateTime.now()
                    .withHour(timePickerState.hour)
                    .withMinute(timePickerState.minute)
                    .withSecond(0)
                    .withNano(0)
                onPreparedAtChange(updated.atZone(ZoneId.systemDefault()).toInstant())
                onIsJustNowChange(false)
                showTimePicker = false
            },
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

// --------------------------------------------------------------------------
// Milk type selector (segmented buttons)
// --------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MilkTypeSelector(
    selected: MilkType,
    onSelect: (MilkType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(MilkType.FORMULA, MilkType.BREAST_MILK, MilkType.CUSTOM)
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, milkType ->
            SegmentedButton(
                selected = selected == milkType,
                onClick = { onSelect(milkType) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(milkType.label()) },
            )
        }
    }
}

// --------------------------------------------------------------------------
// Amount selector (presets + manual)
// --------------------------------------------------------------------------

@Composable
private fun AmountSelector(
    amountMl: Double?,
    unitType: UnitType,
    onAmountChange: (Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val presets = if (unitType == UnitType.ML) ML_PRESETS else OZ_PRESETS_ML
    val presetLabels = if (unitType == UnitType.ML) {
        ML_PRESETS.map { "%.0f".format(it) }
    } else {
        listOf("2", "3", "4", "5", "6")
    }

    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presets.forEachIndexed { index, presetMl ->
                FilterChip(
                    selected = amountMl != null && kotlin.math.abs(amountMl - presetMl) < 1.0,
                    onClick = { onAmountChange(presetMl) },
                    label = { Text(presetLabels[index]) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        val unitSuffix = if (unitType == UnitType.ML) " ml" else " oz"
        val displayValue = when {
            amountMl == null -> ""
            unitType == UnitType.ML -> "%.0f".format(amountMl)
            else -> "%.1f".format(amountMl / 29.5735)
        }
        OutlinedTextField(
            value = displayValue,
            onValueChange = { text ->
                val parsed = text.trim().toDoubleOrNull()
                if (parsed != null) {
                    val ml = if (unitType == UnitType.ML) parsed else parsed * 29.5735
                    onAmountChange(ml)
                } else if (text.isBlank()) {
                    onAmountChange(null)
                }
            },
            label = { Text(stringResource(R.string.new_bottle_amount_label) + unitSuffix) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }
}

// --------------------------------------------------------------------------
// Prepared at selector
// --------------------------------------------------------------------------

@Composable
private fun PreparedAtSelector(
    isJustNow: Boolean,
    preparedAt: Instant,
    onIsJustNowChange: (Boolean) -> Unit,
    onPickTime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedTime = remember(preparedAt) {
        LocalDateTime.ofInstant(preparedAt, ZoneId.systemDefault())
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SuggestionChip(
            onClick = { onIsJustNowChange(true) },
            label = { Text(stringResource(R.string.new_bottle_prepared_just_now)) },
            enabled = !isJustNow,
        )
        SuggestionChip(
            onClick = onPickTime,
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (!isJustNow) formattedTime
                        else stringResource(R.string.new_bottle_prepared_pick_time),
                    )
                }
            },
            enabled = isJustNow,
        )
    }
}

// --------------------------------------------------------------------------
// Time picker dialog wrapper
// --------------------------------------------------------------------------

@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(android.R.string.ok))
            }
        },
        text = { content() },
    )
}

// --------------------------------------------------------------------------
// Extensions
// --------------------------------------------------------------------------

@Composable
private fun MilkType.label(): String = when (this) {
    MilkType.FORMULA -> stringResource(R.string.milk_type_formula)
    MilkType.BREAST_MILK -> stringResource(R.string.milk_type_breast_milk)
    MilkType.CUSTOM -> stringResource(R.string.milk_type_custom)
}

// --------------------------------------------------------------------------
// Previews
// --------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "NewBottleSheet (Light)", showBackground = true)
@Composable
private fun PreviewNewBottleSheetLight() {
    NurtlinaTheme {
        NewBottleSheet(
            state = NewBottleUiState(
                milkType = MilkType.FORMULA,
                amountMl = 120.0,
                unitType = UnitType.ML,
            ),
            onMilkTypeChange = {},
            onAmountChange = {},
            onPreparedAtChange = {},
            onIsJustNowChange = {},
            onNoteChange = {},
            onCreate = {},
            onDismiss = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "NewBottleSheet (Dark)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewNewBottleSheetDark() {
    NurtlinaTheme(darkTheme = true) {
        NewBottleSheet(
            state = NewBottleUiState(
                milkType = MilkType.BREAST_MILK,
                amountMl = null,
                unitType = UnitType.OZ,
            ),
            onMilkTypeChange = {},
            onAmountChange = {},
            onPreparedAtChange = {},
            onIsJustNowChange = {},
            onNoteChange = {},
            onCreate = {},
            onDismiss = {},
        )
    }
}
