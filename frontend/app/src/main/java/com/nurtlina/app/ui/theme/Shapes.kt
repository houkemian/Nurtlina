package com.nurtlina.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val NurtlinaShapes = Shapes(
    // Extra small – chips, small badges, input fields
    extraSmall = RoundedCornerShape(4.dp),
    // Small – compact cards, snackbars
    small = RoundedCornerShape(8.dp),
    // Medium – dialogs, bottom sheets, modals
    medium = RoundedCornerShape(12.dp),
    // Large – bottle cards, main content panels
    large = RoundedCornerShape(16.dp),
    // Extra large – full-width cards, paywall sheets
    extraLarge = RoundedCornerShape(24.dp),
)
