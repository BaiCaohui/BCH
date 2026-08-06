package com.baicaohui.lightweb.data.repo

import com.baicaohui.lightweb.data.db.ReaderCacheDao
import com.baicaohui.lightweb.data.db.ReaderCacheEntity

interface ReaderCacheStore {
    suspend fun get(url: String): ReaderCacheEntity?
    suspend fun put(entity: ReaderCacheEntity)
    suspend fun delete(url: String)
    suspend fun clear()
}

class ReaderCacheRepository(private val dao: ReaderCacheDao) : ReaderCacheStore {
    override suspend fun get(url: String): ReaderCacheEntity? = dao.get(url)
    override suspend fun put(entity: ReaderCacheEntity) = dao.upsert(entity)
    override suspend fun delete(url: String) = dao.delete(url)
    suspend fun deleteByHost(host: String) =
        dao.deleteByHost("%" + host.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%")
    override suspend fun clear() = dao.clear()
}
