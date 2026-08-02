package com.baicaohui.lightweb.browser

import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView

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
}
