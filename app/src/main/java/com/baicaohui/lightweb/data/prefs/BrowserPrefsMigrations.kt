package com.baicaohui.lightweb.data.prefs

/** 浏览偏好一次性迁移：按旧版本号逐级应用，最终收敛到当前版本。 */
object BrowserPrefsMigrations {

    fun migrate(prefs: BrowserPrefs): BrowserPrefs {
        var p = prefs
        if (p.prefsVersion < 1) {
            p = p.copy(
                tabPreviewEnabled = true,
                menuRows = 2,
                downloadMode = DownloadMode.APP,
                historySuggestionLimit = 2,
                downloadLocation = DownloadLocation.PUBLIC,
            )
        }
        if (p.prefsVersion < 2) {
            p = p.copy(
                menuRows = 2,
                downloadMode = DownloadMode.APP,
                historySuggestionLimit = 2,
                downloadLocation = DownloadLocation.PUBLIC,
            )
        }
        if (p.prefsVersion < 3) {
            p = p.copy(
                historySuggestionLimit = 2,
                downloadLocation = DownloadLocation.PUBLIC,
            )
        }
        if (p.prefsVersion < 4) {
            p = p.copy(downloadLocation = DownloadLocation.PUBLIC)
        }
        if (p.prefsVersion < 5) {
            p = p.copy(downloadLocation = DownloadLocation.PUBLIC)
        }
        if (p.prefsVersion < 6) {
            p = p.copy(prefsVersion = 6)
        }
        if (p.prefsVersion < 7) {
            p = p.copy(
                adLevel = if (p.adLevel == "OFF") "BASIC" else p.adLevel,
                prefsVersion = 7,
            )
        }
        return p
    }
}
