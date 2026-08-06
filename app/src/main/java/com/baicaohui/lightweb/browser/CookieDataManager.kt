package com.baicaohui.lightweb.browser

/**
 * Cookie 管理纯函数：解析 Cookie 头、生成按名称“过期删除”的 setCookie 键值。
 * WebView 公共 API 不支持按域名删除，采用“读取名称 → 写入过期值”实现主机级清理。
 */
object CookieDataManager {

    fun cookieNames(cookieHeader: String?): List<String> =
        cookieHeader?.split(';')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && it.contains('=') }
            ?.map { it.substringBefore('=').trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?: emptyList()

    fun expiredSetCookieEntries(host: String, cookieHeader: String?): List<Pair<String, String>> {
        val names = cookieNames(cookieHeader)
        val normalized = normalizeHost(host)
        if (names.isEmpty() || normalized.isEmpty()) return emptyList()
        return buildList {
            for (name in names) {
                add("https://$normalized/" to expiredFor(name))
                add("http://$normalized/" to expiredFor(name))
            }
        }
    }

    fun normalizeHost(host: String): String {
        var value = host.trim().lowercase()
        value = value.substringAfter("://", value)
        value = value.substringBefore('/').substringBefore(':')
        return value
    }

    private fun expiredFor(name: String): String =
        "$name=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/"
}
