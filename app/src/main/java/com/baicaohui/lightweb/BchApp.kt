package com.baicaohui.lightweb

import android.app.Application
import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.os.Process
import android.os.Build
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import com.baicaohui.lightweb.browser.AdBlocker
import com.baicaohui.lightweb.browser.AppDownloadManager
import com.baicaohui.lightweb.browser.CookieDataManager
import com.baicaohui.lightweb.browser.IncognitoProcess
import com.baicaohui.lightweb.browser.SessionStore
import com.baicaohui.lightweb.browser.TabManager
import com.baicaohui.lightweb.browser.TabThumbnailStore
import com.baicaohui.lightweb.browser.TrackerBlocker
import com.baicaohui.lightweb.browser.WebViewStore
import com.baicaohui.lightweb.browser.AdLevel
import com.baicaohui.lightweb.data.db.AppDatabase
import com.baicaohui.lightweb.data.prefs.BrowserPrefs
import com.baicaohui.lightweb.data.prefs.BrowserPrefsStore
import com.baicaohui.lightweb.data.prefs.DownloadLocation
import com.baicaohui.lightweb.data.prefs.HomePrefs
import com.baicaohui.lightweb.data.prefs.IncognitoPrefsFiles
import com.baicaohui.lightweb.data.prefs.RecentSearchStore
import com.baicaohui.lightweb.data.prefs.ThemePrefs
import com.baicaohui.lightweb.data.repo.BookmarkRepository
import com.baicaohui.lightweb.data.repo.DownloadRepository
import com.baicaohui.lightweb.data.repo.HistoryRepository
import com.baicaohui.lightweb.data.repo.ReaderCacheRepository
import com.baicaohui.lightweb.data.repo.ShortcutRepository
import com.baicaohui.lightweb.data.repo.SiteSettingsRepository
import com.baicaohui.lightweb.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "BchApp"

data class NavigationState(
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
)

class BchApp : Application() {

    /**
     * 低版本 Android WebView 会自动附加 `X-Requested-With: 包名` 请求头，
     * 哔哩哔哩等站点据此把页面降级为精简版。Chromium 在 BuildInfo.getAll 中读取包名时
     * 返回空串，可让该请求头不再携带应用包名。
     */
    override fun getPackageName(): String {
        try {
            val stackTrace = Thread.currentThread().stackTrace
            for (element in stackTrace) {
                if (element.className == "org.chromium.base.BuildInfo" &&
                    element.methodName == "getAll"
                ) {
                    return ""
                }
            }
        } catch (_: Exception) {
            // 忽略异常，回退默认包名
        }
        return super.getPackageName()
    }

    lateinit var themePrefs: ThemePrefs
        private set

    lateinit var homePrefs: HomePrefs
        private set

    lateinit var browserPrefsStore: BrowserPrefsStore
        private set

    private var incognitoProcess = false

    val tabManager: TabManager by lazy { TabManager(initialIncognito = incognitoProcess) }

    val adBlocker: AdBlocker by lazy { AdBlocker.fromResources(this) }

    val trackerBlocker: TrackerBlocker by lazy { TrackerBlocker.fromResources(this) }

    @Volatile
    var pendingUrl: String? = null

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var sessionStore: SessionStore

    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val bookmarkRepository by lazy {
        BookmarkRepository(database.bookmarkDao(), database.folderDao())
    }
    val historyRepository by lazy { HistoryRepository(database.historyDao()) }
    val recentSearchStore by lazy {
        if (incognitoProcess) RecentSearchStore.createIncognito(this) else RecentSearchStore.create(this)
    }
    val shortcutRepository by lazy { ShortcutRepository(database.shortcutDao()) }
    val siteSettingsRepository by lazy { SiteSettingsRepository(database.siteSettingDao()) }
    val downloadRepository by lazy { DownloadRepository(database.downloadDao()) }
    val readerCacheRepository by lazy { ReaderCacheRepository(database.readerCacheDao()) }

