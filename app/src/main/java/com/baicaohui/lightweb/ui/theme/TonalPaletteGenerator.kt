package com.baicaohui.lightweb.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * 从 seed 颜色生成近似 MD3 tonal palette 的 ColorScheme。
 * 实现为 HSV 变换（饱和度缩放 + 明度阶梯），Android 12+ 动态取色生效时不会走到这里。
 */
object TonalPaletteGenerator {

    fun lightScheme(seed: Color): ColorScheme {
        val hsv = hsvOf(seed)
        fun tone(satScale: Float, value: Float): Color =
            fromHsv(hsv[0], hsv[1] * satScale, value)

        return lightColorScheme(
            primary = seed,
            onPrimary = Color.White,
            primaryContainer = tone(0.8f, 0.92f),
            onPrimaryContainer = tone(0.9f, 0.18f),
            secondary = tone(0.5f, 0.55f),
            onSecondary = Color.White,
            secondaryContainer = tone(0.4f, 0.90f),
            onSecondaryContainer = tone(0.5f, 0.20f),
            tertiary = tone(0.7f, 0.62f),
            onTertiary = Color.White,
            tertiaryContainer = tone(0.6f, 0.90f),
            onTertiaryContainer = tone(0.7f, 0.20f),
            error = Color(0xFFB3261E),
            onError = Color.White,
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
            background = Color(0xFFFDFBFF),
            onBackground = Color(0xFF1A1B20),
            surface = Color(0xFFFDFBFF),
            onSurface = Color(0xFF1A1B20),
            surfaceVariant = Color(0xFFE7E0EC),
            onSurfaceVariant = Color(0xFF49454F),
            outline = Color(0xFF79747E),
        )
    }

    fun darkScheme(seed: Color): ColorScheme {
        val hsv = hsvOf(seed)
        fun tone(satScale: Float, value: Float): Color =
            fromHsv(hsv[0], hsv[1] * satScale, value)

        return darkColorScheme(
            primary = tone(0.9f, 0.85f),
            onPrimary = Color(0xFF00315A),
            primaryContainer = tone(0.9f, 0.35f),
            onPrimaryContainer = tone(0.8f, 0.92f),
            secondary = tone(0.5f, 0.80f),
            onSecondary = Color(0xFF00332B),
            secondaryContainer = tone(0.5f, 0.30f),
            onSecondaryContainer = tone(0.4f, 0.90f),
            tertiary = tone(0.7f, 0.80f),
            onTertiary = Color(0xFF3F0030),
            tertiaryContainer = tone(0.7f, 0.30f),
            onTertiaryContainer = tone(0.6f, 0.90f),
            error = Color(0xFFF2B8B5),
            onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = Color(0xFFF9DEDC),
            background = Color(0xFF121316),
            onBackground = Color(0xFFE6E1E5),
            surface = Color(0xFF121316),
            onSurface = Color(0xFFE6E1E5),
            surfaceVariant = Color(0xFF49454F),
            onSurfaceVariant = Color(0xFFCAC4D0),
            outline = Color(0xFF938F99),
        )
    }

    private fun hsvOf(color: Color): FloatArray {
        val r = color.red
        val g = color.green
        val b = color.blue
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val h = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }.let { if (it < 0f) it + 360f else it }
        val s = if (max == 0f) 0f else delta / max
        return floatArrayOf(h, s, max)
    }

    private fun fromHsv(h: Float, s: Float, v: Float): Color {
        val c = v * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = v - c
        val (r, g, b) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(red = r + m, green = g + m, blue = b + m)
    }

    fun fromHsvColor(h: Float, s: Float, v: Float): Color = fromHsv(h, s, v)
}
