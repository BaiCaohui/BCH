package com.baicaohui.lightweb.ui.browser

/**
 * 三杠菜单图标的顺序与显隐规则：
 * 空列表表示使用默认全量顺序；非空列表为精确的可见项顺序（自动去重、过滤未知 id）；
 * 若过滤后为空（全部隐藏）则回退默认，保证菜单不会完全消失。
 */
object MenuOrder {

    val DEFAULT_ORDER = listOf(
        "reload",
        "reader",
        "incognito",
        "ua",
        "downloads",
        "download_page",
        "cache_page",
        "cached_pages",
        "sniffer",
        "console",
        "add_bookmark",
        "bookmarks",
        "history",
        "settings",
    )

    fun resolve(stored: List<String>): List<String> {
        if (stored.isEmpty()) return DEFAULT_ORDER
        val known = DEFAULT_ORDER.toSet()
        val filtered = stored.filter { it in known }.distinct()
        return if (filtered.isEmpty()) DEFAULT_ORDER else filtered
    }

    fun <T> move(list: List<T>, from: Int, to: Int): List<T> {
        if (from !in list.indices || to !in list.indices) return list
        val mutable = list.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        return mutable
    }
}
