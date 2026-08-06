package com.baicaohui.lightweb.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 书签图标落盘：网页图标（Bitmap）与用户选择的图片统一存到应用私有目录。 */
object BookmarkIconStore {

    fun dir(context: Context): File = File(context.filesDir, "bookmark_icons")

    fun savePageIcon(context: Context, bitmap: Bitmap, name: String): String? = runCatching {
        dir(context).apply { mkdirs() }
        val file = File(dir(context), "page_$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        file.absolutePath
    }.getOrNull()

    suspend fun savePickedImage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            dir(context).apply { mkdirs() }
            val input = context.contentResolver.openInputStream(uri) ?: return@runCatching null
            val file = File(dir(context), "picked_${System.currentTimeMillis()}.png")
            input.use { src -> file.outputStream().use { dst -> src.copyTo(dst) } }
            file.absolutePath
        }.getOrNull()
    }
}
