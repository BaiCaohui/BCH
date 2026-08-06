package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadRiskPolicyTest {

    @Test
    fun `high risk executable extensions`() {
        listOf("file.apk", "setup.exe", "run.bat", "x.cmd", "y.scr", "z.msi", "app.jar", "v.vbs", "p.ps1").forEach {
            assertEquals(DownloadRisk.HIGH, DownloadRiskPolicy.riskOf("https://x.com/$it", null))
        }
    }

    @Test
    fun `high risk executable mime types`() {
        assertEquals(
            DownloadRisk.HIGH,
            DownloadRiskPolicy.riskOf("https://x.com/app", "application/vnd.android.package-archive"),
        )
        assertEquals(
            DownloadRisk.HIGH,
            DownloadRiskPolicy.riskOf("https://x.com/app", "application/x-msdownload"),
        )
    }

    @Test
    fun `normal files are low risk`() {
        listOf(
            "https://x.com/doc.pdf" to "application/pdf",
            "https://x.com/photo.jpg" to "image/jpeg",
            "https://x.com/video.mp4" to "video/mp4",
            "https://x.com/archive.zip" to "application/zip",
        ).forEach { (url, mime) ->
            assertEquals(DownloadRisk.LOW, DownloadRiskPolicy.riskOf(url, mime))
        }
    }

    @Test
    fun `query strings do not affect extension detection`() {
        assertEquals(
            DownloadRisk.HIGH,
            DownloadRiskPolicy.riskOf("https://x.com/get?id=1&file=app.apk", null),
        )
    }
}
