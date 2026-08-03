package com.baicaohui.lightweb.data.prefs

import kotlinx.serialization.Serializable

object UaMode {
    const val DEFAULT = "DEFAULT"
    const val DESKTOP = "DESKTOP"
    const val CUSTOM = "CUSTOM"
}

object ToolbarPosition {
    const val TOP = "TOP"
    const val BOTTOM = "BOTTOM"
}

@Serializable
data class BrowserPrefs(
    val searchTemplate: String = "https://www.bing.com/search?q=%s",
    val uaMode: String = UaMode.DEFAULT,
    val customUa: String = "",
    val adLevel: String = "BASIC",
    val defaultJsEnabled: Boolean = true,
    val safeBrowsing: Boolean = true,
    val thirdPartyCookies: Boolean = false,
    val toolbarPosition: String = ToolbarPosition.TOP,
    val showBack: Boolean = true,
    val showForward: Boolean = true,
    val showReload: Boolean = true,
    val autoHideToolbar: Boolean = false,
    val showBottomBarLabels: Boolean = false,
) {
    companion object {
        val DEFAULT = BrowserPrefs()
    }
}
