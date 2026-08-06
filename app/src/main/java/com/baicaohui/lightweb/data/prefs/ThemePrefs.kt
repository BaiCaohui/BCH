package com.baicaohui.lightweb.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.baicaohui.lightweb.ui.theme.DarkMode
import com.baicaohui.lightweb.ui.theme.ShapeStyle
import com.baicaohui.lightweb.ui.theme.ThemeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.themeDataStore by preferencesDataStore(name = "theme")

class ThemePrefs(private val dataStore: DataStore<Preferences>) {

    val config: Flow<ThemeConfig> = dataStore.data.map { it.toThemeConfig() }

    suspend fun update(transform: (ThemeConfig) -> ThemeConfig) {
        dataStore.edit { prefs ->
            val next = transform(prefs.toThemeConfig())
            prefs[Keys.SEED] = next.seedColor
            prefs[Keys.DYNAMIC] = next.useDynamicColor
            prefs[Keys.DARK_MODE] = next.darkMode.name
            prefs[Keys.FONT_SCALE] = next.fontScale
            prefs[Keys.SHAPE] = next.shapeStyle.name
            prefs[Keys.COMPACT] = next.compact
        }
    }

    private fun Preferences.toThemeConfig(): ThemeConfig = ThemeConfig(
        seedColor = this[Keys.SEED] ?: ThemeConfig.DEFAULT.seedColor,
        useDynamicColor = this[Keys.DYNAMIC] ?: ThemeConfig.DEFAULT.useDynamicColor,
        darkMode = this[Keys.DARK_MODE]
            ?.let { runCatching { DarkMode.valueOf(it) }.getOrDefault(ThemeConfig.DEFAULT.darkMode) }
            ?: ThemeConfig.DEFAULT.darkMode,
        fontScale = this[Keys.FONT_SCALE] ?: ThemeConfig.DEFAULT.fontScale,
        shapeStyle = this[Keys.SHAPE]
            ?.let { runCatching { ShapeStyle.valueOf(it) }.getOrDefault(ThemeConfig.DEFAULT.shapeStyle) }
            ?: ThemeConfig.DEFAULT.shapeStyle,
        compact = this[Keys.COMPACT] ?: ThemeConfig.DEFAULT.compact,
    )

    private object Keys {
        val SEED = longPreferencesKey("seed_color")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val SHAPE = stringPreferencesKey("shape_style")
        val COMPACT = booleanPreferencesKey("compact")
    }

    companion object {
        fun create(context: Context): ThemePrefs = ThemePrefs(context.themeDataStore)

        fun createIncognito(context: Context): ThemePrefs = ThemePrefs(
            PreferenceDataStoreFactory.create {
                IncognitoPrefsFiles.targetFile(File(context.filesDir, "datastore"), "theme")
            },
        )
    }
}
