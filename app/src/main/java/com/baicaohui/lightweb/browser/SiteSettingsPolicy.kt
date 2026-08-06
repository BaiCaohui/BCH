package com.baicaohui.lightweb.browser

import com.baicaohui.lightweb.data.db.SiteSettingEntity
import com.baicaohui.lightweb.data.prefs.BrowserPrefs

/** 站点设置与全局设置合并后的最终生效值。 */
data class ResolvedSiteSettings(
    val javaScriptEnabled: Boolean,
    val adLevel: AdLevel,
    val safeBrowsing: Boolean,
    val thirdPartyCookies: Boolean,
    val antiTracking: Boolean,
    val httpsMode: String,
    val autoplay: Boolean,
)

object SiteSettingsPolicy {

    fun resolve(prefs: BrowserPrefs, site: SiteSettingEntity?): ResolvedSiteSettings =
        ResolvedSiteSettings(
            javaScriptEnabled = site?.jsEnabled ?: prefs.defaultJsEnabled,
            adLevel = parseAdLevel(site?.adLevel)
                ?: parseAdLevel(prefs.adLevel)
                ?: AdLevel.OFF,
            safeBrowsing = site?.safeBrowsing ?: prefs.safeBrowsing,
            thirdPartyCookies = site?.thirdPartyCookies ?: prefs.thirdPartyCookies,
            antiTracking = site?.antiTracking ?: prefs.antiTracking,
            httpsMode = when (site?.httpsUpgrade) {
                true -> HttpsMode.STRICT
                false -> HttpsMode.OFF
                null -> prefs.httpsMode
            },
            autoplay = site?.autoplay ?: prefs.autoplayAllowed,
        )

    fun parseAdLevel(raw: String?): AdLevel? = raw?.let {
        runCatching { AdLevel.valueOf(it) }.getOrNull()
    }
}
