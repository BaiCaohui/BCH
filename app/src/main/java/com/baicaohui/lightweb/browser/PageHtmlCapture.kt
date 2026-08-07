package com.baicaohui.lightweb.browser

import kotlinx.serialization.json.Json

/** 抓取当前网页 HTML 的脚本与结果解析（evaluateJavascript 返回 JSON 编码字符串）。 */
object PageHtmlCapture {
    private val json = Json { ignoreUnknownKeys = true }

    fun outerHtmlScript(): String =
        "(function(){try{return document.documentElement.outerHTML}catch(e){return ''}})()"

    fun parseHtml(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return runCatching { json.decodeFromString<String>(raw) }.getOrDefault(raw)
    }
}
