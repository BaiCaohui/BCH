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
                thirdPartyCookies = true,
                toolbarPosition = ToolbarPosition.BOTTOM,
                showReload = false,
                autoHideToolbar = true,
                showBottomBarLabels = true,
                tabPreviewEnabled = true,
            )
        }
        val prefs = store.prefs.first()
        assertEquals(UaMode.CUSTOM, prefs.uaMode)
        assertEquals("BCH/1.0", prefs.customUa)
        assertEquals("STRICT", prefs.adLevel)
        assertEquals(true, prefs.thirdPartyCookies)
        assertEquals(ToolbarPosition.BOTTOM, prefs.toolbarPosition)
        assertEquals(false, prefs.showReload)
        assertEquals(true, prefs.autoHideToolbar)
        assertEquals(true, prefs.showBottomBarLabels)
        assertEquals(true, prefs.tabPreviewEnabled)
    }

    @Test
    fun `legacy prefs migrate tab preview to enabled`() = runTest {
        val store = newStore()
        // 模拟旧版本数据：预览关闭且 prefsVersion=0
        val legacy = BrowserPrefs(tabPreviewEnabled = false)
        store.update { legacy }
        val prefs = store.prefs.first()
        assertEquals(true, prefs.tabPreviewEnabled)
        assertEquals(5, prefs.prefsVersion)
        assertEquals(2, prefs.menuRows)
        assertEquals(DownloadMode.APP, prefs.downloadMode)
        assertEquals(2, prefs.historySuggestionLimit)
        assertEquals(DownloadLocation.PUBLIC, prefs.downloadLocation)
    }

    @Test
    fun `preview toggle persists after migration`() = runTest {
        val store = newStore()
        store.update { it.copy(tabPreviewEnabled = false) }
        store.update { it.copy(tabPreviewEnabled = false) }
        assertEquals(false, store.prefs.first().tabPreviewEnabled)
    }

    @Test
    fun `defaults include two menu rows and app downloader`() = runTest {
        val prefs = newStore().prefs.first()
        assertEquals(2, prefs.menuRows)
        assertEquals(DownloadMode.APP, prefs.downloadMode)
        assertEquals(2, prefs.historySuggestionLimit)
        assertEquals(DownloadLocation.PUBLIC, prefs.downloadLocation)
    }

    @Test
    fun `menu rows and download mode persist`() = runTest {
        val store = newStore()
        store.update {
            it.copy(
                menuRows = 1,
                downloadMode = DownloadMode.SYSTEM,
            )
        }
        val prefs = store.prefs.first()
        assertEquals(1, prefs.menuRows)
        assertEquals(DownloadMode.SYSTEM, prefs.downloadMode)
    }

    @Test
    fun `legacy v2 prefs migrate history suggestion limit`() = runTest {
        val store = newStore()
        store.update { it.copy(prefsVersion = 2) }
        val prefs = store.prefs.first()
        assertEquals(2, prefs.historySuggestionLimit)
        assertEquals(5, prefs.prefsVersion)
        assertEquals(DownloadLocation.PUBLIC, prefs.downloadLocation)
    }

    @Test
    fun `history suggestion limit persists`() = runTest {
        val store = newStore()
        store.update { it.copy(historySuggestionLimit = 4) }
        assertEquals(4, store.prefs.first().historySuggestionLimit)
    }

    @Test
    fun `menu item order persists`() = runTest {
        val store = newStore()
        store.update {
            it.copy(menuItemOrder = listOf("settings", "reload", "bookmarks"))
        }
        assertEquals(
            listOf("settings", "reload", "bookmarks"),
            store.prefs.first().menuItemOrder,
        )
    }

    @Test
    fun `menu item order defaults to empty meaning default order`() = runTest {
        assertEquals(emptyList<String>(), newStore().prefs.first().menuItemOrder)
    }

    @Test
    fun `legacy v3 prefs migrate download location`() = runTest {
        val store = newStore()
        store.update { it.copy(prefsVersion = 3) }
        val prefs = store.prefs.first()
        assertEquals(DownloadLocation.PUBLIC, prefs.downloadLocation)
        assertEquals(5, prefs.prefsVersion)
    }

    @Test
    fun `download location persists`() = runTest {
        val store = newStore()
        store.update { it.copy(downloadLocation = DownloadLocation.APP) }
        assertEquals(DownloadLocation.APP, store.prefs.first().downloadLocation)
    }

    @Test
    fun `legacy v4 prefs migrate download location to public`() = runTest {
        val store = newStore()
        store.update {
            it.copy(prefsVersion = 4, downloadLocation = DownloadLocation.APP)
        }
        val prefs = store.prefs.first()
        assertEquals(DownloadLocation.PUBLIC, prefs.downloadLocation)
        assertEquals(5, prefs.prefsVersion)
    }
}
