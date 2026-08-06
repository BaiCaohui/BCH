package com.baicaohui.lightweb.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val Context.recentSearchDataStore by preferencesDataStore(name = "recent_searches")

/** 记录用户在地址栏/主页发起的搜索关键词（不含直接打开的网址）。 */
interface SearchRecorder {
    suspend fun record(query: String)
}

class RecentSearchStore(private val dataStore: DataStore<Preferences>) : SearchRecorder {

    private val json = Json { ignoreUnknownKeys = true }

    val recent: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[Keys.RECENT]?.let { raw ->
            runCatching { json.decodeFromString<List<String>>(raw) }.getOrNull()
        } ?: emptyList()
    }

    override suspend fun record(query: String) {
        val input = query.trim()
        if (input.isEmpty()) return
        dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT]?.let { raw ->
                runCatching { json.decodeFromString<List<String>>(raw) }.getOrNull()
            } ?: emptyList()
            prefs[Keys.RECENT] =
                json.encodeToString((listOf(input) + current.filterNot { it == input }).take(MAX_RECENT))
        }
    }

    private object Keys {
        val RECENT = stringPreferencesKey("recent")
    }

    companion object {
        const val MAX_RECENT = 10

        fun create(context: Context): RecentSearchStore =
            RecentSearchStore(context.recentSearchDataStore)

        fun createIncognito(context: Context): RecentSearchStore = RecentSearchStore(
            PreferenceDataStoreFactory.create {
                IncognitoPrefsFiles.targetFile(File(context.filesDir, "datastore"), "recent_searches")
            },
        )
    }
}
