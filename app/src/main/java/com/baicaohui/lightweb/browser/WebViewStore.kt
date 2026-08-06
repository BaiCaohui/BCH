package com.baicaohui.lightweb.browser

import android.content.Context

class WebViewStore(
    private val adBlocker: AdBlocker,
    private val adLevel: () -> AdLevel,
    private val trackerBlocker: TrackerBlocker,
    private val customRules: () -> List<String>,
) {
    private val views = mutableMapOf<Long, BrowserWebView>()
    private val loadedUrls = mutableMapOf<Long, String>()
    private val lastAccess = mutableMapOf<Long, Long>()

    fun getOrCreate(id: Long, context: Context, callbacks: WebCallbacks): BrowserWebView {
        lastAccess[id] = System.currentTimeMillis()
        return views.getOrPut(id) {
            BrowserWebView(
                context = context,
                callbacks = callbacks,
                adBlocker = adBlocker,
                adLevel = adLevel,
                trackerBlocker = trackerBlocker,
                customRules = customRules,
            )
        }
    }

    fun get(id: Long): BrowserWebView? {
        if (id in views) lastAccess[id] = System.currentTimeMillis()
        return views[id]
    }

    fun ensureLoaded(id: Long, url: String?) {
        if (url.isNullOrBlank()) return
        if (loadedUrls[id] == url) return
        views[id]?.let {
            // 显式置空 x-requested-with，防止部分低版本 WebView 仍附带应用包名
            it.loadUrl(url, mapOf("x-requested-with" to ""))
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
        lastAccess.remove(id)
    }

    fun destroyRemoved(activeIds: Set<Long>) {
        views.keys.filterNot { it in activeIds }.forEach { destroy(it) }
    }

    /**
     * 软回收：标签数量不设上限，但只保留最近使用的 [limit] 个 WebView 实例，
     * 其余后台 WebView 销毁（切回时按 URL 重新加载）。
     */
    fun trim(activeIds: Set<Long>, keepId: Long?, limit: Int) {
        if (views.size <= limit) return
        val candidates = views.keys
            .filter { it != keepId && it in activeIds }
            .sortedBy { lastAccess[it] ?: 0L }
        var excess = views.size - limit
        for (id in candidates) {
            if (excess <= 0) break
            destroy(id)
            excess--
        }
    }
}
