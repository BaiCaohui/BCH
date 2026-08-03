package com.baicaohui.lightweb.data.repo

import com.baicaohui.lightweb.data.db.SiteSettingDao
import com.baicaohui.lightweb.data.db.SiteSettingEntity
import kotlinx.coroutines.flow.Flow

class SiteSettingsRepository(private val dao: SiteSettingDao) {

    fun observe(host: String): Flow<SiteSettingEntity?> = dao.observe(host)

    val all: Flow<List<SiteSettingEntity>> = dao.observeAll()

    suspend fun upsert(
        host: String,
        jsEnabled: Boolean? = null,
        adLevel: String? = null,
        desktopMode: Boolean? = null,
    ) = dao.upsert(SiteSettingEntity(host = host, jsEnabled = jsEnabled, adLevel = adLevel, desktopMode = desktopMode))

    suspend fun delete(setting: SiteSettingEntity) = dao.delete(setting)

    suspend fun clear() = dao.clear()
}
