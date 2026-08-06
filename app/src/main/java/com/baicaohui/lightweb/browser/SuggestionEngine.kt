package com.baicaohui.lightweb.browser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 当前搜索引擎的联想建议。
 * 按搜索模板中的主机名映射到各引擎的联想接口（Bing/Baidu/Google），
 * 自定义搜索引擎不提供联想。
 */
object SuggestionEngine {

    private val json = Json { ignoreUnknownKeys = true }

    fun endpointFor(template: String): String? {
        val host = UrlSecurity.extractHost(template) ?: return null
        return when {
            host.contains("bing.com") -> "https://api.bing.com/osjson.aspx?query=%s"
            host.contains("baidu.com") -> "https://suggestion.baidu.com/su?wd=%s&json=1"
            host.contains("google.com") -> "https://suggestqueries.google.com/complete/search?client=firefox&q=%s"
            else -> null
        }
    }

    fun parseBing(body: String): List<String> = parsePairJson(body)

    fun parseGoogle(body: String): List<String> = parsePairJson(body)

    fun parseBaidu(body: String): List<String> = runCatching {
        val jsonBody = body
            .substringAfter('(')
            .substringBeforeLast(')')
            .takeIf { it.isNotBlank() }
            ?: body
        json.parseToJsonElement(jsonBody).jsonObject["s"]
            ?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?: emptyList()
    }.getOrDefault(emptyList())

    suspend fun fetch(template: String, query: String): List<String> = withContext(Dispatchers.IO) {
        val endpoint = endpointFor(template) ?: return@withContext emptyList()
        val url = endpoint.replace("%s", URLEncoder.encode(query.trim(), "UTF-8"))
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", BrowserWebView.ANDROID_UA)
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode !in 200..299) return@withContext emptyList()
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (endpoint.contains("baidu.com")) parseBaidu(body) else parsePairJson(body)
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    private fun parsePairJson(body: String): List<String> = runCatching {
        json.parseToJsonElement(body).jsonArray
            .getOrNull(1)
            ?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?: emptyList()
    }.getOrDefault(emptyList())
}
