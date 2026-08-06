package com.baicaohui.lightweb.browser

import kotlin.concurrent.thread
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.net.ServerSocket

class HttpDownloaderTest {

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
}
