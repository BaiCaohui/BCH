package com.baicaohui.lightweb.ui.home

import com.baicaohui.lightweb.data.db.HistoryEntity

/** 首页搜索框的历史推荐：按标题/网址匹配，未输入时返回最近记录。 */
object HistorySuggestions {

    fun matches(query: String, title: String, url: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return title.contains(q, ignoreCase = true) || url.contains(q, ignoreCase = true)
    }

    fun suggest(
        entries: List<HistoryEntity>,
        query: String,
        limit: Int = 8,
    ): List<HistoryEntity> =
        entries.filter { matches(query, it.title, it.url) }.take(limit)
}
