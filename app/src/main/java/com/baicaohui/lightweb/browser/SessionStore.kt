package com.baicaohui.lightweb.browser

import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SessionStore(private val prefs: SharedPreferences) {

    fun load(): List<TabSnapshot>? {
        val json = prefs.getString(KEY, null) ?: return null
        return runCatching { Json.decodeFromString<List<TabSnapshot>>(json) }.getOrNull()
    }

    fun save(snapshots: List<TabSnapshot>) {
        prefs.edit().putString(KEY, Json.encodeToString(snapshots)).apply()
    }

    private companion object {
        const val KEY = "tabs"
    }
}
