package com.nurtlina.app.domain.guideline

import com.nurtlina.app.domain.model.GuidelineRegion
import com.nurtlina.app.domain.model.MilkType

/**
 * Versioned guideline rules for bottle expiry calculation.
 * Based on public health sources (CDC, AAP, NHS). Not medical advice.
 * Rule version must be incremented when durations change.
 */
data class GuidelineRule(
    val id: String,
    val version: Int,
    val region: GuidelineRegion,
    val milkType: MilkType,
    val roomTempMinutes: Int?,
    val feedingStartedMinutes: Int?,
    val refrigeratedMinutes: Int?,
    val sourceName: String,
    val sourceUrl: String,
)

object DefaultGuidelineRules {

    const val RULE_VERSION = 1

    val US_FORMULA = GuidelineRule(
        id = "us_formula_v1",
        version = RULE_VERSION,
        region = GuidelineRegion.US,
        milkType = MilkType.FORMULA,
        roomTempMinutes = 120,
        feedingStartedMinutes = 60,
        refrigeratedMinutes = 24 * 60,
        sourceName = "CDC: Infant Formula Preparation and Storage",
        sourceUrl = "https://www.cdc.gov/infant-toddler-nutrition/formula-feeding/preparation-and-storage.html",
    )

    val US_BREAST_MILK = GuidelineRule(
        id = "us_breast_milk_v1",
        version = RULE_VERSION,
        region = GuidelineRegion.US,
        milkType = MilkType.BREAST_MILK,
        roomTempMinutes = 240,
        feedingStartedMinutes = null,
        refrigeratedMinutes = 4 * 24 * 60,
        sourceName = "CDC: Breast Milk Storage and Preparation",
        sourceUrl = "https://www.cdc.gov/breastfeeding/breast-milk-preparation-and-storage/handling-breastmilk.html",
    )

    val UK_FORMULA = GuidelineRule(
        id = "uk_formula_v1",
        version = RULE_VERSION,
        region = GuidelineRegion.UK,
        milkType = MilkType.FORMULA,
        roomTempMinutes = 120,
        feedingStartedMinutes = 60,
        refrigeratedMinutes = 24 * 60,
        sourceName = "NHS: How to make up baby formula",
        sourceUrl = "https://www.nhs.uk/baby/breastfeeding-and-bottle-feeding/bottle-feeding/making-up-baby-formula/",
    )

    val UK_BREAST_MILK = GuidelineRule(
        id = "uk_breast_milk_v1",
        version = RULE_VERSION,
        region = GuidelineRegion.UK,
        milkType = MilkType.BREAST_MILK,
        roomTempMinutes = 240,
        feedingStartedMinutes = null,
        refrigeratedMinutes = 8 * 24 * 60,
        sourceName = "NHS: Expressing and storing breast milk",
        sourceUrl = "https://www.nhs.uk/conditions/baby/breastfeeding-and-bottle-feeding/breastfeeding/expressing-breast-milk/",
    )

    fun forRegionAndType(region: GuidelineRegion, milkType: MilkType): GuidelineRule {
        return when {
            region == GuidelineRegion.US && milkType == MilkType.FORMULA -> US_FORMULA
            region == GuidelineRegion.US && milkType == MilkType.BREAST_MILK -> US_BREAST_MILK
            region == GuidelineRegion.UK && milkType == MilkType.FORMULA -> UK_FORMULA
            region == GuidelineRegion.UK && milkType == MilkType.BREAST_MILK -> UK_BREAST_MILK
            else -> US_FORMULA
        }
    }

    fun allRules(): List<GuidelineRule> = listOf(US_FORMULA, US_BREAST_MILK, UK_FORMULA, UK_BREAST_MILK)
}
