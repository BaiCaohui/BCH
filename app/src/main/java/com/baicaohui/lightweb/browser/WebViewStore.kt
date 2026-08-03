package com.baicaohui.lightweb.browser

import android.content.Context

class WebViewStore(
    private val adBlocker: AdBlocker,
    private val adLevel: () -> AdLevel,
) {
    private val views = mutableMapOf<Long, BrowserWebView>()
    private val loadedUrls = mutableMapOf<Long, String>()

    fun getOrCreate(id: Long, context: Context, callbacks: WebCallbacks): BrowserWebView =
        views.getOrPut(id) { BrowserWebView(context, callbacks, adBlocker, adLevel) }

    fun get(id: Long): BrowserWebView? = views[id]

    fun ensureLoaded(id: Long, url: String?) {
        if (url.isNullOrBlank()) return
        if (loadedUrls[id] == url) return
        views[id]?.let {
            it.loadUrl(url)
            loadedUrls[id] = url
        }
    }

    fun markLoaded(id: Long, url: String) {
        loadedUrls[id] = url
    }

    fun destroy(id: Long) {
        views.remove(id)?.let { wv ->
            runCatching {
                wv.stopLoading()
                wv.removeAllViews()
                wv.destroy()
            }
        }
        loadedUrls.remove(id)
    }

    fun destroyRemoved(activeIds: Set<Long>) {
        views.keys.filterNot { it in activeIds }.forEach { destroy(it) }
    }
}
