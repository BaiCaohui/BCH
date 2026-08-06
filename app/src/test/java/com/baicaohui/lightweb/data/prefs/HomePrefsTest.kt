package com.baicaohui.lightweb.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.baicaohui.lightweb.ui.home.BackgroundType
import com.baicaohui.lightweb.ui.home.HomeConfig
import com.baicaohui.lightweb.ui.home.HomeWidgetType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HomePrefsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun newPrefs() = HomePrefs(
        PreferenceDataStoreFactory.create { tmp.newFile("home-${System.nanoTime()}.preferences_pb") },
    )

    @Test
    fun `defaults when store is empty`() = runTest {
        assertEquals(HomeConfig.DEFAULT, newPrefs().config.first())
    }

    @Test
    fun `update persists widgets order and background image`() = runTest {
        val prefs = newPrefs()
        prefs.update {
            it.copy(
                widgets = listOf(
                    it.widgets.first { w -> w.type == HomeWidgetType.CLOCK },
                    it.widgets.first { w -> w.type == HomeWidgetType.SEARCH },
                ),
                background = it.background.copy(
                    type = BackgroundType.IMAGE,
                    imageUri = "file:///data/user/0/com.baicaohui.lightweb/files/home_background.jpg",
                ),
                overlayAlpha = 0.3f,
            )
        }
        val config = prefs.config.first()
        assertEquals(listOf(HomeWidgetType.CLOCK, HomeWidgetType.SEARCH), config.widgets.map { w -> w.type })
        assertEquals(BackgroundType.IMAGE, config.background.type)
        assertEquals(
            "file:///data/user/0/com.baicaohui.lightweb/files/home_background.jpg",
            config.background.imageUri,
        )
        assertEquals(0.3f, config.overlayAlpha, 0.001f)
    }

    @Test
    fun `content uri background is dropped on read after restart`() = runTest {
        val prefs = newPrefs()
        prefs.update {
            it.copy(
                background = it.background.copy(
                    type = BackgroundType.IMAGE,
                    imageUri = "content://media/1",
                ),
            )
        }
        assertEquals(null, prefs.config.first().background.imageUri)
    }
}
