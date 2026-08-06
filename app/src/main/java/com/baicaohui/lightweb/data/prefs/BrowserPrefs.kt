package com.baicaohui.lightweb.data.prefs

import kotlinx.serialization.Serializable

object UaMode {
    const val DEFAULT = "DEFAULT"
    const val ANDROID = "ANDROID"
    const val IPHONE = "IPHONE"
    const val DESKTOP = "DESKTOP"
    const val CUSTOM = "CUSTOM"
}

object ToolbarPosition {
    const val TOP = "TOP"
    const val BOTTOM = "BOTTOM"
}

object DownloadMode {
    const val APP = "APP"
    const val SYSTEM = "SYSTEM"
}

object DownloadLocation {
    const val APP = "APP"
    const val PUBLIC = "PUBLIC"
}

@Serializable
data class BrowserPrefs(
    val searchTemplate: String = "https://www.bing.com/search?q=%s",
    val uaMode: String = UaMode.DEFAULT,
    val customUa: String = "",
    val adLevel: String = "OFF",
    val defaultJsEnabled: Boolean = true,
    val safeBrowsing: Boolean = true,
    val thirdPartyCookies: Boolean = false,
    val toolbarPosition: String = ToolbarPosition.TOP,
    val showBack: Boolean = true,
    val showForward: Boolean = true,
    val showReload: Boolean = true,
    val autoHideToolbar: Boolean = false,
    val showBottomBarLabels: Boolean = false,
    val tabPreviewEnabled: Boolean = true,
    val menuRows: Int = 2,
    val downloadMode: String = DownloadMode.APP,
    val historySuggestionLimit: Int = 2,
    val menuItemOrder: List<String> = emptyList(),
    val downloadLocation: String = DownloadLocation.PUBLIC,
    val antiTracking: Boolean = true,
    val httpsMode: String = "PREFER",
    val clearCookiesOnExit: Boolean = false,
    val downloadRiskWarnings: Boolean = true,
    val permissionPromptEnabled: Boolean = true,
    val autoplayAllowed: Boolean = false,
    val customAdRules: List<String> = emptyList(),
    val trackedHosts: List<String> = emptyList(),
    val prefsVersion: Int = 0,
) {
    companion object {
        val DEFAULT = BrowserPrefs()
    }
}
