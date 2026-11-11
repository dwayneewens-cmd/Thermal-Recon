package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ThermalDarkColorScheme = darkColorScheme(
    primary = ThermalAmber,
    onPrimary = DeepObsidian,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = ThermalCoreYellow,
    secondary = ThermalColdCyan,
    onSecondary = DeepObsidian,
    secondaryContainer = SurfaceDark,
    onSecondaryContainer = ThermalColdCyan,
    tertiary = ThermalHotRed,
    onTertiary = TextPrimary,
    background = DeepObsidian,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = ThermalHotRed,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent thermal HUD aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = ThermalDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DeepObsidian.toArgb()
                window.navigationBarColor = DeepObsidian.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
