package com.baicaohui.lightweb.browser

object HttpsMode {
    const val OFF = "OFF"
    const val PREFER = "PREFER"
    const val STRICT = "STRICT"
}

/** HTTPS 升级策略：OFF 不升级；PREFER 优先升级、失败回退 http；STRICT 强制升级、失败阻止。 */
object HttpsPolicy {

    fun shouldUpgrade(url: String, mode: String): Boolean =
        scheme(url) == "http" && mode != HttpsMode.OFF

    fun upgrade(url: String): String =
        if (scheme(url) == "http") "https://" + url.substringAfter("://") else url

    fun shouldFallback(url: String, mode: String): Boolean =
        scheme(url) == "https" && mode == HttpsMode.PREFER

    fun toHttp(url: String): String =
        if (scheme(url) == "https") "http://" + url.substringAfter("://") else url

    fun isInsecure(url: String): Boolean = scheme(url) == "http"

    fun isHttps(url: String): Boolean = scheme(url) == "https"

    private fun scheme(url: String): String =
        url.substringBefore(":", missingDelimiterValue = "").lowercase()
}
