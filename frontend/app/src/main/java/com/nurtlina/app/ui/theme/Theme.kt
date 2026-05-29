package com.nurtlina.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nurtlina.app.domain.model.BottleStatus

// --------------------------------------------------------------------------
// Light color scheme
// --------------------------------------------------------------------------
private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,

    secondary = Peach40,
    onSecondary = Color.White,
    secondaryContainer = Peach90,
    onSecondaryContainer = Peach10,

    tertiary = StatusFedLight,
    onTertiary = Color.White,
    tertiaryContainer = StatusFedContainerLight,
    onTertiaryContainer = Color(0xFF0A3C0D),

    error = Error40,
    onError = Color.White,
    errorContainer = Error90,
    onErrorContainer = Error10,

    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    scrim = Color.Black,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    inversePrimary = Teal80,
)

// --------------------------------------------------------------------------
// Dark color scheme
// --------------------------------------------------------------------------
private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,

    secondary = Peach80,
    onSecondary = Peach20,
    secondaryContainer = Peach30,
    onSecondaryContainer = Peach90,

    tertiary = StatusFedDark,
    onTertiary = Color(0xFF0A3C0D),
    tertiaryContainer = StatusFedContainerDark,
    onTertiaryContainer = StatusFedDark,

    error = Error80,
    onError = Error20,
    errorContainer = Error30,
    onErrorContainer = Error90,

    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    scrim = Color.Black,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    inversePrimary = Teal40,
)

// --------------------------------------------------------------------------
// NurtlinaTheme
// --------------------------------------------------------------------------

/**
 * Main app theme. Wraps [MaterialTheme] with Nurtlina branding.
 *
 * @param darkTheme      Whether to apply the dark color scheme.
 * @param dynamicColor   Use Android 12+ dynamic color. Falls back to
 *                       Nurtlina palette on older devices.
 */
@Composable
fun NurtlinaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Make status bar transparent and use edge-to-edge rendering.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NurtlinaTypography,
        shapes = NurtlinaShapes,
        content = content,
    )
}

// --------------------------------------------------------------------------
// BottleStatusColors – semantic color tokens per BottleStatus
// --------------------------------------------------------------------------

/**
 * Provides content color and container color for each [BottleStatus].
 * Use [contentColor] for text/icons and [containerColor] for card backgrounds.
 */
data class StatusColorPair(
    val contentColor: Color,
    val containerColor: Color,
)

object BottleStatusColors {

    @Composable
    fun colorsFor(status: BottleStatus, darkTheme: Boolean = isSystemInDarkTheme()): StatusColorPair =
        when (status) {
            BottleStatus.NOT_STARTED -> StatusColorPair(
                contentColor = if (darkTheme) StatusNotStartedDark else StatusNotStartedLight,
                containerColor = if (darkTheme) StatusNotStartedContainerDark else StatusNotStartedContainerLight,
            )
            BottleStatus.FEEDING_STARTED -> StatusColorPair(
                contentColor = if (darkTheme) StatusFeedingStartedDark else StatusFeedingStartedLight,
                containerColor = if (darkTheme) StatusFeedingStartedContainerDark else StatusFeedingStartedContainerLight,
            )
            BottleStatus.REFRIGERATED -> StatusColorPair(
                contentColor = if (darkTheme) StatusRefrigeratedDark else StatusRefrigeratedLight,
                containerColor = if (darkTheme) StatusRefrigeratedContainerDark else StatusRefrigeratedContainerLight,
            )
            BottleStatus.EXPIRED -> StatusColorPair(
                contentColor = if (darkTheme) StatusExpiredDark else StatusExpiredLight,
                containerColor = if (darkTheme) StatusExpiredContainerDark else StatusExpiredContainerLight,
            )
            BottleStatus.FED -> StatusColorPair(
                contentColor = if (darkTheme) StatusFedDark else StatusFedLight,
                containerColor = if (darkTheme) StatusFedContainerDark else StatusFedContainerLight,
            )
            BottleStatus.DISCARDED -> StatusColorPair(
                contentColor = if (darkTheme) StatusDiscardedDark else StatusDiscardedLight,
                containerColor = if (darkTheme) StatusDiscardedContainerDark else StatusDiscardedContainerLight,
            )
            BottleStatus.CANCELED -> StatusColorPair(
                contentColor = if (darkTheme) StatusCanceledDark else StatusCanceledLight,
                containerColor = if (darkTheme) StatusCanceledContainerDark else StatusCanceledContainerLight,
            )
        }

    /** Convenience: content color only. */
    @Composable
    fun contentColorFor(status: BottleStatus, darkTheme: Boolean = isSystemInDarkTheme()): Color =
        colorsFor(status, darkTheme).contentColor

    /** Convenience: container/background color only. */
    @Composable
    fun containerColorFor(status: BottleStatus, darkTheme: Boolean = isSystemInDarkTheme()): Color =
        colorsFor(status, darkTheme).containerColor
}