    /** 阅读模式使用的本地 Readability.js（Apache-2.0，随 APK 打包，不访问网络）。 */
    val readabilityJs: String by lazy {
        assets.open("reader/Readability.js").bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    val appDownloadManager by lazy {
        AppDownloadManager(
            store = downloadRepository,
            scope = appScope,
            stagingDir = { downloadsStagingDir() },
            finalizer = { staged, fileName, mime -> finalizeDownload(staged, fileName, mime) },
        )
    }
    @Volatile
    var currentBrowserPrefs: BrowserPrefs = BrowserPrefs.DEFAULT
        private set

    val navigationState = MutableStateFlow(NavigationState())

    /** 各标签页最近一次抓到的网页图标文件路径（用于添加书签时默认图标）。 */
    val pageIcons = MutableStateFlow<Map<Long, String>>(emptyMap())

    val webViewStore by lazy {
        WebViewStore(
            adBlocker = adBlocker,
            adLevel = { AdLevel.valueOf(currentBrowserPrefs.adLevel) },
            trackerBlocker = trackerBlocker,
            customRules = { currentBrowserPrefs.customAdRules },
        )
    }

    val tabThumbnailStore: TabThumbnailStore by lazy {
        TabThumbnailStore(
            thumbnailDir = File(filesDir, "thumbnails"),
            ioScope = appScope,
        )
    }

    val networkMonitor by lazy { NetworkMonitor(this) }

    override fun onCreate() {
        super.onCreate()
        incognitoProcess = isIncognitoProcess()
        if (incognitoProcess) {
            // 先清除上一次无痕会话残留数据，再切换数据目录；普通进程数据完全不受影响。
            purgeIncognitoData()
            IncognitoPrefsFiles.copyMainPrefsToIncognito(File(filesDir, "datastore"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                WebView.setDataDirectorySuffix(IncognitoProcess.SUFFIX)
            }
        }
        themePrefs = if (incognitoProcess) ThemePrefs.createIncognito(this) else ThemePrefs.create(this)
        homePrefs = if (incognitoProcess) HomePrefs.createIncognito(this) else HomePrefs.create(this)
        browserPrefsStore = if (incognitoProcess) {
            BrowserPrefsStore.createIncognito(this)
        } else {
            BrowserPrefsStore.create(this)
        }
        CookieManager.getInstance().setAcceptCookie(true)
        networkMonitor.start()
        if (!incognitoProcess) {
            sessionStore = SessionStore(getSharedPreferences("session", MODE_PRIVATE))
            sessionStore.load()?.let { tabManager.restore(it) }
        }
        appScope.launch {
            if (!incognitoProcess) {
                tabThumbnailStore.loadAll(tabManager.tabs.value.map { it.id }.toSet())
            }
        }
        if (!incognitoProcess) {
            appScope.launch {
                tabManager.tabs.collect {
                    if (!tabManager.incognito.value) {
                        tabManager.snapshots().let(sessionStore::save)
                    }
                }
            }
        }
        appScope.launch {
            browserPrefsStore.prefs.collect { prefs ->
                currentBrowserPrefs = prefs
            }
        }
        if (!incognitoProcess) {
            appScope.launch {
                val prefs = browserPrefsStore.prefs.first()
                if (prefs.clearCookiesOnExit) {
                    CookieManager.getInstance().removeAllCookies(null)
                }
                siteSettingsRepository.all.first()
                    .filter { it.clearOnExit == true }
                    .forEach { site ->
                        val header = listOf(
                            "https://${site.host}/",
                            "http://${site.host}/",
                        ).mapNotNull { CookieManager.getInstance().getCookie(it) }
                            .joinToString("; ")
                        CookieDataManager.expiredSetCookieEntries(site.host, header)
                            .forEach { (url, value) ->
                                CookieManager.getInstance().setCookie(url, value)
                            }
                    }
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            runCatching { CookieManager.getInstance().flush() }
        }
    }

    /** 删除无痕 WebView 数据目录（Cookie/网站数据/缓存），普通数据目录不受影响。 */
    fun purgeIncognitoData() {
        val purged = IncognitoProcess.purgeDataDir(dataDir)
        if (!purged) {
            Log.w(TAG, "Failed to fully purge incognito webview data dir: " +
                IncognitoProcess.dataDir(dataDir))
        }
    }

    private fun isIncognitoProcess(): Boolean {
        val processName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
                ?.runningAppProcesses
                ?.firstOrNull { it.pid == Process.myPid() }
                ?.processName
                ?: packageName
        }
        return IncognitoProcess.isIncognitoProcessName(processName, packageName)
    }

    /** 默认下载目录：Download 分类下的本应用目录（Android/data/<包名>/files/Download）。 */
    private fun appDownloadsDir(): File =
        getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir

    private fun downloadsStagingDir(): File =
        if (currentBrowserPrefs.downloadLocation == DownloadLocation.PUBLIC &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ) {
            File(cacheDir, "downloads")
        } else {
            appDownloadsDir()
        }

    private suspend fun finalizeDownload(staged: File, fileName: String, mimeType: String?): String =
        if (currentBrowserPrefs.downloadLocation == DownloadLocation.PUBLIC &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ) {
            moveToPublicDownloads(staged, fileName, mimeType)
        } else {
            staged.absolutePath
        }

    /** 通过 MediaStore 写入公共下载目录 Download/BCH（Android 10+，免存储权限）。 */
    private suspend fun moveToPublicDownloads(
        staged: File,
        fileName: String,
        mimeType: String?,
    ): String = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType ?: "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/BCH")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        if (uri == null) return@withContext staged.absolutePath
        val copied = runCatching {
            contentResolver.openOutputStream(uri)?.use { out ->
                staged.inputStream().use { it.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
            uri.toString()
        }
        if (copied.isSuccess) {
            staged.delete()
            copied.getOrThrow()
        } else {
            runCatching { contentResolver.delete(uri, null, null) }
            staged.absolutePath
        }
    }
}
