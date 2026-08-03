package com.baicaohui.lightweb.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

@Composable
fun BchTheme(
    config: ThemeConfig,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (config.darkMode) {
        DarkMode.SYSTEM -> isSystemInDarkTheme()
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
    val context = LocalContext.current
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    val colorScheme = when {
        config.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> TonalPaletteGenerator.darkScheme(Color(config.seedColor))
        else -> TonalPaletteGenerator.lightScheme(Color(config.seedColor))
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = bchTypography(config.fontScale),
        shapes = bchShapes(config.shapeStyle),
        content = content,
    )
}
