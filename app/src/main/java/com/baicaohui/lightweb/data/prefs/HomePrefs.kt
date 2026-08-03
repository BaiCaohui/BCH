package com.baicaohui.lightweb.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.baicaohui.lightweb.ui.home.HomeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.homeDataStore by preferencesDataStore(name = "home")

private val json = Json { ignoreUnknownKeys = true }

class HomePrefs(private val dataStore: DataStore<Preferences>) {

    val config: Flow<HomeConfig> = dataStore.data.map { prefs ->
        prefs[Keys.CONFIG]?.let { raw ->
            runCatching { json.decodeFromString<HomeConfig>(raw) }.getOrNull()
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
    }
}
