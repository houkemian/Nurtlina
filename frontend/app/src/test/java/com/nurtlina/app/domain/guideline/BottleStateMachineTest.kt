package com.nurtlina.app.domain.guideline

import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.BottleStatus
import com.nurtlina.app.domain.model.BottleTransition
import com.nurtlina.app.domain.model.BottleTransitionResult
import com.nurtlina.app.domain.model.GuidelineRegion
import com.nurtlina.app.domain.model.MilkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.Instant
import java.time.temporal.ChronoUnit

@RunWith(JUnit4::class)
class BottleStateMachineTest {

    private val now: Instant = Instant.parse("2024-01-01T10:00:00Z")
    private val rule = DefaultGuidelineRules.US_FORMULA

    private fun bottle(
        status: BottleStatus = BottleStatus.NOT_STARTED,
        milkType: MilkType = MilkType.FORMULA,
        feedingStartedAt: Instant? = null,
        refrigeratedAt: Instant? = null,
        expiresAt: Instant? = null,
    ) = Bottle(
        id = "bottle-1",
        babyId = "baby-1",
        milkType = milkType,
        amountMl = 120.0,
        preparedAt = now,
        feedingStartedAt = feedingStartedAt,
        refrigeratedAt = refrigeratedAt,
        status = status,
        guidelineRegion = GuidelineRegion.US,
        expiresAt = expiresAt,
        discardedAt = null,
        fedAt = null,
        note = null,
        createdAt = now,
        updatedAt = now,
    )

    private fun assertSuccess(result: BottleTransitionResult): Bottle {
        assertTrue("Expected Success but got $result", result is BottleTransitionResult.Success)
        return (result as BottleTransitionResult.Success).bottle
    }

    private fun assertError(result: BottleTransitionResult) {
        assertTrue("Expected Error but got $result", result is BottleTransitionResult.Error)
    }

