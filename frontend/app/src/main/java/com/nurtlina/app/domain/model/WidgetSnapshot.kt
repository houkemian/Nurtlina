package com.nurtlina.app.domain.model

import java.time.Instant

/**
 * One-shot data snapshot rendered by the home-screen widget.
 *
 * Contains no medical claims — it only surfaces the last feeding record and a
 * gentle next-feed window computed from the same logic the app already uses.
 */
data class WidgetSnapshot(
    val babyName: String?,
    val lastFeedAt: Instant?,
    val lastFeedAmountMl: Double?,
    val unit: UnitType,
    val nextFeedAt: Instant?,
    val theme: WidgetTheme = WidgetTheme.DEFAULT,
) {
    companion object {
        val Empty = WidgetSnapshot(
            babyName = null,
            lastFeedAt = null,
            lastFeedAmountMl = null,
            unit = UnitType.ML,
            nextFeedAt = null,
        )
    }
}
