package com.nurtlina.app.domain.rating

sealed interface RatingPromptDecision {
    data object Eligible : RatingPromptDecision
    data class Blocked(val reason: RatingPromptBlockedReason) : RatingPromptDecision
}

enum class RatingPromptBlockedReason {
    FIRST_LAUNCH_NOT_SET,
    TOO_SOON_AFTER_FIRST_LAUNCH,
    NOT_ENOUGH_FEEDS,
    NOT_ENOUGH_POSITIVE_ACTIONS,
    NIGHT_MODE,
    COOLDOWN,
    DISMISSED_PERMANENTLY,
    ALREADY_RATED,
    MAX_SHOWN,
    RECENT_NOTIFICATION_SESSION,
    RECENT_NEGATIVE_ACTION,
    ALREADY_SHOWN_THIS_SESSION,
}
