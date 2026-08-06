package com.baicaohui.lightweb.browser

import android.webkit.WebView
import android.util.Log
import com.baicaohui.lightweb.data.db.ReaderCacheEntity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** 阅读模式 WebView 交互薄封装：注入提取、恢复原文、加载离线缓存页。 */
class ReaderModeController(private val readabilityJs: () -> String) {

    suspend fun enter(wv: WebView, theme: String): ReaderArticle? =
        suspendCancellableCoroutine { cont ->
            wv.evaluateJavascript(ReaderPage.enterScript(readabilityJs(), theme)) { raw ->
                Log.d("ReaderMode", "enter result: $raw")
                if (cont.isActive) cont.resume(ReaderPage.parseResult(raw))
            }
        }

    fun exit(wv: WebView, onResult: (Boolean) -> Unit) {
        wv.evaluateJavascript(ReaderPage.exitScript()) { raw ->
            Log.d("ReaderMode", "exit result: $raw")
            onResult(ReaderPage.parseExit(raw))
        }
    }

    fun loadOffline(
        wv: WebView,
        url: String,
        article: ReaderCacheEntity,
        theme: String,
        offlineBadge: String,
    ) {
        wv.loadDataWithBaseURL(
            url,
            ReaderPage.offlineHtml(
                title = article.title,
                byline = article.byline,
                contentHtml = article.contentHtml,
                theme = theme,
                offlineBadge = offlineBadge,
            ),
            "text/html",
            "UTF-8",
            url,
        )
    }
}
