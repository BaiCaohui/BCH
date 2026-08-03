package com.baicaohui.lightweb

import android.app.Application
import com.baicaohui.lightweb.browser.SessionStore
import com.baicaohui.lightweb.browser.AdBlocker
import com.baicaohui.lightweb.browser.TabManager
import com.baicaohui.lightweb.browser.WebViewStore
import com.baicaohui.lightweb.browser.AdLevel
import com.baicaohui.lightweb.data.db.AppDatabase
import com.baicaohui.lightweb.data.prefs.ThemePrefs
import com.baicaohui.lightweb.data.prefs.HomePrefs
import com.baicaohui.lightweb.data.repo.BookmarkRepository
import com.baicaohui.lightweb.data.repo.HistoryRepository
import com.baicaohui.lightweb.data.repo.ShortcutRepository
import com.baicaohui.lightweb.data.repo.SiteSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BchApp : Application() {

    lateinit var themePrefs: ThemePrefs
        private set

    lateinit var homePrefs: HomePrefs
        private set

    val tabManager: TabManager = TabManager()

    val adBlocker: AdBlocker by lazy { AdBlocker.fromResources(this) }

    @Volatile
    var pendingUrl: String? = null

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val bookmarkRepository by lazy {
        BookmarkRepository(database.bookmarkDao(), database.folderDao())
    }
    val historyRepository by lazy { HistoryRepository(database.historyDao()) }
    val shortcutRepository by lazy { ShortcutRepository(database.shortcutDao()) }
    val siteSettingsRepository by lazy { SiteSettingsRepository(database.siteSettingDao()) }
    val webViewStore by lazy { WebViewStore(adBlocker) { AdLevel.BASIC } }

    override fun onCreate() {
        super.onCreate()
        themePrefs = ThemePrefs.create(this)
        homePrefs = HomePrefs.create(this)
        val sessionStore = SessionStore(getSharedPreferences("session", MODE_PRIVATE))
        sessionStore.load()?.let { tabManager.restore(it) }
        appScope.launch {
            tabManager.tabs.collect { tabManager.snapshots().let(sessionStore::save) }
        }
    }
}
