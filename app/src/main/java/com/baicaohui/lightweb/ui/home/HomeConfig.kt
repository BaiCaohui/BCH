package com.baicaohui.lightweb.ui.home

import kotlinx.serialization.Serializable

@Serializable
enum class HomeWidgetType { SEARCH, SPEED_DIAL, RECENT, BOOKMARKS, CLOCK }

@Serializable
data class HomeWidgetConfig(
    val type: HomeWidgetType,
    val enabled: Boolean = true,
    val columns: Int = 4,
    val limit: Int = 8,
)

@Serializable
enum class BackgroundType { COLOR, GRADIENT, IMAGE }

@Serializable
data class HomeBackground(
    val type: BackgroundType = BackgroundType.COLOR,
    val color: Long = 0x00000000,
    val gradientStart: Long = 0xFF2B7FFF,
    val gradientEnd: Long = 0xFF00A87E,
    val imageUri: String? = null,
)

@Serializable
data class HomeConfig(
    val widgets: List<HomeWidgetConfig> = defaultWidgets(),
    val background: HomeBackground = HomeBackground(),
    val overlayAlpha: Float = 0.15f,
    val showSearchSuggestions: Boolean = false,
) {
    companion object {
        val DEFAULT = HomeConfig()

        private fun defaultWidgets(): List<HomeWidgetConfig> = listOf(
            HomeWidgetConfig(type = HomeWidgetType.SEARCH, enabled = true),
            HomeWidgetConfig(type = HomeWidgetType.SPEED_DIAL, enabled = true),
            HomeWidgetConfig(type = HomeWidgetType.RECENT, enabled = false),
            HomeWidgetConfig(type = HomeWidgetType.BOOKMARKS, enabled = false),
            HomeWidgetConfig(type = HomeWidgetType.CLOCK, enabled = false),
        )
    }
}
