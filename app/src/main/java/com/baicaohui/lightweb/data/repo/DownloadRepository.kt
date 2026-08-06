package com.baicaohui.lightweb.data.repo

import com.baicaohui.lightweb.data.db.DownloadDao
import com.baicaohui.lightweb.data.db.DownloadEntity
import kotlinx.coroutines.flow.Flow

interface DownloadStore {
    val downloads: Flow<List<DownloadEntity>>
    suspend fun insert(entity: DownloadEntity): Long
    suspend fun update(entity: DownloadEntity)
    suspend fun delete(entity: DownloadEntity)
    suspend fun clear()
}

class DownloadRepository(private val dao: DownloadDao) : DownloadStore {
    override val downloads: Flow<List<DownloadEntity>> = dao.observeAll()
    override suspend fun insert(entity: DownloadEntity): Long = dao.insert(entity)
    override suspend fun update(entity: DownloadEntity) = dao.update(entity)
    override suspend fun delete(entity: DownloadEntity) = dao.delete(entity)
    override suspend fun clear() = dao.clear()
}
