package com.nurtlina.app.ui.widget

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.nurtlina.app.MainActivity
import com.nurtlina.app.R
import com.nurtlina.app.core.time.TimeFormatter
import com.nurtlina.app.domain.model.WidgetSnapshot
import com.nurtlina.app.domain.usecase.widget.GetWidgetSnapshotUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Home-screen widget showing the selected baby's last feed and next-feed window.
 *
 * Rendered with Glance; tap opens [MainActivity].
 */
class NurtlinaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val snapshot = runCatching { entryPoint.getWidgetSnapshotUseCase().invoke() }
            .getOrDefault(WidgetSnapshot.Empty)
        val now = Instant.now()
        provideContent {
            NurtlinaWidgetContent(snapshot = snapshot, now = now)
        }
    }
}

class NurtlinaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NurtlinaWidget()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun getWidgetSnapshotUseCase(): GetWidgetSnapshotUseCase
}

@Composable
private fun NurtlinaWidgetContent(snapshot: WidgetSnapshot, now: Instant) {
    val context = LocalContext.current
    val title = snapshot.babyName ?: context.getString(R.string.app_name)
    val lastFeedLine = buildLastFeedLine(context, snapshot, now)
    val nextFeedLine = buildNextFeedLine(context, snapshot)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(
                ColorProvider(
                    day = Color(0xFFF6F1EA),
                    night = Color(0xFF1C1917),
                ),
            )
            .cornerRadius(24.dp)
            .padding(16.dp)
            .clickable(onClick = actionStartActivity<MainActivity>()),
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = ColorProvider(day = Color(0xFF8A6A4C), night = Color(0xFFD9B99A)),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            ),
        )
        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = lastFeedLine,
            style = TextStyle(
                color = ColorProvider(day = Color(0xFF352E28), night = Color(0xFFEDE6DF)),
                fontSize = 14.sp,
            ),
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = nextFeedLine,
            style = TextStyle(
                color = ColorProvider(day = Color(0xFF352E28), night = Color(0xFFEDE6DF)),
                fontSize = 14.sp,
            ),
        )
    }
}

private fun buildLastFeedLine(context: Context, snapshot: WidgetSnapshot, now: Instant): String {
    val lastFeedAt = snapshot.lastFeedAt
    if (lastFeedAt == null) {
        return context.getString(R.string.widget_no_feeds)
    }
    val timeAgo = DateUtils.getRelativeTimeSpanString(
        lastFeedAt.toEpochMilli(),
        now.toEpochMilli(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
    val amount = snapshot.lastFeedAmountMl?.let { TimeFormatter.formatAmount(it, snapshot.unit) }
    val value = if (amount != null) "$timeAgo · $amount" else timeAgo
    return context.getString(R.string.widget_last_feed, value)
}

private fun buildNextFeedLine(context: Context, snapshot: WidgetSnapshot): String {
    val nextFeedAt = snapshot.nextFeedAt
    if (nextFeedAt == null) {
        return context.getString(R.string.widget_learning)
    }
    val time = nextFeedAt.atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    return context.getString(R.string.widget_next_feed, time)
}

/**
 * Triggers a widget refresh after local feeding data changes.
 *
 * Kept separate so ViewModels can depend on it without reaching into Glance APIs.
 */
@Singleton
class WidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun refresh() {
        runCatching { NurtlinaWidget().updateAll(context) }
    }
}
