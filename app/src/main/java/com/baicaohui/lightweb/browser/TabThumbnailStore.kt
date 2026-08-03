package com.baicaohui.lightweb.browser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * 按 Tab 缓存整页截图（内存 + 磁盘双份）：
 * - 内存 StateFlow 供 UI 即时读取，带像素预算防止内存膨胀
 * - 磁盘以 JPEG 持久化，冷启动恢复标签时重新加载
 */
class TabThumbnailStore(
    private val thumbnailDir: File,
    private val ioScope: CoroutineScope,
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
        ioScope.launch { saveToDisk(id, bitmap) }
    }

    fun loadAll(ids: Set<Long>) {
        val loaded = ids.mapNotNull { id ->
            val file = fileFor(id)
            if (file.exists()) {
                runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()?.let { id to it }
            } else {
                null
            }
        }.toMap()
        if (loaded.isNotEmpty()) {
            _thumbnails.value = _thumbnails.value + loaded
        }
    }

    fun retain(activeIds: Set<Long>) {
        val current = _thumbnails.value
        val removed = current.keys.filterNot { it in activeIds }
        if (removed.isEmpty()) return
        val next = current - removed.toSet()
        removed.forEach { current[it]?.recycle() }
        _thumbnails.value = next
        ioScope.launch {
            removed.forEach { fileFor(it).delete() }
        }
    }

    fun clear() {
        _thumbnails.value.values.forEach { it.recycle() }
        _thumbnails.value = emptyMap()
        ioScope.launch {
            thumbnailDir.listFiles()?.forEach { it.delete() }
        }
    }

    private fun fileFor(id: Long) = File(thumbnailDir, "$id.jpg")

    private suspend fun saveToDisk(id: Long, bitmap: Bitmap) {
        runCatching {
            thumbnailDir.mkdirs()
            FileOutputStream(fileFor(id)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
        }
    }
}
