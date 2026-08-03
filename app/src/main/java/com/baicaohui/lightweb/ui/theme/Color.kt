package com.baicaohui.lightweb.ui.theme

import androidx.compose.ui.graphics.Color

val PRESET_SEEDS: List<Pair<String, Long>> = listOf(
    "青蓝" to 0xFF2B7FFF,
    "青绿" to 0xFF00A87E,
    "蓝紫" to 0xFF7C4DFF,
    "玫红" to 0xFFE91E63,
    "橙色" to 0xFFFF6D00,
    "红色" to 0xFFD32F2F,
    "棕色" to 0xFF795548,
    "灰色" to 0xFF607D8B,
    "墨黑" to 0xFF263238,
    "金色" to 0xFFB8860B,
)

fun Color.toSeedLong(): Long {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return (0xFFL shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
}
