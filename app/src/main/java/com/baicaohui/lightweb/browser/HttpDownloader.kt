package com.baicaohui.lightweb.browser

import java.io.IOException
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class DownloadException(message: String, cause: Throwable? = null) : IOException(message, cause)

object HttpDownloader {

    suspend fun contentLength(
        url: String,
        userAgent: String,
        connectTimeoutMs: Int = 10_000,
        readTimeoutMs: Int = 10_000,
    ): Long? {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.setRequestProperty("User-Agent", userAgent)
            connection.requestMethod = "HEAD"
            val code = connection.responseCode
            if (code !in 200..299) return null
            return connection.contentLengthLong.takeIf { it > 0 }
        } catch (_: IOException) {
            return null
        } finally {
            connection.disconnect()
        }
    }

    suspend fun downloadResumable(
        url: String,
        userAgent: String,
        file: File,
        startOffset: Long = 0,
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
            if (startOffset > 0) {
                connection.setRequestProperty("Range", "bytes=$startOffset-")
            }
            val code = connection.responseCode
            val partial = code == 206 && startOffset > 0
            val total = when {
                partial -> connection.contentLengthLong.coerceAtLeast(0L) + startOffset
                code in 200..299 -> connection.contentLengthLong.coerceAtLeast(0L)
                else -> throw DownloadException("HTTP $code")
            }
            val effectiveStart = if (partial) startOffset else 0L
            val buffer = ByteArray(64 * 1024)
            var downloaded = effectiveStart
            val output = if (partial) FileOutputStream(file, true) else FileOutputStream(file)
            output.use { out ->
                connection.inputStream.use { input ->
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }
            return downloaded
        } finally {
            connection.disconnect()
        }
    }

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
