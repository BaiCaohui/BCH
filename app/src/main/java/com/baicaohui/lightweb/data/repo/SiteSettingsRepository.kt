package com.baicaohui.lightweb.data.repo

import com.baicaohui.lightweb.data.db.SiteSettingDao
import com.baicaohui.lightweb.data.db.SiteSettingEntity
import kotlinx.coroutines.flow.Flow

class SiteSettingsRepository(private val dao: SiteSettingDao) {

    fun observe(host: String): Flow<SiteSettingEntity?> = dao.observe(host)

    val all: Flow<List<SiteSettingEntity>> = dao.observeAll()

    suspend fun get(host: String): SiteSettingEntity? = dao.getByHost(host)

    suspend fun upsert(
        host: String,
        jsEnabled: Boolean? = null,
        adLevel: String? = null,
        desktopMode: Boolean? = null,
        safeBrowsing: Boolean? = null,
        thirdPartyCookies: Boolean? = null,
        location: Boolean? = null,
        camera: Boolean? = null,
        microphone: Boolean? = null,
        notifications: Boolean? = null,
        popups: Boolean? = null,
        autoplay: Boolean? = null,
        httpsUpgrade: Boolean? = null,
        clearOnExit: Boolean? = null,
        antiTracking: Boolean? = null,
    ) = dao.upsert(
        SiteSettingEntity(
            host = host,
            jsEnabled = jsEnabled,
            adLevel = adLevel,
            desktopMode = desktopMode,
            safeBrowsing = safeBrowsing,
            thirdPartyCookies = thirdPartyCookies,
            location = location,
            camera = camera,
            microphone = microphone,
            notifications = notifications,
            popups = popups,
            autoplay = autoplay,
            httpsUpgrade = httpsUpgrade,
            clearOnExit = clearOnExit,
            antiTracking = antiTracking,
        ),
    )

    suspend fun delete(setting: SiteSettingEntity) = dao.delete(setting)

    suspend fun clear() = dao.clear()
}
