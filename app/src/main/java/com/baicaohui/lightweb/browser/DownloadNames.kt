package com.baicaohui.lightweb.browser

import java.net.URLDecoder

object DownloadNames {

    private val ILLEGAL = Regex("""[\\/:*?"<>|\u0000-\u001F]""")

    fun from(url: String, contentDisposition: String?, mimeType: String?): String {
        parseContentDisposition(contentDisposition)?.let { return sanitize(it) }
        val fromUrl = url.substringBefore('?').substringAfterLast('/')
        if (fromUrl.isNotBlank()) {
            val cleaned = sanitize(fromUrl)
            if (cleaned.isNotBlank()) {
                val ext = extensionFor(mimeType)
                return if (!cleaned.contains('.') && ext != null) "$cleaned.$ext" else cleaned
            }
        }
        return extensionFor(mimeType)?.let { "download.$it" } ?: "download"
    }

    private fun parseContentDisposition(disposition: String?): String? {
        if (disposition.isNullOrBlank()) return null
        val star = Regex("""filename\*=UTF-8''([^;]+)""", RegexOption.IGNORE_CASE).find(disposition)
        if (star != null) {
            return runCatching {
                URLDecoder.decode(star.groupValues[1], Charsets.UTF_8.name())
            }.getOrNull()?.takeIf { it.isNotBlank() }
        }
        val plain = Regex("""filename="?([^";]+)"?""", RegexOption.IGNORE_CASE).find(disposition)
        return plain?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun sanitize(name: String): String {
        val cleaned = ILLEGAL.replace(name, "_").trim(' ', '.')
        return cleaned.ifBlank { "download" }
    }

    private fun extensionFor(mimeType: String?): String? = when (mimeType?.lowercase()) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        "application/pdf" -> "pdf"
        "application/zip" -> "zip"
        "application/x-7z-compressed" -> "7z"
        "application/vnd.android.package-archive" -> "apk"
        "text/html" -> "html"
        "text/plain" -> "txt"
        "application/json" -> "json"
        "audio/mpeg" -> "mp3"
        "video/mp4" -> "mp4"
        else -> null
    }
}
