package com.nurtlina.app.domain.model

enum class MilkType {
    FORMULA,
    BREAST_MILK,
    CUSTOM,
}

enum class FeedType {
    FORMULA,
    BREAST_MILK,
    MIXED,
    NURSING,
    OTHER,
}

enum class DiaperType {
    WET,
    DIRTY,
    MIXED,
    DRY,
}

enum class GuidelineRegion {
    US,
    UK,
    CUSTOM,
}

enum class UnitType {
    ML,
    OZ,
}

enum class ThemeType {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class WidgetTheme {
    DEFAULT,
    SAGE,
    LAVENDER,
    ROSE,
    SLATE,
}
