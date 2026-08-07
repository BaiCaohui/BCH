package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadFormatTest {

    @Test
    fun `formatBytes shows bytes kilobytes and megabytes`() {
        assertEquals("512 B", DownloadFormat.formatBytes(512))
        assertEquals("1.0 KB", DownloadFormat.formatBytes(1024))
        assertEquals("2.5 KB", DownloadFormat.formatBytes(2560))
        assertEquals("1.5 MB", DownloadFormat.formatBytes(1_572_864))
        assertEquals("2.0 GB", DownloadFormat.formatBytes(2_147_483_648))
    }

    @Test
    fun `formatBytes handles zero and negative`() {
        assertEquals("0 B", DownloadFormat.formatBytes(0))
        assertEquals("0 B", DownloadFormat.formatBytes(-1))
    }

    @Test
    fun `formatSpeed shows bytes per second`() {
        assertEquals("0 B/s", DownloadFormat.formatSpeed(0))
        assertEquals("512 B/s", DownloadFormat.formatSpeed(512))
        assertEquals("1.0 KB/s", DownloadFormat.formatSpeed(1024))
        assertEquals("320.5 KB/s", DownloadFormat.formatSpeed(328_192))
        assertEquals("2.0 MB/s", DownloadFormat.formatSpeed(2_097_152))
    }
}
