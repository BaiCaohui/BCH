package com.baicaohui.lightweb.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.baicaohui.lightweb.ui.home.HomeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.File

private val Context.homeDataStore by preferencesDataStore(name = "home")

private val json = Json { ignoreUnknownKeys = true }

class HomePrefs(private val dataStore: DataStore<Preferences>) {

    val config: Flow<HomeConfig> = dataStore.data.map { prefs ->
        prefs[Keys.CONFIG]?.let { raw ->
            runCatching { json.decodeFromString<HomeConfig>(raw) }.getOrNull()
        }?.let { config ->
            // 旧版本直接保存相册 content:// URI，重启后权限失效；读取时清除，避免空白背景。
            val uri = config.background.imageUri
            if (uri != null && uri.startsWith("content://")) {
                config.copy(background = config.background.copy(imageUri = null))
            } else {
                config
            }
        } ?: HomeConfig.DEFAULT
    }

    suspend fun update(transform: (HomeConfig) -> HomeConfig) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.CONFIG]?.let { raw ->
                runCatching { json.decodeFromString<HomeConfig>(raw) }.getOrNull()
            } ?: HomeConfig.DEFAULT
            prefs[Keys.CONFIG] = json.encodeToString(HomeConfig.serializer(), transform(current))
        }
    }

    private object Keys {
        val CONFIG = stringPreferencesKey("config")
    }

    companion object {
        fun create(context: Context): HomePrefs = HomePrefs(context.homeDataStore)

        fun createIncognito(context: Context): HomePrefs = HomePrefs(
            PreferenceDataStoreFactory.create {
                IncognitoPrefsFiles.targetFile(File(context.filesDir, "datastore"), "home")
            },
        )
    }
}
