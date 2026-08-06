package com.baicaohui.lightweb.browser

import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadException(message: String, cause: Throwable? = null) : IOException(message, cause)

object HttpDownloader {

    suspend fun download(
        url: String,
        userAgent: String,
        output: OutputStream,
        onProgress: suspend (downloaded: Long, total: Long) -> Unit,
        connectTimeoutMs: Int = 15_000,
        readTimeoutMs: Int = 30_000,
    ): Long {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.setRequestProperty("User-Agent", userAgent)
            connection.requestMethod = "GET"
            val code = connection.responseCode
            if (code !in 200..299) throw DownloadException("HTTP $code")
            val total = connection.contentLengthLong.coerceAtLeast(0L)
            val buffer = ByteArray(64 * 1024)
            var downloaded = 0L
            connection.inputStream.use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    downloaded += read
                    onProgress(downloaded, total)
                }
            }
            return downloaded
        } finally {
            connection.disconnect()
        }
    }
}
