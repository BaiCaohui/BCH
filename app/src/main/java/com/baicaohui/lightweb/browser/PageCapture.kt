package com.baicaohui.lightweb.browser

import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** 全页截图：分段滚动 WebView 并绘制拼接，输出等比例缩小的缩略图。 */
object PageCapture {

    private const val THUMB_WIDTH = 360
    private const val MAX_HEIGHT = 2400
    private const val SEGMENT_DELAY_MS = 20L

    suspend fun capture(webView: WebView): Bitmap? = withContext(Dispatchers.Main) {
        if (!webView.isAttachedToWindow) return@withContext null
        val width = webView.width
        val viewHeight = webView.height
        val contentHeight = webView.contentHeight
        if (width <= 0 || viewHeight <= 0 || contentHeight <= 0) {
            return@withContext null
        }
        val scale = THUMB_WIDTH.toFloat() / width
        val captureHeight = minOf(contentHeight, (MAX_HEIGHT / scale).toInt())
        val bitmapHeight = (captureHeight * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(THUMB_WIDTH, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        val originalY = webView.scrollY
        var y = 0
        while (y < captureHeight) {
            webView.scrollTo(0, y)
            webView.invalidate()
            delay(SEGMENT_DELAY_MS)
            canvas.save()
            canvas.translate(0f, -y.toFloat())
            webView.draw(canvas)
            canvas.restore()
            y += viewHeight
        }
        webView.scrollTo(0, originalY)
        bitmap
    }
}
