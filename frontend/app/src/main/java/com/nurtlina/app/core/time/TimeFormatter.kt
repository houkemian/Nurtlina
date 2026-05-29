package com.nurtlina.app.core.time

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeFormatter {

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")

    fun formatTime(instant: Instant): String =
        instant.atZone(ZoneId.systemDefault()).format(timeFormatter)

    fun formatDateTime(instant: Instant): String =
        instant.atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

    fun formatDate(instant: Instant): String =
        instant.atZone(ZoneId.systemDefault()).format(dateFormatter)

    fun formatCountdown(expiresAt: Instant, now: Instant = Instant.now()): CountdownState {
        val diff = Duration.between(now, expiresAt)
        return if (diff.isNegative) {
            CountdownState.Expired(overdueDuration = diff.abs())
        } else {
            CountdownState.Active(remaining = diff)
        }
    }

    fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    fun formatAmount(amountMl: Double?, unit: com.nurtlina.app.domain.model.UnitType): String {
        if (amountMl == null) return "-"
        return when (unit) {
            com.nurtlina.app.domain.model.UnitType.ML -> "${amountMl.toInt()} ml"
            com.nurtlina.app.domain.model.UnitType.OZ -> String.format("%.1f oz", amountMl / 29.5735)
        }
    }
}

sealed interface CountdownState {
    data class Active(val remaining: Duration) : CountdownState {
        val displayText: String get() {
            val hours = remaining.toHours()
            val minutes = remaining.toMinutesPart()
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                else -> "${minutes}m"
            }
        }
    }
    data class Expired(val overdueDuration: Duration) : CountdownState {
        val displayText: String get() = "EXPIRED"
    }
}
