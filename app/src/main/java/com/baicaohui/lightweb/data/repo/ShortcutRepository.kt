package com.baicaohui.lightweb.data.repo

import com.baicaohui.lightweb.data.db.ShortcutDao
import com.baicaohui.lightweb.data.db.ShortcutEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ShortcutRepository(private val dao: ShortcutDao) {

    val shortcuts: Flow<List<ShortcutEntity>> = dao.observe()

    suspend fun add(title: String, url: String, color: Long?): Long {
        val position = dao.observe().first().size
        return dao.insert(ShortcutEntity(title = title, url = url, color = color, position = position))
    }

    suspend fun update(shortcut: ShortcutEntity) = dao.update(shortcut)

    suspend fun delete(shortcut: ShortcutEntity) = dao.delete(shortcut)
}
