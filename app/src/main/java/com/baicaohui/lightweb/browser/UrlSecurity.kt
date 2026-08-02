package com.baicaohui.lightweb.browser

import java.net.URLEncoder

object UrlSecurity {

    private val ALLOWED_SCHEMES = setOf("http", "https", "about")

    fun normalize(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        return if (looksLikeDomain(trimmed)) "https://$trimmed" else trimmed
    }

    fun isSafeUrl(url: String): Boolean = schemeOf(url) in ALLOWED_SCHEMES

    fun schemeOf(url: String): String? =
        url.substringBefore(":", missingDelimiterValue = "").lowercase().ifEmpty { null }

    fun isHttpUrl(url: String): Boolean = schemeOf(url) in setOf("http", "https")

    fun toSearchUrl(query: String, template: String): String =
        template.replace("%s", URLEncoder.encode(query.trim(), "UTF-8"))

    fun extractHost(url: String): String? {
        val rest = url.substringAfter("://", missingDelimiterValue = url)
        val authority = rest.substringBefore('/').substringBefore('?').substringBefore('#')
        val host = authority.substringBefore('@').substringBefore(':').lowercase()
        return host.takeIf {
            it.isNotEmpty() && it.all { c -> c.isLetterOrDigit() || c in ".-_" }
        }
    }

    private fun looksLikeDomain(input: String): Boolean =
        input.contains(".") &&
            !input.contains(" ") &&
            schemeOf(input) == null &&
            input.all { it.isLetterOrDigit() || it in ".-/" }
}
