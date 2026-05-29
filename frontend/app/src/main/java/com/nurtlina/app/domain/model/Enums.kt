package com.nurtlina.app.domain.model

enum class MilkType {
    FORMULA,
    BREAST_MILK,
    CUSTOM,
}

enum class BottleStatus {
    NOT_STARTED,
    FEEDING_STARTED,
    REFRIGERATED,
    EXPIRED,
    FED,
    DISCARDED,
    CANCELED,
    ;

    val isTerminal: Boolean
        get() = this == FED || this == DISCARDED || this == CANCELED
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
