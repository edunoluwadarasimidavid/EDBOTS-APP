package com.example.ui.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = EdBotGreen,
    onPrimary = Color(0xFF04121F),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = EdBotCyan,
    onSecondary = Color(0xFF00363A),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = EdBotBlue,
    background = EdBotDarkBg,
    onBackground = EdBotTextPrimaryDark,
    surface = EdBotDarkSurface,
    onSurface = EdBotTextPrimaryDark,
    surfaceVariant = EdBotDarkSurfaceVariant,
    onSurfaceVariant = EdBotTextSecondaryDark,
    outline = EdBotDarkBorder,
    error = EdBotStatusDisconnected,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = EdBotGreenDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF14532D),
    secondary = EdBotBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF075985),
    tertiary = EdBotCyan,
    background = EdBotLightBg,
    onBackground = EdBotTextPrimaryLight,
    surface = EdBotLightSurface,
    onSurface = EdBotTextPrimaryLight,
    surfaceVariant = EdBotLightSurfaceVariant,
    onSurfaceVariant = EdBotTextSecondaryLight,
    outline = EdBotLightBorder,
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun EdBotsTheme(
    darkTheme: Boolean = true, // Default to sleek tech dark theme
    dynamicColor: Boolean = false, // Keep consistent branding colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
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
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
