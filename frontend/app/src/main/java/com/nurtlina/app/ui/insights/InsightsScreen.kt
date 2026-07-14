package com.nurtlina.app.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nurtlina.app.R
import com.nurtlina.app.domain.model.TodaySummary
import com.nurtlina.app.ui.theme.NurtlinaTheme

// ---------------------------------------------------------------------------
// Data types
// ---------------------------------------------------------------------------

enum class InsightsDateRange(val days: Int) {
    SEVEN(7), FOURTEEN(14), THIRTY(30)
}

/** One bucket of data per day, used by the trend charts. */
data class DailyDataPoint(
    val label: String,
    val value: Float,
)

data class InsightsTrendData(
    val feedsPerDay: List<DailyDataPoint>,
    val avgAmountMl: Double?,
    val diapersPerDay: List<DailyDataPoint>,
    val sleepHoursPerDay: List<DailyDataPoint>,
)

/** Alias kept for Pro-gated extended-range (14d/30d) trend data. */
typealias InsightsProData = InsightsTrendData

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

/**
 * Stateless Insights screen.
 *
 * @param todaySummary   Today's totals, shown for both free and pro users.
 * @param isPro          Whether the user has a Pro subscription.
 * @param useOz          Display amounts in oz (false = ml).
 * @param selectedRange  Currently selected trend date range (pro only).
 * @param weeklyData     7-day trend data — always shown for all users.
 * @param proData        Extended-range trend data for Pro users (14d / 30d).
 * @param onRangeSelected Called when user selects a different date range.
 * @param onUpgradeTapped Called when the user taps the upgrade banner.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    todaySummary: TodaySummary,
    isPro: Boolean,
    useOz: Boolean,
    selectedRange: InsightsDateRange,
    weeklyData: InsightsTrendData?,
    proData: InsightsTrendData?,
    onRangeSelected: (InsightsDateRange) -> Unit,
    onUpgradeTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.insights_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ---- Today summary section ----
            TodaySummarySection(
                summary = todaySummary,
                useOz = useOz,
            )

            // ---- Disclaimer (always visible) ----
            DisclaimerCard()

            // ---- 7-Day trends (always visible for all users) ----
            if (weeklyData != null) {
                SectionHeader(stringResource(R.string.insights_7day_trend_header))

                TrendChartCard(
                    title = stringResource(R.string.insights_feed_trend_header),
                    dataPoints = weeklyData.feedsPerDay,
                    barColor = MaterialTheme.colorScheme.primary,
                )

                weeklyData.avgAmountMl?.let { avg ->
                    AvgAmountCard(avgMl = avg, useOz = useOz)
                }

                TrendChartCard(
                    title = stringResource(R.string.insights_diaper_trend_header),
                    dataPoints = weeklyData.diapersPerDay,
                    barColor = MaterialTheme.colorScheme.secondary,
                )

                TrendChartCard(
                    title = stringResource(R.string.insights_sleep_trend_header),
                    dataPoints = weeklyData.sleepHoursPerDay,
                    barColor = MaterialTheme.colorScheme.tertiary,
                )
            }

            if (isPro && proData != null) {
                // ---- Pro: range selector ----
                DateRangeSelector(
                    selected = selectedRange,
                    onSelect = onRangeSelected,
                )

                // ---- Pro: extended-range charts ----
                if (selectedRange != InsightsDateRange.SEVEN) {
                    TrendChartCard(
                        title = stringResource(R.string.insights_feed_trend_header),
                        dataPoints = proData.feedsPerDay,
                        barColor = MaterialTheme.colorScheme.primary,
                    )

                    proData.avgAmountMl?.let { avg ->
                        AvgAmountCard(avgMl = avg, useOz = useOz)
                    }

                    TrendChartCard(
                        title = stringResource(R.string.insights_diaper_trend_header),
                        dataPoints = proData.diapersPerDay,
                        barColor = MaterialTheme.colorScheme.secondary,
                    )

                    TrendChartCard(
                        title = stringResource(R.string.insights_sleep_trend_header),
                        dataPoints = proData.sleepHoursPerDay,
                        barColor = MaterialTheme.colorScheme.tertiary,
                    )
                }
            } else if (!isPro) {
                // ---- Free: upgrade banner (below 7-day charts) ----
                UpgradeBanner(onUpgradeTapped = onUpgradeTapped)
            }

            // Bottom spacer for navigation bar
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Section header
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp),
    )
}

// ---------------------------------------------------------------------------
// Today summary
// ---------------------------------------------------------------------------

@Composable
private fun TodaySummarySection(
    summary: TodaySummary,
    useOz: Boolean,
) {
    val amountDisplay = if (useOz) {
        "%.1f oz".format(summary.totalAmountMl / 29.5735)
    } else {
        "%.0f ml".format(summary.totalAmountMl)
    }

    val sleepMillis = summary.sleepDurationMillis
    val sleepH = sleepMillis / 3_600_000
    val sleepM = (sleepMillis % 3_600_000) / 60_000
    val sleepDisplay = if (sleepH > 0) "%dh %02dm".format(sleepH, sleepM) else "%dm".format(sleepM)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.insights_today_summary_header),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryCard(
                icon = Icons.Outlined.LocalDrink,
                label = stringResource(R.string.insights_feeds_label),
                value = "${summary.totalFeedCount}",
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                icon = Icons.Outlined.Opacity,
                label = stringResource(R.string.insights_amount_label),
                value = amountDisplay,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryCard(
                icon = Icons.Outlined.BabyChangingStation,
                label = stringResource(R.string.insights_diapers_label),
                value = "${summary.diaperCount}",
                modifier = Modifier.weight(1f),
            )
            SummaryCard(
                icon = Icons.Outlined.Bedtime,
                label = stringResource(R.string.insights_sleep_label),
                value = sleepDisplay,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Disclaimer card (always shown)
// ---------------------------------------------------------------------------

@Composable
private fun DisclaimerCard() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 1.dp),
            )
            Text(
                text = stringResource(R.string.insights_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Date range selector (Pro)
// ---------------------------------------------------------------------------

@Composable
private fun DateRangeSelector(
    selected: InsightsDateRange,
    onSelect: (InsightsDateRange) -> Unit,
) {
    val ranges = listOf(
        InsightsDateRange.SEVEN to R.string.insights_range_7d,
        InsightsDateRange.FOURTEEN to R.string.insights_range_14d,
        InsightsDateRange.THIRTY to R.string.insights_range_30d,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ranges.forEach { (range, labelRes) ->
            FilterChip(
                selected = selected == range,
                onClick = { onSelect(range) },
                label = { Text(stringResource(labelRes)) },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Trend bar chart card (Pro)
// ---------------------------------------------------------------------------

@Composable
private fun TrendChartCard(
    title: String,
    dataPoints: List<DailyDataPoint>,
    barColor: Color,
) {
    val chartCd = stringResource(R.string.insights_chart_cd)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))

            if (dataPoints.isEmpty()) {
                Text(
                    text = stringResource(R.string.insights_no_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SimpleBarChart(
                    dataPoints = dataPoints,
                    barColor = barColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .semantics { contentDescription = chartCd },
                )
                Spacer(Modifier.height(4.dp))
                // X-axis labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val step = maxOf(1, dataPoints.size / 5)
                    dataPoints.filterIndexed { i, _ -> i % step == 0 || i == dataPoints.lastIndex }
                        .forEach { pt ->
                            Text(
                                text = pt.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun SimpleBarChart(
    dataPoints: List<DailyDataPoint>,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier) {
        if (dataPoints.isEmpty()) return@Canvas

        val maxValue = dataPoints.maxOf { it.value }.coerceAtLeast(1f)
        val barCount = dataPoints.size
        val totalWidth = size.width
        val totalHeight = size.height
        val barWidth = (totalWidth / barCount) * 0.6f
        val gap = (totalWidth / barCount) * 0.4f

        // Draw horizontal grid line at top
        drawLine(
            color = gridLineColor,
            start = Offset(0f, 0f),
            end = Offset(totalWidth, 0f),
            strokeWidth = 1.dp.toPx(),
        )

        dataPoints.forEachIndexed { i, pt ->
            val left = i * (barWidth + gap) + gap / 2
            val barHeight = (pt.value / maxValue) * totalHeight
            val top = totalHeight - barHeight

            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Average amount card (Pro)
// ---------------------------------------------------------------------------

@Composable
private fun AvgAmountCard(avgMl: Double, useOz: Boolean) {
    val display = if (useOz) "%.1f oz".format(avgMl / 29.5735) else "%.0f ml".format(avgMl)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.insights_avg_amount_per_feed),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = display,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Upgrade banner (Free tier)
// ---------------------------------------------------------------------------

@Composable
private fun UpgradeBanner(onUpgradeTapped: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = stringResource(R.string.insights_pro_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }

            Text(
                text = stringResource(R.string.insights_pro_banner_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )

            Button(
                onClick = onUpgradeTapped,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Text(stringResource(R.string.insights_upgrade_pro))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

private fun fakeSummary() = TodaySummary(
    totalFeedCount = 6,
    totalAmountMl = 720.0,
    diaperCount = 5,
    sleepDurationMillis = 10_800_000L,
    activeSleepStartedAt = null,
)

@Preview(name = "Insights – free tier light", showBackground = true)
@Composable
private fun InsightsFreeLightPreview() {
    val days = (1..7).map { DailyDataPoint("D$it", it.toFloat() % 8 + 3f) }
    NurtlinaTheme {
        InsightsScreen(
            todaySummary = fakeSummary(),
            isPro = false,
            useOz = false,
            selectedRange = InsightsDateRange.SEVEN,
            weeklyData = InsightsTrendData(
                feedsPerDay = days,
                avgAmountMl = 115.0,
                diapersPerDay = days.map { it.copy(value = it.value * 0.7f) },
                sleepHoursPerDay = days.map { it.copy(value = it.value * 1.5f) },
            ),
            proData = null,
            onRangeSelected = {},
            onUpgradeTapped = {},
        )
    }
}

@Preview(name = "Insights – pro tier light", showBackground = true)
@Composable
private fun InsightsProLightPreview() {
    val days7 = (1..7).map { DailyDataPoint("D$it", it.toFloat() % 8 + 3f) }
    val days14 = (1..14).map { DailyDataPoint("D$it", it.toFloat() % 8 + 3f) }
    NurtlinaTheme {
        InsightsScreen(
            todaySummary = fakeSummary(),
            isPro = true,
            useOz = false,
            selectedRange = InsightsDateRange.FOURTEEN,
            weeklyData = InsightsTrendData(
                feedsPerDay = days7,
                avgAmountMl = 115.0,
                diapersPerDay = days7.map { it.copy(value = it.value * 0.7f) },
                sleepHoursPerDay = days7.map { it.copy(value = it.value * 1.5f) },
            ),
            proData = InsightsTrendData(
                feedsPerDay = days14,
                avgAmountMl = 120.0,
                diapersPerDay = days14.map { it.copy(value = it.value * 0.8f) },
                sleepHoursPerDay = days14.map { it.copy(value = it.value * 1.3f) },
            ),
            onRangeSelected = {},
            onUpgradeTapped = {},
        )
    }
}

@Preview(name = "Insights – free tier dark", showBackground = true)
@Composable
private fun InsightsFreeDarkPreview() {
    val days = (1..7).map { DailyDataPoint("D$it", it.toFloat() % 8 + 3f) }
    NurtlinaTheme(darkTheme = true) {
        InsightsScreen(
            todaySummary = fakeSummary(),
            isPro = false,
            useOz = true,
            selectedRange = InsightsDateRange.SEVEN,
            weeklyData = InsightsTrendData(
                feedsPerDay = days,
                avgAmountMl = 115.0,
                diapersPerDay = days.map { it.copy(value = it.value * 0.7f) },
                sleepHoursPerDay = days.map { it.copy(value = it.value * 1.5f) },
            ),
            proData = null,
            onRangeSelected = {},
            onUpgradeTapped = {},
        )
    }
}
