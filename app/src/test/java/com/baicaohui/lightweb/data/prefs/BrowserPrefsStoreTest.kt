package com.baicaohui.lightweb.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BrowserPrefsStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun newStore() = BrowserPrefsStore(
        PreferenceDataStoreFactory.create { tmp.newFile("browser-${System.nanoTime()}.preferences_pb") },
    )

    @Test
    fun `defaults when store is empty`() = runTest {
        assertEquals(BrowserPrefs.DEFAULT, newStore().prefs.first())
    }

    @Test
    fun `update persists all fields`() = runTest {
        val store = newStore()
        store.update {
            it.copy(
                uaMode = UaMode.CUSTOM,
                customUa = "BCH/1.0",
                adLevel = "STRICT",
                maxTabs = 6,
                thirdPartyCookies = true,
                toolbarPosition = ToolbarPosition.BOTTOM,
                showReload = false,
                autoHideToolbar = true,
            )
        }
        val prefs = store.prefs.first()
        assertEquals(UaMode.CUSTOM, prefs.uaMode)
        assertEquals("BCH/1.0", prefs.customUa)
        assertEquals("STRICT", prefs.adLevel)
        assertEquals(6, prefs.maxTabs)
        assertEquals(true, prefs.thirdPartyCookies)
        assertEquals(ToolbarPosition.BOTTOM, prefs.toolbarPosition)
        assertEquals(false, prefs.showReload)
        assertEquals(true, prefs.autoHideToolbar)
    }
}
