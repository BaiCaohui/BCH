package com.baicaohui.lightweb.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class IncognitoProcessTest {

    @Test
    fun `recognizes incognito process name`() {
        assertTrue(
            IncognitoProcess.isIncognitoProcessName(
                "com.baicaohui.lightweb:incognito",
                "com.baicaohui.lightweb",
            ),
        )
        assertFalse(
            IncognitoProcess.isIncognitoProcessName(
                "com.baicaohui.lightweb",
                "com.baicaohui.lightweb",
            ),
        )
    }

    @Test
    fun `data dir appends suffix to webview dir`() {
        assertEquals(
            File("/data/data/pkg/app_webview_incognito"),
            IncognitoProcess.dataDir(File("/data/data/pkg")),
        )
    }

    @Test
    fun `suffix is incognito`() {
        assertEquals("incognito", IncognitoProcess.SUFFIX)
    }

    @Test
    fun `purges data dir including stale webview lock and recreates empty dir`() {
        val dataDir = createTempDirectory().toFile()
        val dir = IncognitoProcess.dataDir(dataDir)
        assertTrue(dir.mkdirs())
        File(dir, "webview_data.lock").writeText("stale")
        val cache = File(dir, "cache")
        assertTrue(cache.mkdir())
        File(cache, "entry").writeText("x")

        assertTrue(IncognitoProcess.purgeDataDir(dataDir, retryDelayMs = 0))
        assertTrue(dir.isDirectory)
        assertTrue(dir.listFiles().isNullOrEmpty())
    }

    @Test
    fun `purge creates data dir when absent`() {
        val dataDir = createTempDirectory().toFile()
        val dir = IncognitoProcess.dataDir(dataDir)
        assertTrue(IncognitoProcess.purgeDataDir(dataDir, retryDelayMs = 0))
        assertTrue(dir.isDirectory)
        assertTrue(dir.listFiles().isNullOrEmpty())
    }

    @Test
    fun `purge returns false when deletion keeps failing`() {
        val dataDir = createTempDirectory().toFile()
        val dir = IncognitoProcess.dataDir(dataDir)
        assertTrue(dir.mkdirs())
        File(dir, "webview_data.lock").writeText("held")
        File(dir, "stale_entry").writeText("x")

        assertFalse(
            IncognitoProcess.purgeDataDir(
                dataDir,
                attempts = 3,
                retryDelayMs = 0,
                deleteDir = { false },
            ),
        )
        assertTrue(dir.exists())
        assertTrue(File(dir, "stale_entry").exists())
    }
}
