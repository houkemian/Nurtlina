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

// --------------------------------------------------------------------------
// Light color scheme
// --------------------------------------------------------------------------
private val LightColorScheme = lightColorScheme(
    primary = MistBlueDarkContainer,
    onPrimary = Color.White,
    primaryContainer = MistBlueLight,
    onPrimaryContainer = DeepGray,

    secondary = SoftSage,
    onSecondary = DeepGray,
    secondaryContainer = SoftSageLight,
    onSecondaryContainer = DeepGray,

    tertiary = SoftSageDark,
    onTertiary = Color.White,
    tertiaryContainer = StatusFedContainerLight,
    onTertiaryContainer = DeepGray,

    error = Error40,
    onError = Color.White,
    errorContainer = Error90,
    onErrorContainer = Error10,

    background = WarmGreige,
    onBackground = DeepGray,
    surface = WarmGreigeLight,
    onSurface = DeepGray,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = DeepGray,
    outline = DeepGray,
    outlineVariant = NeutralVariant80,
    scrim = DeepGray,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    inversePrimary = MistBlueDark,
)

// --------------------------------------------------------------------------
// Dark color scheme
// --------------------------------------------------------------------------
private val DarkColorScheme = darkColorScheme(
    primary = MistBlue,
    onPrimary = DeepGray,
    primaryContainer = MistBlueDarkContainer,
    onPrimaryContainer = MistBlueLight,

    secondary = SoftSage,
    onSecondary = DeepGray,
    secondaryContainer = SoftSageDarkContainer,
    onSecondaryContainer = SoftSageLight,

    tertiary = SoftSage,
    onTertiary = DeepGray,
    tertiaryContainer = StatusFedContainerDark,
    onTertiaryContainer = StatusFedDark,

    error = Error80,
    onError = Error20,
    errorContainer = Error30,
    onErrorContainer = Error90,

    background = Color(0xFF172229),
    onBackground = Neutral90,
    surface = Color(0xFF1F2C33),
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant20,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    scrim = DeepGray,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    inversePrimary = MistBlueDark,
)

// --------------------------------------------------------------------------
// NurtlinaTheme
// --------------------------------------------------------------------------

/**
 * Main app theme. Wraps [MaterialTheme] with Nurtlina branding.
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
