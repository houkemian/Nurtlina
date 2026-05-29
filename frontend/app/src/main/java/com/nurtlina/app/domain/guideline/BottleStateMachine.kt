package com.nurtlina.app.domain.guideline

import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.BottleStatus
import com.nurtlina.app.domain.model.BottleTransition
import com.nurtlina.app.domain.model.BottleTransitionResult
import java.time.Instant

/**
 * Enforces valid bottle state transitions.
 * FeedingStarted does not revert to NotStarted silently.
 * Terminal states (Fed, Discarded, Canceled) accept no transitions.
 */
object BottleStateMachine {

    fun transition(
        bottle: Bottle,
        transition: BottleTransition,
        rule: GuidelineRule,
        now: Instant = Instant.now(),
    ): BottleTransitionResult {
        if (bottle.status.isTerminal) {
            return BottleTransitionResult.Error("Cannot transition from terminal status ${bottle.status}")
        }

        return when (transition) {
            is BottleTransition.StartFeeding -> handleStartFeeding(bottle, rule, now)
            is BottleTransition.Refrigerate -> handleRefrigerate(bottle, rule, now)
            is BottleTransition.MarkFed -> handleMarkFed(bottle, now)
            is BottleTransition.Discard -> handleDiscard(bottle, now)
            is BottleTransition.Cancel -> handleCancel(bottle, now)
            is BottleTransition.EditPreparedAt -> handleEditPreparedAt(bottle, transition.newTime, rule, now)
            is BottleTransition.EditAmount -> handleEditAmount(bottle, transition.newAmountMl, now)
        }
    }

    private fun handleStartFeeding(bottle: Bottle, rule: GuidelineRule, now: Instant): BottleTransitionResult {
        val allowed = bottle.status == BottleStatus.NOT_STARTED || bottle.status == BottleStatus.REFRIGERATED
        if (!allowed) {
            return BottleTransitionResult.Error("Cannot start feeding from ${bottle.status}")
        }
        val updated = bottle.copy(
            status = BottleStatus.FEEDING_STARTED,
            feedingStartedAt = now,
            updatedAt = now,
        )
        val expiresAt = ExpiryCalculator.calculate(updated, rule)
        return BottleTransitionResult.Success(updated.copy(expiresAt = expiresAt))
    }

    private fun handleRefrigerate(bottle: Bottle, rule: GuidelineRule, now: Instant): BottleTransitionResult {
        if (bottle.status != BottleStatus.NOT_STARTED) {
            return BottleTransitionResult.Error("Cannot refrigerate from ${bottle.status}")
        }
        val updated = bottle.copy(
            status = BottleStatus.REFRIGERATED,
            refrigeratedAt = now,
            updatedAt = now,
        )
        val expiresAt = ExpiryCalculator.calculate(updated, rule)
        return BottleTransitionResult.Success(updated.copy(expiresAt = expiresAt))
    }

    private fun handleMarkFed(bottle: Bottle, now: Instant): BottleTransitionResult {
        val allowed = bottle.status == BottleStatus.FEEDING_STARTED ||
            bottle.status == BottleStatus.NOT_STARTED
        if (!allowed) {
            return BottleTransitionResult.Error("Cannot mark fed from ${bottle.status}")
        }
        return BottleTransitionResult.Success(
            bottle.copy(
                status = BottleStatus.FED,
                fedAt = now,
                expiresAt = null,
                updatedAt = now,
            )
        )
    }

    private fun handleDiscard(bottle: Bottle, now: Instant): BottleTransitionResult {
        return BottleTransitionResult.Success(
            bottle.copy(
                status = BottleStatus.DISCARDED,
                discardedAt = now,
                expiresAt = null,
                updatedAt = now,
            )
        )
    }

    private fun handleCancel(bottle: Bottle, now: Instant): BottleTransitionResult {
        if (bottle.status != BottleStatus.NOT_STARTED) {
            return BottleTransitionResult.Error("Can only cancel a NotStarted bottle")
        }
        return BottleTransitionResult.Success(
            bottle.copy(
                status = BottleStatus.CANCELED,
                expiresAt = null,
                updatedAt = now,
            )
        )
    }

    private fun handleEditPreparedAt(
        bottle: Bottle,
        newTime: Instant,
        rule: GuidelineRule,
        now: Instant,
    ): BottleTransitionResult {
        val updated = bottle.copy(preparedAt = newTime, updatedAt = now)
        val expiresAt = ExpiryCalculator.calculate(updated, rule)
        return BottleTransitionResult.Success(updated.copy(expiresAt = expiresAt))
    }

    private fun handleEditAmount(bottle: Bottle, newAmountMl: Double?, now: Instant): BottleTransitionResult {
        return BottleTransitionResult.Success(
            bottle.copy(amountMl = newAmountMl, updatedAt = now)
        )
    }
}
