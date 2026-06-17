package com.nurtlina.app.domain.rating

import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.BottleStatus
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

class RatingPromptEligibility @Inject constructor() {

    fun evaluate(
        state: RatingPromptState,
        activeBottles: List<Bottle>,
        nightModeEnabled: Boolean,
        alreadyShownThisSession: Boolean,
        now: Instant,
    ): RatingPromptDecision {
        val firstLaunchAt = state.firstLaunchAt
            ?: return RatingPromptDecision.Blocked(RatingPromptBlockedReason.FIRST_LAUNCH_NOT_SET)

        return when {
            alreadyShownThisSession ->
                RatingPromptDecision.Blocked(RatingPromptBlockedReason.ALREADY_SHOWN_THIS_SESSION)
            state.ratingPromptDismissedPermanently ->
                RatingPromptDecision.Blocked(RatingPromptBlockedReason.DISMISSED_PERMANENTLY)
            state.ratingPromptRatedAt != null ->
                RatingPromptDecision.Blocked(RatingPromptBlockedReason.ALREADY_RATED)
            state.ratingPromptShownCount >= MAX_SHOWN_COUNT ->
                RatingPromptDecision.Blocked(RatingPromptBlockedReason.MAX_SHOWN)
            Duration.between(firstLaunchAt, now) < MIN_TIME_AFTER_FIRST_LAUNCH ->
                RatingPromptDecision.Blocked(RatingPromptBlockedReason.TOO_SOON_AFTER_FIRST_LAUNCH)
            state.bottleTimerCreatedCount < MIN_BOTTLE_TIMERS_CREATED ->
                RatingPromptDecision.Blocked(RatingPromptBlockedReason.NOT_ENOUGH_BOTTLE_TIMERS)
            state.eligiblePositiveActionCount < MIN_POSITIVE_ACTIONS ->
                RatingPromptDecision.Blocked(RatingPromptBlockedReason.NOT_ENOUGH_POSITIVE_ACTIONS)
            nightModeEnabled ->
                RatingPromptDecision.Blocked(RatingPromptBlockedReason.NIGHT_MODE)
            activeBottles.any { it.status == BottleStatus.EXPIRED || it.expiresAt?.isBefore(now) == true } ->
                RatingPromptDecision.Blocked(RatingPromptBlockedReason.ACTIVE_EXPIRED_BOTTLE)
            activeBottles.any { it.status == BottleStatus.FEEDING_STARTED } ->
                RatingPromptDecision.Blocked(RatingPromptBlockedReason.ACTIVE_FEEDING)
            state.ratingPromptLastShownAt?.let { Duration.between(it, now) < PROMPT_COOLDOWN } == true ->
                RatingPromptDecision.Blocked(RatingPromptBlockedReason.COOLDOWN)
            state.lastNotificationOpenAt?.let { Duration.between(it, now) < NOTIFICATION_SESSION_SUPPRESSION } == true ->
                RatingPromptDecision.Blocked(RatingPromptBlockedReason.RECENT_NOTIFICATION_SESSION)
            state.lastNegativeActionAt?.let { Duration.between(it, now) < NEGATIVE_ACTION_SUPPRESSION } == true ->
                RatingPromptDecision.Blocked(RatingPromptBlockedReason.RECENT_NEGATIVE_ACTION)
            else -> RatingPromptDecision.Eligible
        }
    }

    companion object {
        private const val MIN_BOTTLE_TIMERS_CREATED = 5
        private const val MIN_POSITIVE_ACTIONS = 3
        private const val MAX_SHOWN_COUNT = 2
        private val MIN_TIME_AFTER_FIRST_LAUNCH: Duration = Duration.ofDays(3)
        private val PROMPT_COOLDOWN: Duration = Duration.ofDays(30)
        private val NOTIFICATION_SESSION_SUPPRESSION: Duration = Duration.ofMinutes(30)
        private val NEGATIVE_ACTION_SUPPRESSION: Duration = Duration.ofMinutes(30)
    }
}
