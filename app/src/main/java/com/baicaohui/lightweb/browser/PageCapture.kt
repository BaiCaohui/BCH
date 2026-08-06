package com.baicaohui.lightweb.browser

import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** 标签页缩略图：只截当前视口，不滚动页面，避免打扰动态网页并保证页面变化后能可靠更新。 */
object PageCapture {

    private const val THUMB_WIDTH = 360
    private const val SETTLE_DELAY_MS = 400L

    suspend fun capture(webView: WebView): Bitmap? = withContext(Dispatchers.Main) {
        if (!webView.isAttachedToWindow) return@withContext null
        delay(SETTLE_DELAY_MS)
        // 等待期间用户可能已切走标签页，避免把空白画面写回缩略图
        if (!webView.isAttachedToWindow) return@withContext null
        val width = webView.width
        val viewHeight = webView.height
        val contentHeight = webView.contentHeight
        if (width <= 0 || viewHeight <= 0 || contentHeight <= 0) {
            return@withContext null
        }
        val scale = THUMB_WIDTH.toFloat() / width
        val bitmapHeight = (viewHeight * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(THUMB_WIDTH, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        webView.draw(canvas)
        bitmap
    }
}
