package com.baicaohui.lightweb.data.repo

import com.baicaohui.lightweb.data.db.MarkedAdDao
import com.baicaohui.lightweb.data.db.MarkedAdEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MarkedAdRepository(private val dao: MarkedAdDao) {
    val all: Flow<List<MarkedAdEntity>> = dao.observeAll()

    /** 所有启用记录中收集到的广告主机（用于网络层自动屏蔽）。 */
    val enabledHosts: Flow<Set<String>> = dao.observeAll().map { list ->
        list.asSequence()
            .filter { it.enabled }
            .flatMap { it.adHosts.split(',') }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    /** 指定站点下启用中的元素隐藏规则。 */
    suspend fun byHost(host: String): List<MarkedAdEntity> =
        dao.byHost(host).filter { it.enabled }

    suspend fun insert(entity: MarkedAdEntity): Long = dao.insert(entity)

    suspend fun setEnabled(entity: MarkedAdEntity, enabled: Boolean) =
        dao.update(entity.copy(enabled = enabled))

    suspend fun delete(entity: MarkedAdEntity) = dao.delete(entity)

    suspend fun clear() = dao.clear()
}
