package com.baicaohui.lightweb.ui.theme

enum class DarkMode { SYSTEM, LIGHT, DARK }

enum class ShapeStyle { STANDARD, ROUNDED }

data class ThemeConfig(
    val seedColor: Long = 0xFF2B7FFF,
    val useDynamicColor: Boolean = true,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val fontScale: Float = 1f,
    val shapeStyle: ShapeStyle = ShapeStyle.STANDARD,
    val compact: Boolean = false,
) {
    companion object {
        val DEFAULT = ThemeConfig()
    }
}
