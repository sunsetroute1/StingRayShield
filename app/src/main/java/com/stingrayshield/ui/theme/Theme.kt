package com.stingrayshield.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Custom Stingray color schemes
private val StingrayLightColorScheme = lightColorScheme(
    primary = stingray_light_primary,
    onPrimary = stingray_light_onPrimary,
    primaryContainer = stingray_light_primaryContainer,
    onPrimaryContainer = stingray_light_onPrimaryContainer,
    secondary = stingray_light_secondary,
    onSecondary = stingray_light_onSecondary,
    secondaryContainer = stingray_light_secondaryContainer,
    onSecondaryContainer = stingray_light_onSecondaryContainer,
    tertiary = stingray_light_tertiary,
    onTertiary = stingray_light_onTertiary,
    tertiaryContainer = stingray_light_tertiaryContainer,
    onTertiaryContainer = stingray_light_onTertiaryContainer,
    error = stingray_light_error,
    errorContainer = stingray_light_errorContainer,
    onError = stingray_light_onError,
    onErrorContainer = stingray_light_onErrorContainer,
    background = stingray_light_background,
    onBackground = stingray_light_onBackground,
    surface = stingray_light_surface,
    onSurface = stingray_light_onSurface,
    surfaceVariant = stingray_light_surfaceVariant,
    onSurfaceVariant = stingray_light_onSurfaceVariant,
    outline = stingray_light_outline,
    inverseOnSurface = stingray_light_inverseOnSurface,
    inverseSurface = stingray_light_inverseSurface,
    inversePrimary = stingray_light_inversePrimary,
    surfaceTint = stingray_light_surfaceTint,
    outlineVariant = stingray_light_outlineVariant,
    scrim = stingray_light_scrim,
)

private val StingrayDarkColorScheme = darkColorScheme(
    primary = stingray_dark_primary,
    onPrimary = stingray_dark_onPrimary,
    primaryContainer = stingray_dark_primaryContainer,
    onPrimaryContainer = stingray_dark_onPrimaryContainer,
    secondary = stingray_dark_secondary,
    onSecondary = stingray_dark_onSecondary,
    secondaryContainer = stingray_dark_secondaryContainer,
    onSecondaryContainer = stingray_dark_onSecondaryContainer,
    tertiary = stingray_dark_tertiary,
    onTertiary = stingray_dark_onTertiary,
    tertiaryContainer = stingray_dark_tertiaryContainer,
    onTertiaryContainer = stingray_dark_onTertiaryContainer,
    error = stingray_dark_error,
    errorContainer = stingray_dark_errorContainer,
    onError = stingray_dark_onError,
    onErrorContainer = stingray_dark_onErrorContainer,
    background = stingray_dark_background,
    onBackground = stingray_dark_onBackground,
    surface = stingray_dark_surface,
    onSurface = stingray_dark_onSurface,
    surfaceVariant = stingray_dark_surfaceVariant,
    onSurfaceVariant = stingray_dark_onSurfaceVariant,
    outline = stingray_dark_outline,
    inverseOnSurface = stingray_dark_inverseOnSurface,
    inverseSurface = stingray_dark_inverseSurface,
    inversePrimary = stingray_dark_inversePrimary,
    surfaceTint = stingray_dark_surfaceTint,
    outlineVariant = stingray_dark_outlineVariant,
    scrim = stingray_dark_scrim,
)

// Light theme colors
private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

// Dark theme colors
private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)

@Composable
fun StingrayShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    StingrayShieldTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        useCustomColors = false,
        content = content
    )
}

@Composable
fun StingrayShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    useCustomColors: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        useCustomColors -> {
            // Custom stingray-themed colors
            if (darkTheme) StingrayDarkColorScheme else StingrayLightColorScheme
        }
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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
