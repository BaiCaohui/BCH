package com.baicaohui.lightweb.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.baicaohui.lightweb.ui.theme.DarkMode
import com.baicaohui.lightweb.ui.theme.ShapeStyle
import com.baicaohui.lightweb.ui.theme.ThemeConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ThemePrefsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun newPrefs() = ThemePrefs(
        PreferenceDataStoreFactory.create { tmp.newFile("test-${System.nanoTime()}.preferences_pb") },
    )

    @Test
    fun `defaults when store is empty`() = runTest {
        assertEquals(ThemeConfig.DEFAULT, newPrefs().config.first())
    }

    @Test
    fun `update persists and flows new value`() = runTest {
        val prefs = newPrefs()
        prefs.update {
            it.copy(
                seedColor = 0xFFE91E63,
                useDynamicColor = false,
                darkMode = DarkMode.DARK,
                fontScale = 1.2f,
                shapeStyle = ShapeStyle.ROUNDED,
                compact = true,
            )
        }
        val config = prefs.config.first()
        assertEquals(0xFFE91E63, config.seedColor)
        assertEquals(false, config.useDynamicColor)
        assertEquals(DarkMode.DARK, config.darkMode)
        assertEquals(1.2f, config.fontScale, 0.001f)
        assertEquals(ShapeStyle.ROUNDED, config.shapeStyle)
        assertEquals(true, config.compact)
    }
}
