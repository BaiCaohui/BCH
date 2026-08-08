package com.baicaohui.lightweb.browser

import android.content.Context
import com.baicaohui.lightweb.R

enum class AdLevel { OFF, BASIC, STRICT }

/** 用户自定义屏蔽规则：`||host`=主机/子域，含 `*`=通配符 URL，含 `/`=URL 子串，纯域名=主机/子域。 */
object CustomAdRules {

    fun matches(url: String, rule: String): Boolean {
        val trimmed = rule.trim().lowercase()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return false
        val lowerUrl = url.lowercase()
        return when {
            trimmed.startsWith("||") -> {
                val host = trimmed.removePrefix("||")
                if (host.isEmpty()) return false
                val h = UrlSecurity.extractHost(lowerUrl) ?: return false
                h == host || h.endsWith(".$host")
            }
            trimmed.contains("*") -> {
                val regex = Regex(trimmed.replace(".", "\\.").replace("*", ".*"))
                regex.containsMatchIn(lowerUrl)
            }
            trimmed.contains("/") -> lowerUrl.contains(trimmed)
            else -> {
                val h = UrlSecurity.extractHost(lowerUrl) ?: return false
                h == trimmed || h.endsWith(".$trimmed")
            }
        }
    }
}

class AdBlocker(
    private val basicHosts: Set<String>,
    private val strictHosts: Set<String>,
    private val adguardHosts: Set<String> = emptySet(),
) {

    /** 预合并各档位生效的主机集合，避免每次请求时重复拼接。 */
    private val basicEffective: Set<String> = basicHosts + adguardHosts
    private val strictEffective: Set<String> = basicHosts + strictHosts + adguardHosts

    fun isBlocked(
        url: String,
        level: AdLevel,
        customRules: List<String> = emptyList(),
        markedHosts: Set<String> = emptySet(),
    ): Boolean {
        val host = UrlSecurity.extractHost(url) ?: return false
        if (markedHosts.any { it == host || host.endsWith(".$it") }) return true
        if (level != AdLevel.OFF) {
            val hosts = if (level == AdLevel.STRICT) strictEffective else basicEffective
            if (hosts.any { it == host || host.endsWith(".$it") }) return true
        }
        return customRules.any { CustomAdRules.matches(url, it) }
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
                adguardHosts = read(R.raw.adguard_hosts),
            )
        }
    }
}
