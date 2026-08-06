package com.baicaohui.lightweb.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class IncognitoPrefsFilesTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `target file appends incognito suffix`() {
        val dir = tmp.newFolder("datastore")
        assertEquals(
            File(dir, "browser_incognito.preferences_pb"),
            IncognitoPrefsFiles.targetFile(dir, "browser"),
        )
    }

    @Test
    fun `copy copies existing main prefs`() {
        val dir = tmp.newFolder("datastore")
        File(dir, "browser.preferences_pb").writeText("b")
        File(dir, "theme.preferences_pb").writeText("t")
        IncognitoPrefsFiles.copyMainPrefsToIncognito(dir)
        assertTrue(File(dir, "browser_incognito.preferences_pb").exists())
        assertEquals("b", File(dir, "browser_incognito.preferences_pb").readText())
        assertEquals("t", File(dir, "theme_incognito.preferences_pb").readText())
    }

    @Test
    fun `copy removes stale incognito prefs`() {
        val dir = tmp.newFolder("datastore")
        File(dir, "home_incognito.preferences_pb").writeText("stale")
        IncognitoPrefsFiles.copyMainPrefsToIncognito(dir)
        assertFalse(File(dir, "home_incognito.preferences_pb").exists())
    }
}
