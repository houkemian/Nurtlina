package com.nurtlina.app.ui.theme

import androidx.compose.ui.graphics.Color

// --------------------------------------------------------------------------
// Brand palette - mist blue, warm greige, soft sage, gentle deep gray
// --------------------------------------------------------------------------
val MistBlue = Color(0xFFB8D4E3)
val MistBlueLight = Color(0xFFE6F1F6)
val MistBlueDark = Color(0xFF5E7F90)
val MistBlueDarkContainer = Color(0xFF29424F)

val WarmGreige = Color(0xFFF2E8CF)
val WarmGreigeLight = Color(0xFFFFF8EA)
val WarmGreigeDark = Color(0xFFC8B991)

val SoftSage = Color(0xFFA7C7B5)
val SoftSageLight = Color(0xFFE4F0EA)
val SoftSageDark = Color(0xFF5E7F6D)
val SoftSageDarkContainer = Color(0xFF274236)

val DeepGray = Color(0xFF2F3E46)

// --------------------------------------------------------------------------
// Neutral / surface tones
// --------------------------------------------------------------------------
val Neutral10 = DeepGray
val Neutral20 = Color(0xFF3D4C54)
val Neutral30 = Color(0xFF536269)
val Neutral40 = Color(0xFF68777F)
val Neutral50 = Color(0xFF7F8E96)
val Neutral60 = Color(0xFF98A5AB)
val Neutral70 = Color(0xFFB1BCC1)
val Neutral80 = Color(0xFFCBD5D9)
val Neutral90 = Color(0xFFE5EDF0)
val Neutral95 = Color(0xFFF1F6F8)
val Neutral99 = WarmGreigeLight

val NeutralVariant10 = DeepGray
val NeutralVariant20 = Color(0xFF405159)
val NeutralVariant30 = Color(0xFF58686F)
val NeutralVariant40 = Color(0xFF708086)
val NeutralVariant50 = Color(0xFF89979D)
val NeutralVariant60 = Color(0xFFA2AFB4)
val NeutralVariant70 = Color(0xFFBCC8CC)
val NeutralVariant80 = Color(0xFFD5E1E5)
val NeutralVariant90 = MistBlueLight
val NeutralVariant95 = Color(0xFFF0F7FA)
val NeutralVariant99 = Color(0xFFFBFEFF)

// --------------------------------------------------------------------------
// Error tones (Material 3 standard)
// --------------------------------------------------------------------------
val Error10 = Color(0xFF410002)
val Error20 = Color(0xFF690005)
val Error30 = Color(0xFF93000A)
val Error40 = Color(0xFFBA1A1A)
val Error80 = Color(0xFFFFB4AB)
val Error90 = Color(0xFFFFDAD6)

// --------------------------------------------------------------------------
// Bottle status semantic colors
// --------------------------------------------------------------------------

// NOT_STARTED - mist blue, calm and untouched
val StatusNotStartedLight = MistBlueDark
val StatusNotStartedDark = MistBlue
val StatusNotStartedContainerLight = MistBlueLight
val StatusNotStartedContainerDark = MistBlueDarkContainer

// FEEDING_STARTED – warm amber/orange (active, in progress)
val StatusFeedingStartedLight = Color(0xFFD07A00)
val StatusFeedingStartedDark = Color(0xFFFFB74D)
val StatusFeedingStartedContainerLight = Color(0xFFFFEDD0)
val StatusFeedingStartedContainerDark = Color(0xFF4A2A00)

// REFRIGERATED – cool blue (cold storage)
val StatusRefrigeratedLight = Color(0xFF1565C0)
val StatusRefrigeratedDark = Color(0xFF82B1FF)
val StatusRefrigeratedContainerLight = Color(0xFFDBEAFF)
val StatusRefrigeratedContainerDark = Color(0xFF0D2F60)

// EXPIRED – alert red (timer has run out)
val StatusExpiredLight = Color(0xFFBA1A1A)
val StatusExpiredDark = Color(0xFFFFB4AB)
val StatusExpiredContainerLight = Color(0xFFFFDAD6)
val StatusExpiredContainerDark = Color(0xFF690005)

// FED - soft sage accent
val StatusFedLight = SoftSageDark
val StatusFedDark = SoftSage
val StatusFedContainerLight = SoftSageLight
val StatusFedContainerDark = SoftSageDarkContainer

// DISCARDED – neutral gray (removed/thrown away)
val StatusDiscardedLight = Neutral40
val StatusDiscardedDark = Neutral70
val StatusDiscardedContainerLight = Neutral90
val StatusDiscardedContainerDark = Neutral20

// CANCELED – muted purple-gray (voided)
val StatusCanceledLight = Color(0xFF6B6176)
val StatusCanceledDark = Color(0xFFBFB5CC)
val StatusCanceledContainerLight = Color(0xFFECE4F5)
val StatusCanceledContainerDark = Color(0xFF2E2535)

// --------------------------------------------------------------------------
// Warning – expiring soon (not yet expired but close)
// --------------------------------------------------------------------------
val WarningSoonLight = Color(0xFFF57C00)
val WarningSoonDark = Color(0xFFFFCC80)
val WarningSoonContainerLight = Color(0xFFFFF3E0)
val WarningSoonContainerDark = Color(0xFF4E2800)
