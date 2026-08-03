package com.baicaohui.lightweb.browser

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 按 Tab 缓存整页截图，带像素预算（防止多标签内存膨胀）。 */
class TabThumbnailStore(
    private val maxPixels: Long = 8_000_000L,
) {
    private val _thumbnails = MutableStateFlow<Map<Long, Bitmap>>(emptyMap())
    val thumbnails: StateFlow<Map<Long, Bitmap>> = _thumbnails.asStateFlow()

    fun put(id: Long, bitmap: Bitmap) {
        val current = _thumbnails.value.toMutableMap()
        val old = current.remove(id)
        current[id] = bitmap
        var total = current.values.sumOf { it.width.toLong() * it.height }
        while (total > maxPixels && current.size > 1) {
            val oldestKey = current.keys.first()
            current.remove(oldestKey)?.recycle()
            total = current.values.sumOf { it.width.toLong() * it.height }
        }
        _thumbnails.value = current
        old?.recycle()
    }

    fun retain(activeIds: Set<Long>) {
        val current = _thumbnails.value
        val removed = current.keys.filterNot { it in activeIds }
        if (removed.isEmpty()) return
        val next = current - removed.toSet()
        removed.forEach { current[it]?.recycle() }
        _thumbnails.value = next
    }

    fun clear() {
        _thumbnails.value.values.forEach { it.recycle() }
        _thumbnails.value = emptyMap()
    }
}
