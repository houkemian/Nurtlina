package com.nurtlina.app.domain.guideline

import com.nurtlina.app.domain.model.Bottle
import com.nurtlina.app.domain.model.BottleStatus
import com.nurtlina.app.domain.model.GuidelineRegion
import com.nurtlina.app.domain.model.MilkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.Instant
import java.time.temporal.ChronoUnit

@RunWith(JUnit4::class)
class ExpiryCalculatorTest {

    private val now: Instant = Instant.parse("2024-01-01T10:00:00Z")

    private fun baseFormulaBottle(
        status: BottleStatus = BottleStatus.NOT_STARTED,
        feedingStartedAt: Instant? = null,
        refrigeratedAt: Instant? = null,
        expiresAt: Instant? = null,
    ) = Bottle(
        id = "test-bottle-1",
        babyId = "baby-1",
        milkType = MilkType.FORMULA,
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

    private fun baseBreastMilkBottle(
        status: BottleStatus = BottleStatus.NOT_STARTED,
        feedingStartedAt: Instant? = null,
        refrigeratedAt: Instant? = null,
        expiresAt: Instant? = null,
    ) = Bottle(
        id = "test-bottle-2",
        babyId = "baby-1",
        milkType = MilkType.BREAST_MILK,
        amountMl = 90.0,
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

    // 1. Formula NOT_STARTED at room temp: expires preparedAt + 2 hours
    @Test
    fun `formula NOT_STARTED at room temp expires at preparedAt plus 2 hours`() {
        val bottle = baseFormulaBottle(status = BottleStatus.NOT_STARTED)
        val rule = DefaultGuidelineRules.US_FORMULA

        val result = ExpiryCalculator.calculate(bottle, rule)

        val expected = now.plus(120, ChronoUnit.MINUTES)
        assertEquals(expected, result)
    }

    // 2. Formula FEEDING_STARTED: expires feedingStartedAt + 1 hour
    @Test
    fun `formula FEEDING_STARTED expires at feedingStartedAt plus 1 hour`() {
        val feedStart = now.plus(30, ChronoUnit.MINUTES)
        val bottle = baseFormulaBottle(
            status = BottleStatus.FEEDING_STARTED,
            feedingStartedAt = feedStart,
        )
        val rule = DefaultGuidelineRules.US_FORMULA

        val result = ExpiryCalculator.calculate(bottle, rule)

        val expected = feedStart.plus(60, ChronoUnit.MINUTES)
        assertEquals(expected, result)
    }

    // 3. Formula REFRIGERATED: expires preparedAt + 24 hours
    @Test
    fun `formula REFRIGERATED expires at preparedAt plus 24 hours`() {
        val bottle = baseFormulaBottle(
            status = BottleStatus.REFRIGERATED,
            refrigeratedAt = now,
        )
        val rule = DefaultGuidelineRules.US_FORMULA

        val result = ExpiryCalculator.calculate(bottle, rule)

        val expected = now.plus(24 * 60, ChronoUnit.MINUTES)
        assertEquals(expected, result)
    }

    // 4. Breast milk NOT_STARTED at room temp: expires preparedAt + 4 hours
    @Test
    fun `breast milk NOT_STARTED at room temp expires at preparedAt plus 4 hours`() {
        val bottle = baseBreastMilkBottle(status = BottleStatus.NOT_STARTED)
        val rule = DefaultGuidelineRules.US_BREAST_MILK

        val result = ExpiryCalculator.calculate(bottle, rule)

        val expected = now.plus(240, ChronoUnit.MINUTES)
        assertEquals(expected, result)
    }

    // 5. Breast milk REFRIGERATED: expires preparedAt + 4 days
    @Test
    fun `breast milk REFRIGERATED expires at preparedAt plus 4 days`() {
        val bottle = baseBreastMilkBottle(
            status = BottleStatus.REFRIGERATED,
            refrigeratedAt = now,
        )
        val rule = DefaultGuidelineRules.US_BREAST_MILK

        val result = ExpiryCalculator.calculate(bottle, rule)

        val expected = now.plus(4 * 24 * 60, ChronoUnit.MINUTES)
        assertEquals(expected, result)
    }

    // 6. Fed bottle: returns null
    @Test
    fun `FED bottle returns null expiry`() {
        val bottle = baseFormulaBottle(status = BottleStatus.FED)
        val rule = DefaultGuidelineRules.US_FORMULA

        val result = ExpiryCalculator.calculate(bottle, rule)

        assertNull(result)
    }

    // 7. Discarded bottle: returns null
    @Test
    fun `DISCARDED bottle returns null expiry`() {
        val bottle = baseFormulaBottle(status = BottleStatus.DISCARDED)
        val rule = DefaultGuidelineRules.US_FORMULA

        val result = ExpiryCalculator.calculate(bottle, rule)

        assertNull(result)
    }

    // 8. Canceled bottle: returns null
    @Test
    fun `CANCELED bottle returns null expiry`() {
        val bottle = baseFormulaBottle(status = BottleStatus.CANCELED)
        val rule = DefaultGuidelineRules.US_FORMULA

        val result = ExpiryCalculator.calculate(bottle, rule)

        assertNull(result)
    }

    // 9. isExpired returns true when now is after expiresAt
    @Test
    fun `isExpired returns true when now is after expiresAt`() {
        val expiresAt = now.minus(1, ChronoUnit.MINUTES)
        val checkTime = now

        assertTrue(ExpiryCalculator.isExpired(expiresAt, checkTime))
    }

    // 10. isExpired returns false when now is before expiresAt
    @Test
    fun `isExpired returns false when now is before expiresAt`() {
        val expiresAt = now.plus(1, ChronoUnit.MINUTES)
        val checkTime = now

        assertFalse(ExpiryCalculator.isExpired(expiresAt, checkTime))
    }

    @Test
    fun `isExpired returns false when expiresAt is null`() {
        assertFalse(ExpiryCalculator.isExpired(null, now))
    }

    @Test
    fun `minutesUntilExpiry returns correct positive value`() {
        val expiresAt = now.plus(45, ChronoUnit.MINUTES)
        val minutes = ExpiryCalculator.minutesUntilExpiry(expiresAt, now)
        assertEquals(45L, minutes)
    }

    @Test
    fun `minutesUntilExpiry returns negative when already expired`() {
        val expiresAt = now.minus(10, ChronoUnit.MINUTES)
        val minutes = ExpiryCalculator.minutesUntilExpiry(expiresAt, now)
        assertTrue(minutes < 0)
    }

    @Test
    fun `expiresAt calculation uses only local bottle and rule inputs`() {
        val bottle = baseFormulaBottle(status = BottleStatus.NOT_STARTED)

        val result = ExpiryCalculator.calculate(bottle, DefaultGuidelineRules.US_FORMULA)

        assertEquals(now.plus(120, ChronoUnit.MINUTES), result)
    }
}