    // 1. NOT_STARTED -> FEEDING_STARTED is allowed
    @Test
    fun `NOT_STARTED to FEEDING_STARTED is allowed`() {
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.NOT_STARTED),
            transition = BottleTransition.StartFeeding,
            rule = rule,
            now = now,
        )
        val updated = assertSuccess(result)
        assertEquals(BottleStatus.FEEDING_STARTED, updated.status)
    }

    // 2. NOT_STARTED -> REFRIGERATED is allowed
    @Test
    fun `NOT_STARTED to REFRIGERATED is allowed`() {
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.NOT_STARTED),
            transition = BottleTransition.Refrigerate,
            rule = rule,
            now = now,
        )
        val updated = assertSuccess(result)
        assertEquals(BottleStatus.REFRIGERATED, updated.status)
    }

    // 3. NOT_STARTED -> DISCARDED is allowed
    @Test
    fun `NOT_STARTED to DISCARDED is allowed`() {
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.NOT_STARTED),
            transition = BottleTransition.Discard,
            rule = rule,
            now = now,
        )
        val updated = assertSuccess(result)
        assertEquals(BottleStatus.DISCARDED, updated.status)
    }

    // 4. NOT_STARTED -> CANCELED is allowed
    @Test
    fun `NOT_STARTED to CANCELED is allowed`() {
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.NOT_STARTED),
            transition = BottleTransition.Cancel,
            rule = rule,
            now = now,
        )
        val updated = assertSuccess(result)
        assertEquals(BottleStatus.CANCELED, updated.status)
    }

    // 5. REFRIGERATED -> FEEDING_STARTED is allowed
    @Test
    fun `REFRIGERATED to FEEDING_STARTED is allowed`() {
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.REFRIGERATED, refrigeratedAt = now),
            transition = BottleTransition.StartFeeding,
            rule = rule,
            now = now,
        )
        val updated = assertSuccess(result)
        assertEquals(BottleStatus.FEEDING_STARTED, updated.status)
    }

    // 6. REFRIGERATED -> DISCARDED is allowed
    @Test
    fun `REFRIGERATED to DISCARDED is allowed`() {
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.REFRIGERATED, refrigeratedAt = now),
            transition = BottleTransition.Discard,
            rule = rule,
            now = now,
        )
        val updated = assertSuccess(result)
        assertEquals(BottleStatus.DISCARDED, updated.status)
    }

    // 7. FEEDING_STARTED -> FED is allowed
    @Test
    fun `FEEDING_STARTED to FED is allowed`() {
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.FEEDING_STARTED, feedingStartedAt = now),
            transition = BottleTransition.MarkFed,
            rule = rule,
            now = now,
        )
        val updated = assertSuccess(result)
        assertEquals(BottleStatus.FED, updated.status)
    }

    // 8. FEEDING_STARTED -> DISCARDED is allowed
    @Test
    fun `FEEDING_STARTED to DISCARDED is allowed`() {
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.FEEDING_STARTED, feedingStartedAt = now),
            transition = BottleTransition.Discard,
            rule = rule,
            now = now,
        )
        val updated = assertSuccess(result)
        assertEquals(BottleStatus.DISCARDED, updated.status)
    }

    // 9. EXPIRED -> DISCARDED is allowed
    @Test
    fun `EXPIRED to DISCARDED is allowed`() {
        val expiresAt = now.minus(5, ChronoUnit.MINUTES)
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.EXPIRED, expiresAt = expiresAt),
            transition = BottleTransition.Discard,
            rule = rule,
            now = now,
        )
        val updated = assertSuccess(result)
        assertEquals(BottleStatus.DISCARDED, updated.status)
    }

    // 10. FED is terminal (no transitions allowed)
    @Test
    fun `FED is terminal and rejects any transition`() {
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.FED),
            transition = BottleTransition.Discard,
            rule = rule,
            now = now,
        )
        assertError(result)
    }

    // 11. DISCARDED is terminal
    @Test
    fun `DISCARDED is terminal and rejects any transition`() {
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.DISCARDED),
            transition = BottleTransition.StartFeeding,
            rule = rule,
            now = now,
        )
        assertError(result)
    }

    // 12. CANCELED is terminal
    @Test
    fun `CANCELED is terminal and rejects any transition`() {
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.CANCELED),
            transition = BottleTransition.StartFeeding,
            rule = rule,
            now = now,
        )
        assertError(result)
    }

    // 13. After StartFeeding, feedingStartedAt is set
    @Test
    fun `after StartFeeding feedingStartedAt is set to now`() {
        val transitionTime = now.plus(15, ChronoUnit.MINUTES)
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.NOT_STARTED),
            transition = BottleTransition.StartFeeding,
            rule = rule,
            now = transitionTime,
        )
        val updated = assertSuccess(result)
        assertEquals(transitionTime, updated.feedingStartedAt)
    }

    // 14. After Refrigerate, refrigeratedAt is set
    @Test
    fun `after Refrigerate refrigeratedAt is set to now`() {
        val transitionTime = now.plus(10, ChronoUnit.MINUTES)
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.NOT_STARTED),
            transition = BottleTransition.Refrigerate,
            rule = rule,
            now = transitionTime,
        )
        val updated = assertSuccess(result)
        assertEquals(transitionTime, updated.refrigeratedAt)
    }

    // 15. Editing preparedAt recalculates expiresAt
    @Test
    fun `editing preparedAt recalculates expiresAt`() {
        val originalPreparedAt = now
        val newPreparedAt = now.plus(30, ChronoUnit.MINUTES)
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.NOT_STARTED),
            transition = BottleTransition.EditPreparedAt(newPreparedAt),
            rule = rule,
            now = now,
        )
        val updated = assertSuccess(result)
        assertEquals(newPreparedAt, updated.preparedAt)
        // expiresAt should now be newPreparedAt + 2h (120 min), not originalPreparedAt + 2h
        val expectedExpiry = newPreparedAt.plus(120, ChronoUnit.MINUTES)
        assertEquals(expectedExpiry, updated.expiresAt)
    }

    @Test
    fun `REFRIGERATED cannot be canceled`() {
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.REFRIGERATED, refrigeratedAt = now),
            transition = BottleTransition.Cancel,
            rule = rule,
            now = now,
        )
        assertError(result)
    }

    @Test
    fun `after MarkFed expiresAt is null`() {
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.FEEDING_STARTED, feedingStartedAt = now),
            transition = BottleTransition.MarkFed,
            rule = rule,
            now = now,
        )
        val updated = assertSuccess(result)
        assertNull(updated.expiresAt)
    }

    @Test
    fun `after Discard expiresAt is null`() {
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.NOT_STARTED),
            transition = BottleTransition.Discard,
            rule = rule,
            now = now,
        )
        val updated = assertSuccess(result)
        assertNull(updated.expiresAt)
    }

    @Test
    fun `StartFeeding sets correct expiresAt from feedingStartedAt`() {
        val feedStart = now.plus(20, ChronoUnit.MINUTES)
        val result = BottleStateMachine.transition(
            bottle = bottle(status = BottleStatus.NOT_STARTED),
            transition = BottleTransition.StartFeeding,
            rule = rule,
            now = feedStart,
        )
        val updated = assertSuccess(result)
        val expectedExpiry = feedStart.plus(60, ChronoUnit.MINUTES)
        assertEquals(expectedExpiry, updated.expiresAt)
    }
}
