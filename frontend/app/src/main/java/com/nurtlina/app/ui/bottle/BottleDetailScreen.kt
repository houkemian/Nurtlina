package com.nurtlina.app.ui.bottle

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nurtlina.app.R
import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.BottleStatus
import com.nurtlina.app.domain.model.GuidelineRegion
import com.nurtlina.app.domain.model.MilkType
import com.nurtlina.app.domain.model.UnitType
import com.nurtlina.app.ui.theme.BottleStatusColors
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
 * Snapshot of data the Bottle Detail screen needs to render.
 *
 * Countdown and elapsed time are pre-formatted by ViewModel to avoid
 * business logic in the composable.
 */
data class BottleDetailUiState(
    val bottle: Bottle? = null,
    /** e.g. "1h 24m", "45m", or empty when terminal. */
    val countdownText: String = "",
    /** Elapsed time since feeding started, e.g. "15m". */
    val elapsedText: String = "",
    val isExpired: Boolean = false,
    val isExpiringSoon: Boolean = false,
    val guidelineSourceName: String = "",
    val unitType: UnitType = UnitType.ML,
)

// --------------------------------------------------------------------------
// Root composable
// --------------------------------------------------------------------------

/**
 * Bottle detail screen showing status, timeline, and available actions.
 *
 * Stateless: all mutable state is owned by [BottleViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottleDetailScreen(
    state: BottleDetailUiState,
    onBack: () -> Unit,
    onStartFeeding: () -> Unit,
    onRefrigerate: () -> Unit,
    onMarkFed: () -> Unit,
    onDiscard: () -> Unit,
    onEditPreparedTime: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bottle_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.onboarding_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEditPreparedTime) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.action_edit_prepared_time),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        val bottle = state.bottle
        if (bottle == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // Status header card
            StatusHeaderCard(
                bottle = bottle,
                countdownText = state.countdownText,
                elapsedText = state.elapsedText,
                isExpired = state.isExpired,
                isExpiringSoon = state.isExpiringSoon,
                unitType = state.unitType,
                modifier = Modifier.fillMaxWidth(),
            )

            // Expired warning
            if (state.isExpired) {
                Spacer(Modifier.height(12.dp))
                ExpiredWarningBanner(modifier = Modifier.fillMaxWidth())
            }

            // Action buttons
            if (!bottle.status.isTerminal) {
                Spacer(Modifier.height(16.dp))
                DetailActionButtons(
                    status = bottle.status,
                    onStartFeeding = onStartFeeding,
                    onRefrigerate = onRefrigerate,
                    onMarkFed = onMarkFed,
                    onDiscard = onDiscard,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(24.dp))

            // Timeline
            Text(
                text = stringResource(R.string.bottle_detail_timeline_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(12.dp))
            BottleTimeline(
                bottle = bottle,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))

            // Guideline attribution
            if (state.guidelineSourceName.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.bottle_detail_guideline_source, state.guidelineSourceName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Disclaimer
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.bottle_detail_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// --------------------------------------------------------------------------
// Status header card
// --------------------------------------------------------------------------

@Composable
private fun StatusHeaderCard(
    bottle: Bottle,
    countdownText: String,
    elapsedText: String,
    isExpired: Boolean,
    isExpiringSoon: Boolean,
    unitType: UnitType,
    modifier: Modifier = Modifier,
) {
    val statusColors = BottleStatusColors.colorsFor(bottle.status)

    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = statusColors.containerColor,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(color = statusColors.contentColor)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = bottle.status.label(),
                    style = MaterialTheme.typography.titleMedium,
                    color = statusColors.contentColor,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = bottle.milkType.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColors.contentColor.copy(alpha = 0.7f),
                )
            }

            Spacer(Modifier.height(20.dp))

            when {
                isExpired -> {
                    Text(
                        text = stringResource(R.string.bottle_status_expired).uppercase(),
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColors.contentColor,
                    )
                }
                bottle.status == BottleStatus.FEEDING_STARTED && elapsedText.isNotBlank() -> {
                    Column {
                        Text(
                            text = stringResource(R.string.bottle_detail_time_elapsed),
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColors.contentColor.copy(alpha = 0.7f),
                        )
                        Text(
                            text = elapsedText,
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = statusColors.contentColor,
                        )
                    }
                }
                !bottle.status.isTerminal && countdownText.isNotBlank() -> {
                    Column {
                        Text(
                            text = stringResource(R.string.bottle_detail_time_remaining),
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColors.contentColor.copy(alpha = 0.7f),
                        )
                        Text(
                            text = countdownText,
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = statusColors.contentColor,
                        )
                    }
                }
                else -> Unit
            }

            bottle.amountMl?.let { ml ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = ml.formatAmount(unitType),
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColors.contentColor.copy(alpha = 0.8f),
                )
            }

            bottle.note?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColors.contentColor.copy(alpha = 0.7f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier.size(12.dp),
    ) {
        drawCircle(color = color)
    }
}

// --------------------------------------------------------------------------
// Expired warning banner
// --------------------------------------------------------------------------

@Composable
private fun ExpiredWarningBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.bottle_detail_discard_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

// --------------------------------------------------------------------------
// Action buttons
// --------------------------------------------------------------------------

@Composable
private fun DetailActionButtons(
    status: BottleStatus,
    onStartFeeding: () -> Unit,
    onRefrigerate: () -> Unit,
    onMarkFed: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (status) {
            BottleStatus.NOT_STARTED -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onStartFeeding,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Text(stringResource(R.string.action_start_feeding))
                    }
                    OutlinedButton(
                        onClick = onRefrigerate,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AcUnit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.action_refrigerate))
                    }
                }
                DiscardButton(
                    onDiscard = onDiscard,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                )
            }
            BottleStatus.REFRIGERATED -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onStartFeeding,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Text(stringResource(R.string.action_start_feeding))
                    }
                    DiscardButton(
                        onDiscard = onDiscard,
                        modifier = Modifier.weight(1f).height(48.dp),
                    )
                }
            }
            BottleStatus.FEEDING_STARTED -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onMarkFed,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.action_mark_fed))
                    }
                    DiscardButton(
                        onDiscard = onDiscard,
                        modifier = Modifier.weight(1f).height(48.dp),
                    )
                }
            }
            BottleStatus.EXPIRED -> {
                DiscardButton(
                    onDiscard = onDiscard,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                )
            }
            else -> Unit
        }
    }
}

@Composable
private fun DiscardButton(
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onDiscard,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.action_discard))
    }
}

// --------------------------------------------------------------------------
// Timeline
// --------------------------------------------------------------------------

@Composable
private fun BottleTimeline(
    bottle: Bottle,
    modifier: Modifier = Modifier,
) {
    data class TimelineEvent(
        val labelRes: Int,
        val time: Instant?,
        val icon: ImageVector,
        val isError: Boolean = false,
    )

    val events = buildList {
        add(TimelineEvent(
            labelRes = R.string.bottle_detail_event_prepared,
            time = bottle.preparedAt,
            icon = Icons.Default.LocalDrink,
        ))
        bottle.refrigeratedAt?.let {
            add(TimelineEvent(
                labelRes = R.string.bottle_detail_event_refrigerated,
                time = it,
                icon = Icons.Default.AcUnit,
            ))
        }
        bottle.feedingStartedAt?.let {
            add(TimelineEvent(
                labelRes = R.string.bottle_detail_event_feeding_started,
                time = it,
                icon = Icons.Default.Timer,
            ))
        }
        if (bottle.status == BottleStatus.EXPIRED) {
            add(TimelineEvent(
                labelRes = R.string.bottle_detail_event_timer_expired,
                time = bottle.expiresAt,
                icon = Icons.Default.Warning,
                isError = true,
            ))
        }
        bottle.fedAt?.let {
            add(TimelineEvent(
                labelRes = R.string.bottle_detail_event_fed,
                time = it,
                icon = Icons.Default.CheckCircle,
            ))
        }
        bottle.discardedAt?.let {
            add(TimelineEvent(
                labelRes = R.string.bottle_detail_event_discarded,
                time = it,
                icon = Icons.Default.Delete,
            ))
        }
        if (bottle.status == BottleStatus.CANCELED) {
            add(TimelineEvent(
                labelRes = R.string.bottle_detail_event_canceled,
                time = bottle.updatedAt,
                icon = Icons.Default.Cancel,
            ))
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            events.forEachIndexed { index, event ->
                TimelineRow(
                    labelRes = event.labelRes,
                    time = event.time,
                    icon = event.icon,
                    isLast = index == events.lastIndex,
                    isErrorEvent = event.isError,
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    labelRes: Int,
    time: Instant?,
    icon: ImageVector,
    isLast: Boolean,
    isErrorEvent: Boolean,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (isErrorEvent)
        MaterialTheme.colorScheme.error
    else
        MaterialTheme.colorScheme.primary
    val timelineLineColor = MaterialTheme.colorScheme.outlineVariant

    val formattedTime = remember(time) {
        time?.let {
            LocalDateTime.ofInstant(it, ZoneId.systemDefault())
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
        } ?: "—"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            if (!isLast) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp),
                ) {
                    drawLine(
                        color = timelineLineColor,
                        start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                        strokeWidth = size.width,
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.padding(top = 2.dp)) {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isErrorEvent) contentColor else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isErrorEvent) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// --------------------------------------------------------------------------
// Extensions
// --------------------------------------------------------------------------

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

@Composable
private fun MilkType.label(): String = when (this) {
    MilkType.FORMULA -> stringResource(R.string.milk_type_formula)
    MilkType.BREAST_MILK -> stringResource(R.string.milk_type_breast_milk)
    MilkType.CUSTOM -> stringResource(R.string.milk_type_custom)
}

// --------------------------------------------------------------------------
// Previews
// --------------------------------------------------------------------------

private fun previewBottle(status: BottleStatus = BottleStatus.FEEDING_STARTED) = Bottle(
    id = "b1",
    babyId = "baby1",
    milkType = MilkType.FORMULA,
    amountMl = 120.0,
    preparedAt = Instant.now().minusSeconds(3600),
    feedingStartedAt = if (status == BottleStatus.FEEDING_STARTED ||
        status == BottleStatus.FED || status == BottleStatus.DISCARDED)
        Instant.now().minusSeconds(1800) else null,
    refrigeratedAt = null,
    status = status,
    guidelineRegion = GuidelineRegion.US,
    expiresAt = Instant.now().plusSeconds(1800),
    discardedAt = if (status == BottleStatus.DISCARDED) Instant.now() else null,
    fedAt = if (status == BottleStatus.FED) Instant.now() else null,
    note = "After morning nap",
    createdAt = Instant.now().minusSeconds(3600),
    updatedAt = Instant.now(),
)

@Preview(name = "BottleDetail – Feeding Started (Light)", showBackground = true, showSystemUi = true)
@Composable
private fun PreviewDetailFeedingStarted() {
    NurtlinaTheme {
        BottleDetailScreen(
            state = BottleDetailUiState(
                bottle = previewBottle(BottleStatus.FEEDING_STARTED),
                countdownText = "45m",
                elapsedText = "15m",
                guidelineSourceName = "CDC: Infant Formula Preparation and Storage",
            ),
            onBack = {},
            onStartFeeding = {},
            onRefrigerate = {},
            onMarkFed = {},
            onDiscard = {},
            onEditPreparedTime = {},
        )
    }
}

@Preview(name = "BottleDetail – Expired (Dark)", showBackground = true, showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDetailExpiredDark() {
    NurtlinaTheme(darkTheme = true) {
        BottleDetailScreen(
            state = BottleDetailUiState(
                bottle = previewBottle(BottleStatus.EXPIRED),
                countdownText = "",
                isExpired = true,
                guidelineSourceName = "CDC: Infant Formula Preparation and Storage",
            ),
            onBack = {},
            onStartFeeding = {},
            onRefrigerate = {},
            onMarkFed = {},
            onDiscard = {},
            onEditPreparedTime = {},
        )
    }
}

@Preview(name = "BottleDetail – Not Started (Light)", showBackground = true, showSystemUi = true)
@Composable
private fun PreviewDetailNotStarted() {
    NurtlinaTheme {
        BottleDetailScreen(
            state = BottleDetailUiState(
                bottle = previewBottle(BottleStatus.NOT_STARTED),
                countdownText = "1h 55m",
                guidelineSourceName = "CDC: Infant Formula Preparation and Storage",
            ),
            onBack = {},
            onStartFeeding = {},
            onRefrigerate = {},
            onMarkFed = {},
            onDiscard = {},
            onEditPreparedTime = {},
        )
    }
}
