package com.nurtlina.app.domain.rating

import java.time.Instant

data class RatingPromptState(
    val ratingPromptShownCount: Int = 0,
    val ratingPromptLastShownAt: Instant? = null,
    val ratingPromptDismissedPermanently: Boolean = false,
    val ratingPromptRatedAt: Instant? = null,
    val eligiblePositiveActionCount: Int = 0,
    val feedLoggedCount: Int = 0,
    val firstLaunchAt: Instant? = null,
    val lastNotificationOpenAt: Instant? = null,
    val lastNegativeActionAt: Instant? = null,
)
