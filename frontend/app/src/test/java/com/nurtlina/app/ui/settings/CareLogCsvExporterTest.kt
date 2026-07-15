package com.nurtlina.app.ui.settings

import com.nurtlina.app.domain.model.DiaperLog
import com.nurtlina.app.domain.model.DiaperType
import com.nurtlina.app.domain.model.FeedLog
import com.nurtlina.app.domain.model.FeedType
import com.nurtlina.app.domain.model.SleepLog
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CareLogCsvExporterTest {

    @Test
    fun `exports all record types in chronological order`() {
        val csv = CareLogCsvExporter.export(
            feeds = listOf(feed(at = "2026-07-15T09:00:00Z")),
            diapers = listOf(diaper(at = "2026-07-15T08:00:00Z")),
            sleeps = listOf(sleep(at = "2026-07-15T10:00:00Z")),
        )

        val lines = csv.lines().filter { it.isNotEmpty() }
        assertEquals(4, lines.size)
        assertTrue(lines[1].startsWith("\"diaper\""))
        assertTrue(lines[2].startsWith("\"feed\""))
        assertTrue(lines[3].startsWith("\"sleep\""))
    }

    @Test
    fun `escapes quotes newlines commas and spreadsheet formulas`() {
        val csv = CareLogCsvExporter.export(
            feeds = listOf(feed(at = "2026-07-15T09:00:00Z", note = "=SUM(1,2)\n\"check\"")),
            diapers = emptyList(),
            sleeps = emptyList(),
        )

        assertTrue(csv.contains("\"'=SUM(1,2)\n\"\"check\"\"\""))
    }

    private fun feed(at: String, note: String? = null): FeedLog {
        val instant = Instant.parse(at)
        return FeedLog(
            id = "feed-1",
            babyId = "baby-1",
            bottleId = null,
            feedType = FeedType.FORMULA,
            amountMl = 120.0,
            startedAt = instant,
            endedAt = instant,
            side = null,
            note = note,
            createdAt = instant,
            updatedAt = instant,
        )
    }

    private fun diaper(at: String): DiaperLog {
        val instant = Instant.parse(at)
        return DiaperLog(
            id = "diaper-1",
            babyId = "baby-1",
            diaperType = DiaperType.WET,
            changedAt = instant,
            note = null,
            createdAt = instant,
            updatedAt = instant,
        )
    }

    private fun sleep(at: String): SleepLog {
        val instant = Instant.parse(at)
        return SleepLog(
            id = "sleep-1",
            babyId = "baby-1",
            startedAt = instant,
            endedAt = null,
            note = null,
            createdAt = instant,
            updatedAt = instant,
        )
    }
}
