package com.baicaohui.lightweb.browser

import kotlin.concurrent.thread
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.ServerSocket

class HttpDownloaderTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var server: ServerSocket

    @Before
    fun setUp() {
        server = ServerSocket(0)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun url() = "http://127.0.0.1:${server.localPort}/file"

    private fun startHandler(responseCode: Int, body: ByteArray) {
        thread(isDaemon = true) {
            runCatching {
                server.accept().use { socket ->
                    val reader = socket.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                    }
                    val status = if (responseCode == 200) "200 OK" else "404 Not Found"
                    val head = buildString {
                        append("HTTP/1.1 $status\r\n")
                        append("Content-Length: ${body.size}\r\n")
                        append("Connection: close\r\n\r\n")
                    }
                    socket.getOutputStream().use { out ->
                        out.write(head.toByteArray(Charsets.ISO_8859_1))
                        if (responseCode == 200) out.write(body)
                    }
                }
            }
        }
    }

    private fun startRangeHandler(body: ByteArray) {
        thread(isDaemon = true) {
            runCatching {
                server.accept().use { socket ->
                    val reader = socket.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                    val requestLine = reader.readLine() ?: return@use
                    var range: String? = null
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                        if (line.startsWith("Range:", ignoreCase = true)) {
                            range = line.substringAfter(':').trim()
                        }
                    }
                    val start = range
                        ?.substringAfter('=')
                        ?.substringBefore('-')
                        ?.trim()
                        ?.toLongOrNull()
                        ?: 0L
                    val slice = body.drop(start.toInt()).toByteArray()
                    val status = if (range != null) "206 Partial Content" else "200 OK"
                    val head = buildString {
                        append("HTTP/1.1 $status\r\n")
                        append("Content-Length: ${slice.size}\r\n")
                        if (range != null) {
                            append("Content-Range: bytes $start-${body.size - 1}/${body.size}\r\n")
                        }
                        append("Connection: close\r\n\r\n")
                    }
                    socket.getOutputStream().use { out ->
                        out.write(head.toByteArray(Charsets.ISO_8859_1))
                        out.write(slice)
                    }
                }
            }
        }
    }

    @Test
    fun `download writes body and reports progress`() = runTest {
        val body = ByteArray(200_000) { (it % 251).toByte() }
        startHandler(200, body)
        val out = ByteArrayOutputStream()
        val progress = mutableListOf<Pair<Long, Long>>()
        val total = HttpDownloader.download(
            url(),
            "BCH/1.0",
            out,
            onProgress = { done, totalBytes ->
                progress += done to totalBytes
            },
        )
        assertEquals(body.size.toLong(), total)
        assertArrayEquals(body, out.toByteArray())
        assertEquals(body.size.toLong() to body.size.toLong(), progress.last())
    }

    @Test(expected = DownloadException::class)
    fun `download throws on http error`() = runTest {
        startHandler(404, ByteArray(0))
        HttpDownloader.download(
            url(),
            "BCH/1.0",
            ByteArrayOutputStream(),
            onProgress = { _, _ -> },
        )
    }

    @Test
    fun `contentLength returns content length from head`() = runTest {
        val body = ByteArray(1234) { 1 }
        startHandler(200, body)
        assertEquals(body.size.toLong(), HttpDownloader.contentLength(url(), "BCH/1.0"))
    }

    @Test
    fun `contentLength returns null on http error`() = runTest {
        startHandler(404, ByteArray(0))
        assertNull(HttpDownloader.contentLength(url(), "BCH/1.0"))
    }

    @Test
    fun `downloadResumable appends partial when server supports range`() = runTest {
        val body = "0123456789".toByteArray()
        startRangeHandler(body)
        val file = File(tmp.newFolder("resume"), "part.bin")
        file.writeBytes("01234".toByteArray())

        val total = HttpDownloader.downloadResumable(
            url = url(),
            userAgent = "BCH/1.0",
            file = file,
            startOffset = 5,
            onProgress = { _, _ -> },
        )

        assertEquals(10L, total)
        assertEquals("0123456789", file.readText())
    }

    @Test
    fun `downloadResumable restarts from scratch when server ignores range`() = runTest {
        val body = "abcdef".toByteArray()
        startHandler(200, body)
        val file = File(tmp.newFolder("restart"), "part.bin")
        file.writeBytes("01234".toByteArray())

        val total = HttpDownloader.downloadResumable(
            url = url(),
            userAgent = "BCH/1.0",
            file = file,
            startOffset = 5,
            onProgress = { _, _ -> },
        )

        assertEquals(6L, total)
        assertEquals("abcdef", file.readText())
    }
}
