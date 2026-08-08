package com.baicaohui.lightweb.browser

import com.baicaohui.lightweb.data.prefs.BrowserPrefs
import com.baicaohui.lightweb.data.prefs.BrowserPrefsMigrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdBlockDefaultsTest {

    @Test
    fun `default ad level is basic`() {
        assertEquals(AdLevel.BASIC.name, BrowserPrefs.DEFAULT.adLevel)
    }

    @Test
    fun `migration switches off to basic once`() {
        val migrated = BrowserPrefsMigrations.migrate(
            BrowserPrefs.DEFAULT.copy(adLevel = AdLevel.OFF.name, prefsVersion = 6),
        )
        assertEquals(AdLevel.BASIC.name, migrated.adLevel)
        assertTrue(migrated.prefsVersion >= 7)
    }

    @Test
    fun `migration keeps strict choice`() {
        val migrated = BrowserPrefsMigrations.migrate(
            BrowserPrefs.DEFAULT.copy(adLevel = AdLevel.STRICT.name, prefsVersion = 6),
        )
        assertEquals(AdLevel.STRICT.name, migrated.adLevel)
    }

    @Test
    fun `migration leaves current version unchanged`() {
        val prefs = BrowserPrefs.DEFAULT.copy(adLevel = AdLevel.BASIC.name, prefsVersion = 7)
        assertEquals(prefs, BrowserPrefsMigrations.migrate(prefs))
    }
}
