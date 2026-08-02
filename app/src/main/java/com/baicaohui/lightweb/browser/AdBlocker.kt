package com.baicaohui.lightweb.browser

import android.content.Context
import com.baicaohui.lightweb.R

enum class AdLevel { OFF, BASIC, STRICT }

class AdBlocker(
    private val basicHosts: Set<String>,
    private val strictHosts: Set<String>,
) {

    fun isBlocked(url: String, level: AdLevel): Boolean {
        if (level == AdLevel.OFF) return false
        val host = UrlSecurity.extractHost(url) ?: return false
        val hosts = if (level == AdLevel.STRICT) basicHosts + strictHosts else basicHosts
        return hosts.any { it == host || host.endsWith(".$it") }
    }

    companion object {
        fun fromResources(context: Context): AdBlocker {
            fun read(id: Int): Set<String> =
                context.resources.openRawResource(id).bufferedReader().readLines()
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .toSet()
            return AdBlocker(
                basicHosts = read(R.raw.adblock_basic),
                strictHosts = read(R.raw.adblock_strict),
            )
        }
    }
}
