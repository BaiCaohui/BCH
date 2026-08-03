package com.baicaohui.lightweb.data.repo

import com.baicaohui.lightweb.data.db.HistoryDao
import com.baicaohui.lightweb.data.db.HistoryEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: HistoryDao) : HistoryRecorder {

    val all: Flow<List<HistoryEntity>> = dao.observeAll()

    fun recent(limit: Int): Flow<List<HistoryEntity>> = dao.observeRecent(limit)

    override suspend fun record(url: String, title: String) {
        val existing = dao.findByUrl(url)
        if (existing != null) {
            dao.update(
                existing.copy(
                    title = title.ifBlank { existing.title },
                    visitTime = System.currentTimeMillis(),
                    visitCount = existing.visitCount + 1,
                ),
            )
        } else {
            dao.insert(HistoryEntity(url = url, title = title))
        }
    }

    suspend fun delete(url: String) = dao.deleteByUrl(url)

    suspend fun clear() = dao.clear()
}
