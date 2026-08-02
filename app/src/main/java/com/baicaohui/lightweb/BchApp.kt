package com.baicaohui.lightweb

import android.app.Application
import com.baicaohui.lightweb.browser.AdBlocker
import com.baicaohui.lightweb.browser.TabManager
import com.baicaohui.lightweb.data.prefs.ThemePrefs

class BchApp : Application() {

    lateinit var themePrefs: ThemePrefs
        private set

    val tabManager: TabManager = TabManager()

    val adBlocker: AdBlocker by lazy { AdBlocker.fromResources(this) }

    @Volatile
    var pendingUrl: String? = null

    override fun onCreate() {
        super.onCreate()
        themePrefs = ThemePrefs.create(this)
    }
}
