package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadNamesTest {

    @Test
    fun `uses filename from content disposition`() {
        assertEquals(
            "report.pdf",
            DownloadNames.from(
                "https://example.com/a",
                "attachment; filename=\"report.pdf\"",
                "application/pdf",
            ),
        )
    }

    @Test
    fun `decodes utf8 filename star`() {
        assertEquals(
            "报告.pdf",
            DownloadNames.from(
                "https://example.com/a",
                "attachment; filename*=UTF-8''%E6%8A%A5%E5%91%8A.pdf",
                null,
            ),
        )
    }

    @Test
    fun `falls back to url path segment`() {
        assertEquals(
            "archive.zip",
            DownloadNames.from("https://example.com/files/archive.zip?token=1", null, null),
        )
    }

    @Test
    fun `sanitizes illegal characters in path segment`() {
        assertEquals(
            "b_c.zip",
            DownloadNames.from("https://example.com/a/b\\c.zip", null, null),
        )
    }

    @Test
    fun `falls back to download with mime extension`() {
        assertEquals(
            "download.jpg",
            DownloadNames.from("https://example.com/download", null, "image/jpeg"),
        )
    }

    @Test
    fun `falls back to plain download`() {
        assertEquals(
            "download",
            DownloadNames.from("https://example.com/", null, null),
        )
    }
}
