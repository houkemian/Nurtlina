package com.nurtlina.app.ui.settings

import com.nurtlina.app.domain.model.DiaperLog
import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.model.SleepLog
import java.time.Instant
import java.util.Locale

internal object CareLogCsvExporter {

    fun export(
        feeds: List<FeedLog>,
        diapers: List<DiaperLog>,
        sleeps: List<SleepLog>,
    ): String = buildString {
        append("record_type,started_at,ended_at,detail,amount_ml,note\r\n")
        rows(feeds, diapers, sleeps).forEach { row ->
            append(
                listOf(
                    row.recordType,
                    row.startedAt.toString(),
                    row.endedAt?.toString().orEmpty(),
                    row.detail,
                    row.amountMl?.let { String.format(Locale.ROOT, "%.2f", it) }.orEmpty(),
                    row.note.orEmpty(),
                ).joinToString(",", transform = ::escapeCell)
            )
            append("\r\n")
        }
    }

    private fun rows(
        feeds: List<FeedLog>,
        diapers: List<DiaperLog>,
        sleeps: List<SleepLog>,
    ): List<CsvRow> = buildList {
        feeds.forEach { feed ->
            add(
                CsvRow(
                    recordType = "feed",
                    startedAt = feed.startedAt,
                    endedAt = feed.endedAt,
                    detail = buildString {
                        append(feed.feedType.name.lowercase(Locale.ROOT))
                        feed.side?.let { append(":${it.name.lowercase(Locale.ROOT)}") }
                    },
                    amountMl = feed.amountMl,
                    note = feed.note,
                )
            )
        }
        diapers.forEach { diaper ->
            add(
                CsvRow(
                    recordType = "diaper",
                    startedAt = diaper.changedAt,
                    endedAt = null,
                    detail = diaper.diaperType.name.lowercase(Locale.ROOT),
                    amountMl = null,
                    note = diaper.note,
                )
            )
        }
        sleeps.forEach { sleep ->
            add(
                CsvRow(
                    recordType = "sleep",
                    startedAt = sleep.startedAt,
                    endedAt = sleep.endedAt,
                    detail = if (sleep.isActive) "active" else "completed",
                    amountMl = null,
                    note = sleep.note,
                )
            )
        }
    }.sortedBy { it.startedAt }

    private fun escapeCell(rawValue: String): String {
        val startsWithFormula = rawValue.firstOrNull()?.let { it in FORMULA_PREFIXES } == true
        val value = if (startsWithFormula) "'$rawValue" else rawValue
        return "\"${value.replace("\"", "\"\"")}\""
    }

    private data class CsvRow(
        val recordType: String,
        val startedAt: Instant,
        val endedAt: Instant?,
        val detail: String,
        val amountMl: Double?,
        val note: String?,
    )

    private val FORMULA_PREFIXES = setOf('=', '+', '-', '@')
}
