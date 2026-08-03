package com.baicaohui.lightweb

import android.app.Application
import android.content.ComponentCallbacks2
import android.webkit.CookieManager
import com.baicaohui.lightweb.browser.SessionStore
import com.baicaohui.lightweb.browser.AdBlocker
import com.baicaohui.lightweb.browser.TabManager
import com.baicaohui.lightweb.browser.TabThumbnailStore
import com.baicaohui.lightweb.browser.WebViewStore
import com.baicaohui.lightweb.browser.AdLevel
import com.baicaohui.lightweb.data.db.AppDatabase
import com.baicaohui.lightweb.data.prefs.ThemePrefs
import com.baicaohui.lightweb.data.prefs.HomePrefs
import com.baicaohui.lightweb.data.prefs.BrowserPrefs
import com.baicaohui.lightweb.data.prefs.BrowserPrefsStore
import com.baicaohui.lightweb.data.repo.BookmarkRepository
import com.baicaohui.lightweb.data.repo.HistoryRepository
import com.baicaohui.lightweb.data.repo.ShortcutRepository
import com.baicaohui.lightweb.data.repo.SiteSettingsRepository
import com.baicaohui.lightweb.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BchApp : Application() {

    lateinit var themePrefs: ThemePrefs
        private set

    lateinit var homePrefs: HomePrefs
        private set

    lateinit var browserPrefsStore: BrowserPrefsStore
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
    @Volatile
    var currentBrowserPrefs: BrowserPrefs = BrowserPrefs.DEFAULT
        private set

    val webViewStore by lazy {
        WebViewStore(adBlocker) {
            AdLevel.valueOf(currentBrowserPrefs.adLevel)
        }
    }

    val tabThumbnailStore = TabThumbnailStore()

    val networkMonitor by lazy { NetworkMonitor(this) }

    override fun onCreate() {
        super.onCreate()
        themePrefs = ThemePrefs.create(this)
        homePrefs = HomePrefs.create(this)
        browserPrefsStore = BrowserPrefsStore.create(this)
        CookieManager.getInstance().setAcceptCookie(true)
        networkMonitor.start()
        val sessionStore = SessionStore(getSharedPreferences("session", MODE_PRIVATE))
        sessionStore.load()?.let { tabManager.restore(it) }
        appScope.launch {
            tabManager.tabs.collect { tabManager.snapshots().let(sessionStore::save) }
        }
        appScope.launch {
            browserPrefsStore.prefs.collect { prefs ->
                currentBrowserPrefs = prefs
                tabManager.setMaxTabs(prefs.maxTabs)
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            runCatching { CookieManager.getInstance().flush() }
        }
    }
}
