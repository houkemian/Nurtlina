package com.nurtlina.app.core.time

import com.nurtlina.app.domain.model.UnitType
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConversionTest {

    @Test
    fun `formats amount in milliliters without decimals`() {
        assertEquals("120 ml", TimeFormatter.formatAmount(120.0, UnitType.ML))
        assertEquals("89 ml", TimeFormatter.formatAmount(89.6, UnitType.ML))
    }

    @Test
    fun `converts milliliters to ounces with one decimal`() {
        // 120 ml / 29.5735 ≈ 4.1 oz
        assertEquals("4.1 oz", TimeFormatter.formatAmount(120.0, UnitType.OZ))
        // 240 ml ≈ 8.1 oz
        assertEquals("8.1 oz", TimeFormatter.formatAmount(240.0, UnitType.OZ))
    }

    @Test
    fun `returns dash for a null amount`() {
        assertEquals("-", TimeFormatter.formatAmount(null, UnitType.ML))
        assertEquals("-", TimeFormatter.formatAmount(null, UnitType.OZ))
    }
}
