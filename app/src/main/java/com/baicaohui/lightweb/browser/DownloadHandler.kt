package com.baicaohui.lightweb.browser

import android.app.DownloadManager
import android.content.Context
import android.net.Uri

class DownloadHandler(private val context: Context) {

    fun start(url: String, userAgent: String, mimeType: String?) {
        val title = url.substringAfterLast('/').take(60).ifBlank { "download" }
        val request = DownloadManager.Request(Uri.parse(url))
            .setMimeType(mimeType ?: "application/octet-stream")
            .addRequestHeader("User-Agent", userAgent)
            .setTitle(title)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        context.getSystemService(DownloadManager::class.java).enqueue(request)
    }
}
