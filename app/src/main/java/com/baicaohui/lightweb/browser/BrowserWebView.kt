package com.baicaohui.lightweb.browser

import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.CookieManager
import com.baicaohui.lightweb.data.db.SiteSettingEntity
import com.baicaohui.lightweb.data.prefs.BrowserPrefs
import com.baicaohui.lightweb.data.prefs.UaMode

class BrowserWebView(
    context: Context,
    private val callbacks: WebCallbacks,
    private val adBlocker: AdBlocker,
    private val adLevel: () -> AdLevel = { AdLevel.BASIC },
) : WebView(context) {

    init {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            allowUniversalAccessFromFileURLs = false
            allowFileAccessFromFileURLs = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            setSafeBrowsingEnabled(true)
        }
        setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
            callbacks.onDownloadStart(url, userAgent ?: "", contentDisposition, mimetype)
        }
        webViewClient = BchWebViewClient(adBlocker, adLevel, callbacks)
        webChromeClient = BchWebChromeClient(callbacks)
    }

    fun applySiteSettings(url: String, prefs: BrowserPrefs, site: SiteSettingEntity?) {
        val desktop = site?.desktopMode ?: (prefs.uaMode == UaMode.DESKTOP)
        settings.userAgentString = when {
            desktop -> DESKTOP_UA
            prefs.uaMode == UaMode.CUSTOM && prefs.customUa.isNotBlank() -> prefs.customUa
            else -> settings.userAgentString
        }
        settings.javaScriptEnabled = site?.jsEnabled ?: prefs.defaultJsEnabled
        settings.setSafeBrowsingEnabled(prefs.safeBrowsing)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, prefs.thirdPartyCookies)
    }

    private companion object {
        val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    }
}
