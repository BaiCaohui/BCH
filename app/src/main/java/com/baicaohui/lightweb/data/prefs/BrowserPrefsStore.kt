package com.baicaohui.lightweb.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.browserDataStore by preferencesDataStore(name = "browser")

private val json = Json { ignoreUnknownKeys = true }

class BrowserPrefsStore(private val dataStore: DataStore<Preferences>) {

    val prefs: Flow<BrowserPrefs> = dataStore.data.map { prefs ->
        prefs[Keys.PREFS]?.let { raw ->
            runCatching { json.decodeFromString<BrowserPrefs>(raw) }.getOrNull()
        } ?: BrowserPrefs.DEFAULT
    }

    suspend fun update(transform: (BrowserPrefs) -> BrowserPrefs) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.PREFS]?.let { raw ->
                runCatching { json.decodeFromString<BrowserPrefs>(raw) }.getOrNull()
            } ?: BrowserPrefs.DEFAULT
            prefs[Keys.PREFS] = json.encodeToString(BrowserPrefs.serializer(), transform(current))
        }
    }

    private object Keys {
        val PREFS = stringPreferencesKey("prefs")
    }

    companion object {
        fun create(context: Context): BrowserPrefsStore = BrowserPrefsStore(context.browserDataStore)
    }
}
