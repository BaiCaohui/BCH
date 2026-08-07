package com.baicaohui.lightweb.ui.browser

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.Manifest
import android.net.Uri
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.IncognitoActivity
import com.baicaohui.lightweb.NavigationState
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.browser.BrowserWebView
import com.baicaohui.lightweb.browser.DownloadFormat
import com.baicaohui.lightweb.browser.DownloadHandler
import com.baicaohui.lightweb.browser.DownloadRisk
import com.baicaohui.lightweb.browser.DownloadRiskPolicy
import com.baicaohui.lightweb.browser.DownloadNames
import com.baicaohui.lightweb.browser.HttpsMode
import com.baicaohui.lightweb.browser.HttpsPolicy
import com.baicaohui.lightweb.browser.HttpDownloader
import com.baicaohui.lightweb.browser.NotificationPolicy
import com.baicaohui.lightweb.browser.PageContextMenus
import com.baicaohui.lightweb.browser.PageCapture
import com.baicaohui.lightweb.browser.PermissionDecision
import com.baicaohui.lightweb.browser.PermissionMapping
import com.baicaohui.lightweb.browser.PermissionKind
import com.baicaohui.lightweb.browser.ReaderModeController
import com.baicaohui.lightweb.browser.ResourceSniffer
import com.baicaohui.lightweb.browser.SelectionInfo
import com.baicaohui.lightweb.browser.SitePermissionPolicy
import com.baicaohui.lightweb.browser.Tab
import com.baicaohui.lightweb.browser.TabStatus
import com.baicaohui.lightweb.browser.UrlSecurity
import com.baicaohui.lightweb.browser.SafeBrowsingThreats
import com.baicaohui.lightweb.browser.WebCallbacks
import com.baicaohui.lightweb.data.db.ReaderCacheEntity
import com.baicaohui.lightweb.data.db.CachedPageEntity
import com.baicaohui.lightweb.data.prefs.ToolbarPosition
import com.baicaohui.lightweb.data.prefs.DownloadMode
import com.baicaohui.lightweb.util.BookmarkIconStore
import com.baicaohui.lightweb.ui.theme.DarkMode
import com.baicaohui.lightweb.ui.theme.ThemeConfig
import java.io.File
import kotlinx.coroutines.Dispatchers
import com.baicaohui.lightweb.ui.components.ErrorPage
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

private const val SEARCH_TEMPLATE = "https://www.bing.com/search?q=%s"

private data class SafeBrowsingAlert(
    val url: String,
    val threatType: Int,
    val handler: SafeBrowsingResponse,
)

private data class DownloadRequest(
    val url: String,
    val userAgent: String,
    val mimeType: String?,
    val contentDisposition: String?,
    val sizeBytes: Long? = null,
)

private data class GeolocationPrompt(
    val origin: String,
    val callback: GeolocationPermissions.Callback,
)

private data class LinkMenuState(
    val url: String,
    val text: String,
)

private data class ImageMenuState(
    val url: String,
    val name: String,
)

@Composable
fun BrowserScreen(
    initialUrl: String? = null,
    cachedPage: CachedPageEntity? = null,
    sharedViewModel: BrowserViewModel? = null,
) {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val viewModel: BrowserViewModel = sharedViewModel ?: viewModel(
        factory = browserViewModelFactory(app.tabManager, app.historyRepository, app.recentSearchStore),
    )
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val currentId by viewModel.currentId.collectAsStateWithLifecycle()
    val incognito by app.tabManager.incognito.collectAsStateWithLifecycle()
    val activeTab = tabs.firstOrNull { it.id == currentId }

    var addressText by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    var editingPageInfo by remember { mutableStateOf(false) }
    var pageInfoTitle by remember { mutableStateOf("") }
    var pageInfoUrl by remember { mutableStateOf("") }
    var pendingExternal by remember { mutableStateOf<String?>(null) }
    var pendingSsl by remember { mutableStateOf<Pair<String, SslErrorHandler>?>(null) }
    var pendingPermission by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingSafeBrowsing by remember {
        mutableStateOf<SafeBrowsingAlert?>(null)
    }
    var pendingDownload by remember { mutableStateOf<DownloadRequest?>(null) }
    var pendingGeolocation by remember { mutableStateOf<GeolocationPrompt?>(null) }
    var pendingPopup by remember { mutableStateOf<String?>(null) }
    var pendingHttpsBlock by remember { mutableStateOf<String?>(null) }
    var cachePending by remember { mutableStateOf(cachedPage) }
    var textMenu by remember { mutableStateOf<SelectionInfo?>(null) }
    var linkMenu by remember { mutableStateOf<LinkMenuState?>(null) }
    var imageMenu by remember { mutableStateOf<ImageMenuState?>(null) }

    val recentSearches by app.recentSearchStore.recent.collectAsStateWithLifecycle(initialValue = emptyList())
    val visibleRecentSearches = if (incognito) emptyList() else recentSearches
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboard = LocalClipboardManager.current
    val density = LocalDensity.current.density

    val downloadHandler = remember { DownloadHandler(context) }
    val webViewStore = app.webViewStore
    val readerController = remember { ReaderModeController { app.readabilityJs } }
    val browserPrefs by app.browserPrefsStore.prefs.collectAsStateWithLifecycle(
        initialValue = app.currentBrowserPrefs,
    )
    val scope = rememberCoroutineScope()
    val online by app.networkMonitor.online.collectAsStateWithLifecycle(initialValue = true)
    val themeConfig by app.themePrefs.config.collectAsStateWithLifecycle(initialValue = ThemeConfig.DEFAULT)
    val readerTheme = when (themeConfig.darkMode) {
        DarkMode.SYSTEM -> if (isSystemInDarkTheme()) "dark" else "light"
        DarkMode.LIGHT -> "light"
        DarkMode.DARK -> "dark"
    }

    fun updateNavState() {
        val wv = currentId?.let { webViewStore.get(it) }
        app.navigationState.value = NavigationState(
            canGoBack = wv?.canGoBack() == true,
            canGoForward = wv?.canGoForward() == true,
        )
    }

    fun currentTabUrl(): String =
        viewModel.tabs.value.firstOrNull { it.id == viewModel.currentId.value }?.url.orEmpty()

    fun startDownload(request: DownloadRequest) {
        if (app.currentBrowserPrefs.downloadMode == DownloadMode.SYSTEM) {
            downloadHandler.start(request.url, request.userAgent, request.mimeType)
        } else {
            scope.launch {
                app.appDownloadManager.enqueue(
                    request.url,
                    request.userAgent,
                    request.mimeType,
                    request.contentDisposition,
                )
            }
        }
        Toast.makeText(context, R.string.download_started, Toast.LENGTH_SHORT).show()
    }

    fun currentWv(): BrowserWebView? =
        viewModel.currentId.value?.let { webViewStore.get(it) }

    fun copySelectionText() {
        val wv = currentWv() ?: return
        wv.evaluateJavascript(PageContextMenus.selectionTextScript()) { raw ->
            val text = PageContextMenus.parseText(raw)
            if (text.isNotBlank()) {
                clipboard.setText(AnnotatedString(text))
                Toast.makeText(context, R.string.context_copied, Toast.LENGTH_SHORT).show()
            }
            textMenu = null
        }
    }

    fun selectAllText() {
        val wv = currentWv() ?: return
        wv.evaluateJavascript(PageContextMenus.selectAllScript()) {
            wv.evaluateJavascript(PageContextMenus.selectionInfoScript()) { raw ->
                textMenu = PageContextMenus.parseSelectionInfo(raw)
            }
        }
    }

    fun searchSelectionInNewTab() {
        val wv = currentWv() ?: return
        wv.evaluateJavascript(PageContextMenus.selectionTextScript()) { raw ->
            val text = PageContextMenus.parseText(raw)
            if (text.isNotBlank()) {
                app.tabManager.newTab(UrlSecurity.toSearchUrl(text, browserPrefs.searchTemplate))
            }
            textMenu = null
        }
    }

    fun openInNewTab(url: String) {
        app.tabManager.newTab(url)
    }

    fun openInIncognito(url: String) {
        context.startActivity(
            Intent(context, IncognitoActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(IncognitoActivity.EXTRA_URL, url),
        )
    }

    fun copyText(text: String, toastRes: Int) {
        if (text.isNotBlank()) {
            clipboard.setText(AnnotatedString(text))
            Toast.makeText(context, toastRes, Toast.LENGTH_SHORT).show()
        }
    }

    fun downloadFromMenu(url: String) {
        val request = DownloadRequest(
            url = url,
            userAgent = currentWv()?.settings?.userAgentString ?: BrowserWebView.ANDROID_UA,
            mimeType = null,
            contentDisposition = null,
        )
        pendingDownload = request
        scope.launch(Dispatchers.IO) {
            val size = HttpDownloader.contentLength(url, request.userAgent)
            withContext(Dispatchers.Main) {
                pendingDownload = request.copy(sizeBytes = size)
            }
        }
    }

    fun shareLink(url: String) {
        runCatching {
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    },
                    context.getString(R.string.context_share_title),
                ),
            )
        }
    }

    fun copyImage(url: String, name: String) {
        val ua = currentWv()?.settings?.userAgentString ?: BrowserWebView.ANDROID_UA
        scope.launch(Dispatchers.IO) {
            val dir = File(context.filesDir, "clipboard").apply { mkdirs() }
            val file = File(dir, name.replace(Regex("""[\\/:*?"<>|]"""), "_"))
            try {
                file.outputStream().use { out ->
                    HttpDownloader.download(url, ua, out, onProgress = { _, _ -> })
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
                val clip = ClipData.newUri(context.contentResolver, "image", uri)
                withContext(Dispatchers.Main) {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(clip)
                    Toast.makeText(context, R.string.context_image_copied, Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        R.string.context_image_copy_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    fun startEdit() {
        if (!editing) {
            addressText = currentTabUrl()
            editing = true
            keyboardController?.show()
        }
    }

    fun exitEdit() {
        if (!editing) return
        editing = false
        addressText = currentTabUrl()
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    val lastThumbnailAt = remember { mutableStateMapOf<Long, Long>() }

    fun scheduleThumbnailCapture(tabId: Long, force: Boolean = false) {
        if (!browserPrefs.tabPreviewEnabled || incognito) return
        val now = System.currentTimeMillis()
        if (!force && now - (lastThumbnailAt[tabId] ?: 0L) < 2000L) return
        lastThumbnailAt[tabId] = now
        scope.launch {
            delay(250)
            val wv = webViewStore.get(tabId) ?: return@launch
            PageCapture.capture(wv)?.let { app.tabThumbnailStore.put(tabId, it) }
        }
    }

    fun tabCallbacks(tabId: Long) = object : WebCallbacks {
        fun isReaderInternalNavigation(url: String): Boolean {
            val tab = viewModel.tabs.value.firstOrNull { it.id == tabId } ?: return false
            return tab.readerOffline && (url == tab.url || !UrlSecurity.isHttpUrl(url))
        }

        override fun onProgress(progress: Int) = viewModel.onProgress(tabId, progress)

        override fun onPageStarted(url: String) {
            textMenu = null
            linkMenu = null
            imageMenu = null
            if (isReaderInternalNavigation(url)) return
            viewModel.onPageStarted(tabId, url)
            app.pageIcons.update { it - tabId }
            webViewStore.markLoaded(tabId, url)
            updateNavState()
            val host = UrlSecurity.extractHost(url)
            if (host != null) {
                scope.launch {
                    val site = app.siteSettingsRepository.get(host)
                    webViewStore.get(tabId)?.applySiteSettings(url, browserPrefs, site)
                    val suppress = SitePermissionPolicy.resolve(
                        PermissionKind.NOTIFICATIONS,
                        site,
                        browserPrefs.permissionPromptEnabled,
                        browserPrefs.autoplayAllowed,
                    )
                    if (NotificationPolicy.shouldSuppress(suppress)) {
                        webViewStore.get(tabId)
                            ?.evaluateJavascript(NotificationPolicy.suppressionScript(), null)
                    }
                }
                if (host !in app.currentBrowserPrefs.trackedHosts) {
                    scope.launch {
                        app.browserPrefsStore.update {
                            it.copy(trackedHosts = (it.trackedHosts + host).take(200))
                        }
                    }
                }
            }
            if (tabId == viewModel.currentId.value) addressText = url
        }

        override fun onPageFinished(url: String) {
            if (isReaderInternalNavigation(url)) return
            viewModel.onPageFinished(tabId, url)
            updateNavState()
            if (tabId == viewModel.currentId.value) {
                scheduleThumbnailCapture(tabId, force = true)
            }
        }

        override fun onTitleChanged(title: String) {
            viewModel.onTitle(tabId, title)
            scheduleThumbnailCapture(tabId)
        }

        override fun onIconChanged(icon: Bitmap) {
            scope.launch(Dispatchers.IO) {
                val path = BookmarkIconStore.savePageIcon(context, icon, "tab_$tabId")
                if (path != null) {
                    app.pageIcons.update { it + (tabId to path) }
                }
            }
        }

        override fun onDownloadStart(
            url: String,
            userAgent: String,
            contentDisposition: String?,
            mimeType: String?,
        ) = viewModel.onDownload(url, userAgent, mimeType)

        override fun onPermissionRequest(request: PermissionRequest) =
            viewModel.onPermissionRequest(request)

        override fun onExternalScheme(url: String) = viewModel.onExternalScheme(url)

        override fun onMainFrameError(failingUrl: String, code: Int, description: String) {
            viewModel.onError(tabId, failingUrl)
            val id = tabId
            scope.launch {
                if (!incognito && !online) {
                    val cached = runCatching { app.readerCacheRepository.get(failingUrl) }.getOrNull()
                    android.util.Log.d(
                        "ReaderMode",
                        "onMainFrameError tab=$tabId incognito=$incognito online=$online cached=${cached != null}",
                    )
                    if (cached != null) {
                        val wv = webViewStore.get(id) ?: return@launch
                        readerController.loadOffline(
                            wv = wv,
                            url = failingUrl,
                            article = cached,
                            theme = readerTheme,
                            offlineBadge = context.getString(R.string.reader_offline_badge),
                        )
                        viewModel.setReaderMode(id, true)
                        viewModel.setReaderOffline(id, true)
                        viewModel.onOfflineCacheLoaded(id)
                    }
                }
            }
        }

        override fun onSslError(url: String, handler: SslErrorHandler) =
            viewModel.onSslError(url, handler)

        override fun onSafeBrowsingHit(url: String, threatType: Int, handler: SafeBrowsingResponse) =
            viewModel.onSafeBrowsingHit(url, threatType, handler)

        override fun onGeolocationPrompt(origin: String, callback: GeolocationPermissions.Callback) {
            pendingGeolocation = GeolocationPrompt(origin, callback)
        }

        override fun onPopup(url: String) {
            pendingPopup = url
        }

        override fun onHttpsBlocked(url: String) {
            pendingHttpsBlock = url
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowserEvent.Reload ->
                    webViewStore.get(viewModel.currentId.value ?: return@collect)?.reload()
                is BrowserEvent.Navigate -> Unit
                is BrowserEvent.Download -> {
                    val request = DownloadRequest(
                        url = event.url,
                        userAgent = event.userAgent,
                        mimeType = event.mimeType,
                        contentDisposition = null,
                    )
                    pendingDownload = request
                    scope.launch(Dispatchers.IO) {
                        val size = HttpDownloader.contentLength(event.url, event.userAgent)
                        withContext(Dispatchers.Main) {
                            pendingDownload = request.copy(sizeBytes = size)
                        }
                    }
                }
                is BrowserEvent.ExternalScheme -> pendingExternal = event.url
                is BrowserEvent.PermissionRequest -> pendingPermission = event.request
                is BrowserEvent.SslError -> pendingSsl = event.url to event.handler
                is BrowserEvent.SafeBrowsing ->
                    pendingSafeBrowsing = SafeBrowsingAlert(event.url, event.threatType, event.handler)
                is BrowserEvent.EnterReader -> {
                    val id = viewModel.currentId.value ?: return@collect
                    val url = viewModel.tabs.value.firstOrNull { it.id == id }?.url.orEmpty()
                    if (url.isBlank()) return@collect
                    val wv = webViewStore.get(id) ?: return@collect
                    scope.launch {
                        val article = readerController.enter(wv, readerTheme)
                        if (article == null) {
                            Toast.makeText(context, R.string.reader_no_content, Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        viewModel.setReaderMode(id, true)
                        if (!incognito) {
                            runCatching {
                                app.readerCacheRepository.put(
                                    ReaderCacheEntity(
                                        url = url,
                                        title = article.title,
                                        byline = article.byline,
                                        contentHtml = article.contentHtml,
                                    ),
                                )
                            }
                        }
                    }
                }
                is BrowserEvent.ExitReader -> {
                    val id = viewModel.currentId.value ?: return@collect
                    val wv = webViewStore.get(id) ?: return@collect
                    readerController.exit(wv) { restored ->
                        if (restored) {
                            viewModel.setReaderMode(id, false)
                            viewModel.setReaderOffline(id, false)
                        } else if (online) {
                            viewModel.setReaderMode(id, false)
                            viewModel.setReaderOffline(id, false)
                            wv.loadUrl(viewModel.tabs.value.firstOrNull { it.id == id }?.url.orEmpty())
                        } else {
                            Toast.makeText(
                                context,
                                R.string.reader_offline_exit_blocked,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(tabs) {
        val activeIds = app.tabManager.allTabIds()
        webViewStore.destroyRemoved(activeIds)
        webViewStore.trim(activeIds, keepId = currentId, limit = 8)
        if (!incognito) app.tabThumbnailStore.retain(activeIds)
    }

    LaunchedEffect(Unit) {
        if (!initialUrl.isNullOrBlank()) {
            viewModel.submitInput(initialUrl, SEARCH_TEMPLATE)
        }
    }

    LaunchedEffect(activeTab?.id) {
        addressText = activeTab?.url.orEmpty()
        textMenu = null
        linkMenu = null
        imageMenu = null
        currentId?.let { webViewStore.get(it) }
        val activeIds = app.tabManager.allTabIds()
        webViewStore.trim(activeIds, keepId = currentId, limit = 8)
        updateNavState()
        val tab = activeTab
        if (tab != null && tab.status == TabStatus.READY && tab.url.isNotBlank()) {
            scheduleThumbnailCapture(tab.id, force = true)
        }
    }

    // 设置或站点设置变化时，重新应用当前标签的 WebView 设置（HTTPS 模式/反追踪/自动播放等）。
    LaunchedEffect(browserPrefs, activeTab?.url, currentId) {
        val tab = activeTab ?: return@LaunchedEffect
        if (tab.url.isBlank()) return@LaunchedEffect
        val host = UrlSecurity.extractHost(tab.url)
        val site = host?.let { runCatching { app.siteSettingsRepository.get(it) }.getOrNull() }
        currentId?.let { id ->
            webViewStore.get(id)?.applySiteSettings(tab.url, browserPrefs, site)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val request = pendingPermission ?: return@rememberLauncherForActivityResult
        if (grants.values.all { it }) request.grant(request.resources) else request.deny()
        pendingPermission = null
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val prompt = pendingGeolocation ?: return@rememberLauncherForActivityResult
        prompt.callback.invoke(prompt.origin, grants.values.all { it }, false)
        pendingGeolocation = null
    }

    // 站点权限策略：BLOCK 直接拒绝；ALLOW 且无需系统权限时直接授予；
    // ALLOW 且需要系统权限时发起运行时授权；ASK 保留对话框。
    LaunchedEffect(pendingPermission) {
        val request = pendingPermission ?: return@LaunchedEffect
        val host = request.origin?.host
        val site = host?.let {
            runCatching { app.siteSettingsRepository.get(it) }.getOrNull()
        }
        val kinds = SitePermissionPolicy.kindsForResources(request.resources)
        val decisions = kinds.map {
            SitePermissionPolicy.resolve(
                it,
                site,
                browserPrefs.permissionPromptEnabled,
                browserPrefs.autoplayAllowed,
            )
        }
        if (decisions.contains(PermissionDecision.BLOCK)) {
            request.deny()
            pendingPermission = null
            return@LaunchedEffect
        }
        if (kinds.isNotEmpty() && decisions.all { it == PermissionDecision.ALLOW }) {
            val needed = PermissionMapping.androidPermissions(request.resources)
            if (needed.isEmpty()) {
                request.grant(request.resources)
                pendingPermission = null
            } else {
                permissionLauncher.launch(needed.toTypedArray())
            }
        }
    }

    LaunchedEffect(pendingGeolocation) {
        val prompt = pendingGeolocation ?: return@LaunchedEffect
        val host = UrlSecurity.extractHost(prompt.origin) ?: return@LaunchedEffect
        val site = runCatching { app.siteSettingsRepository.get(host) }.getOrNull()
        val decision = SitePermissionPolicy.resolve(
            PermissionKind.LOCATION,
            site,
            browserPrefs.permissionPromptEnabled,
            browserPrefs.autoplayAllowed,
        )
        when (decision) {
            PermissionDecision.ALLOW ->
                locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                )
            PermissionDecision.BLOCK -> {
                prompt.callback.invoke(prompt.origin, false, false)
                pendingGeolocation = null
            }
            PermissionDecision.ASK -> Unit
        }
    }

    LaunchedEffect(pendingPopup) {
        val url = pendingPopup ?: return@LaunchedEffect
        val host = UrlSecurity.extractHost(url)
        val site = host?.let { runCatching { app.siteSettingsRepository.get(it) }.getOrNull() }
        val decision = SitePermissionPolicy.resolve(
            PermissionKind.POPUPS,
            site,
            browserPrefs.permissionPromptEnabled,
            browserPrefs.autoplayAllowed,
        )
        if (decision == PermissionDecision.ALLOW) {
            app.tabManager.newTab(url)
            pendingPopup = null
        } else if (decision == PermissionDecision.BLOCK) {
            pendingPopup = null
        }
    }

    val currentWebView = currentId?.let { webViewStore.get(it) }
    val canGoBack = currentWebView?.canGoBack() == true
    BackHandler(enabled = canGoBack) { currentWebView?.goBack() }
    BackHandler(enabled = editing) { exitEdit() }
    BackHandler(enabled = textMenu != null) { textMenu = null }

    val showStartPage = activeTab == null ||
        (activeTab.status == TabStatus.EMPTY && activeTab.url.isBlank())

    Column(modifier = Modifier.fillMaxSize()) {
        val editPanel = @Composable {
            if (editing) {
                UrlEditPanel(
                    recentSearches = visibleRecentSearches,
                    pageTitle = activeTab?.title.orEmpty(),
                    pageUrl = activeTab?.url.orEmpty(),
                    onOpenSearch = { query ->
                        viewModel.submitInput(query, browserPrefs.searchTemplate)
                        exitEdit()
                    },
                    onCopyPage = {
                        val url = activeTab?.url.orEmpty()
                        if (url.isNotBlank()) {
                            clipboard.setText(AnnotatedString(url))
                            Toast.makeText(context, R.string.address_copied, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEditPage = {
                        pageInfoTitle = activeTab?.title.orEmpty()
                        pageInfoUrl = activeTab?.url.orEmpty()
                        editingPageInfo = true
                    },
                )
            }
        }
        val topBar = @Composable {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                if (!online) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer) {
                        Text(
                            text = stringResource(R.string.offline_banner),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                val currentPageUrl = activeTab?.url.orEmpty()
                if (HttpsPolicy.isInsecure(currentPageUrl)) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer) {
                        Text(
                            text = stringResource(R.string.https_insecure_banner),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (browserPrefs.toolbarPosition == ToolbarPosition.BOTTOM) {
                    editPanel()
                }
                AddressBar(
                    value = addressText,
                    editing = editing,
                    canReload = activeTab?.url?.isNotBlank() == true,
                    onValueChange = { addressText = it },
                    onSubmit = {
                        val input = addressText.trim()
                        if (input.isNotEmpty()) {
                            viewModel.submitInput(addressText, browserPrefs.searchTemplate)
                        }
                        exitEdit()
                    },
                    onReload = { currentId?.let { webViewStore.get(it) }?.reload() },
                    onClear = { addressText = "" },
                    onFocusChanged = { focused -> if (focused) startEdit() else exitEdit() },
                    progress = activeTab?.progress ?: 0,
                )
                if (browserPrefs.toolbarPosition == ToolbarPosition.TOP) {
                    editPanel()
                }
            }
        }
        if (!showStartPage && browserPrefs.toolbarPosition == ToolbarPosition.TOP) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                topBar()
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (!showStartPage && browserPrefs.toolbarPosition == ToolbarPosition.BOTTOM) {
                        Modifier.statusBarsPadding()
                    } else {
                        Modifier
                    },
                ),
        ) {
            val tab = activeTab
            if (tab != null) {
                key(tab.id) {
                    AndroidView(
                        factory = { ctx ->
                            webViewStore.getOrCreate(tab.id, ctx, tabCallbacks(tab.id))
                        },
                        update = { wv ->
                            wv.onUserInteract = { exitEdit() }
                            wv.onLongPressLink = { url, x, y ->
                                wv.evaluateJavascript(PageContextMenus.linkTextScript(x, y)) { raw ->
                                    linkMenu = LinkMenuState(url, PageContextMenus.parseText(raw))
                                }
                            }
                            wv.onLongPressImage = { url ->
                                imageMenu = ImageMenuState(url, ResourceSniffer.nameFor(url))
                            }
                            wv.onTextSelection = { x, y ->
                                wv.evaluateJavascript(PageContextMenus.selectionInfoOrSelectAtScript(x, y)) { raw ->
                                    textMenu = PageContextMenus.parseSelectionInfo(raw)
                                }
                            }
                            val url = tab.url
                            scope.launch {
                                val host = UrlSecurity.extractHost(url)
                                val site = if (host != null) app.siteSettingsRepository.get(host) else null
                                if (viewModel.tabs.value.firstOrNull { it.id == tab.id }?.url != url) {
                                    return@launch
                                }
                                val oldKey = wv.appliedSettingsKey
                                wv.applySiteSettings(url, browserPrefs, site)
                                if (url.isBlank()) return@launch
                                if (oldKey != null && oldKey != wv.appliedSettingsKey) {
                                    wv.reload()
                                } else if (cachePending != null && cachePending?.url == url) {
                                    val entity = cachePending!!
                                    cachePending = null
                                    wv.loadDataWithBaseURL(
                                        entity.url,
                                        entity.html,
                                        "text/html",
                                        "UTF-8",
                                        entity.url,
                                    )
                                    webViewStore.markLoaded(tab.id, url)
                                } else {
                                    webViewStore.ensureLoaded(tab.id, url)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            if (showStartPage) {
                StartPage(
                    onSearch = { viewModel.submitInput(it, browserPrefs.searchTemplate) },
                    onOpenUrl = { viewModel.submitInput(it, browserPrefs.searchTemplate) },
                    incognito = incognito,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (tab?.status == TabStatus.ERROR) {
                ErrorPage(
                    onRetry = viewModel::retry,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        if (!showStartPage && browserPrefs.toolbarPosition == ToolbarPosition.BOTTOM) {
            topBar()
        }

        pendingExternal?.let { url ->
            AlertDialog(
                onDismissRequest = { pendingExternal = null },
                title = { Text(stringResource(R.string.external_scheme_title)) },
                text = { Text(stringResource(R.string.external_scheme_message, url)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingExternal = null
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        },
                    ) {
                        Text(stringResource(R.string.dialog_allow))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingExternal = null }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
            )
        }

        pendingSsl?.let { (url, handler) ->
            AlertDialog(
                onDismissRequest = { handler.cancel(); pendingSsl = null },
                title = { Text(stringResource(R.string.ssl_warning_title)) },
                text = { Text(stringResource(R.string.ssl_warning_message, url)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            handler.proceed()
                            pendingSsl = null
                        },
                    ) {
                        Text(stringResource(R.string.dialog_continue))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { handler.cancel(); pendingSsl = null }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
            )
        }

        pendingSafeBrowsing?.let { alert ->
            AlertDialog(
                onDismissRequest = {
                    alert.handler.backToSafety(true)
                    pendingSafeBrowsing = null
                },
                title = { Text(stringResource(R.string.safe_browsing_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.safe_browsing_message,
                            stringResource(SafeBrowsingThreats.labelRes(alert.threatType)),
                            alert.url,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            alert.handler.proceed(true)
                            pendingSafeBrowsing = null
                        },
                    ) {
                        Text(stringResource(R.string.safe_browsing_proceed))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            alert.handler.backToSafety(true)
                            pendingSafeBrowsing = null
                        },
                    ) {
                        Text(stringResource(R.string.safe_browsing_back))
                    }
                },
            )
        }

        pendingDownload?.let { request ->
            val risk = DownloadRiskPolicy.riskOf(request.url, request.mimeType)
            val risky = risk == DownloadRisk.HIGH && browserPrefs.downloadRiskWarnings
            AlertDialog(
                onDismissRequest = { pendingDownload = null },
                title = {
                    Text(
                        stringResource(
                            if (risky) R.string.download_risk_title else R.string.download_confirm_title,
                        ),
                    )
                },
                text = {
                    Column {
                        Text(
                            stringResource(
                                R.string.download_confirm_message,
                                DownloadNames.from(request.url, request.contentDisposition, request.mimeType),
                                request.url,
                            ),
                        )
                        if (request.sizeBytes != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    R.string.file_size,
                                    DownloadFormat.formatBytes(request.sizeBytes),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (risky) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.download_risk_warning),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingDownload = null
                            startDownload(request)
                        },
                    ) {
                        Text(stringResource(R.string.download_confirm_yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDownload = null }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
            )
        }

        pendingPermission?.let { request ->
            val needed = PermissionMapping.androidPermissions(request.resources)
            AlertDialog(
                onDismissRequest = {
                    request.deny()
                    pendingPermission = null
                },
                title = { Text(stringResource(R.string.permission_dialog_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.permission_dialog_message,
                            request.origin.toString(),
                            PermissionMapping.describe(request.resources),
                        ),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (needed.isEmpty()) {
                                request.grant(request.resources)
                                pendingPermission = null
                            } else {
                                permissionLauncher.launch(needed.toTypedArray())
                            }
                        },
                    ) {
                        Text(stringResource(R.string.dialog_allow))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { request.deny(); pendingPermission = null }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
            )
        }

        pendingGeolocation?.let { prompt ->
            AlertDialog(
                onDismissRequest = {
                    prompt.callback.invoke(prompt.origin, false, false)
                    pendingGeolocation = null
                },
                title = { Text(stringResource(R.string.geolocation_dialog_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.permission_dialog_message,
                            prompt.origin,
                            stringResource(R.string.permission_location),
                        ),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingGeolocation = null
                            locationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            )
                        },
                    ) {
                        Text(stringResource(R.string.dialog_allow))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            prompt.callback.invoke(prompt.origin, false, false)
                            pendingGeolocation = null
                        },
                    ) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
            )
        }

        pendingPopup?.let { url ->
            AlertDialog(
                onDismissRequest = { pendingPopup = null },
                title = { Text(stringResource(R.string.popup_dialog_title)) },
                text = { Text(stringResource(R.string.popup_dialog_message, url)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            app.tabManager.newTab(url)
                            pendingPopup = null
                        },
                    ) {
                        Text(stringResource(R.string.dialog_allow))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingPopup = null }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
            )
        }

        pendingHttpsBlock?.let { url ->
            AlertDialog(
                onDismissRequest = { pendingHttpsBlock = null },
                title = { Text(stringResource(R.string.https_blocked_title)) },
                text = { Text(stringResource(R.string.https_blocked_message, url)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            currentId?.let { id ->
                                webViewStore.get(id)?.bypassHttpsUpgrade(HttpsPolicy.toHttp(url))
                            }
                            pendingHttpsBlock = null
                        },
                    ) {
                        Text(stringResource(R.string.https_blocked_proceed))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingHttpsBlock = null }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
            )
        }

        textMenu?.let { info ->
            TextSelectionPopup(
                info = info,
                density = density,
                onCopy = ::copySelectionText,
                onSelectAll = ::selectAllText,
                onSearch = ::searchSelectionInNewTab,
                onDismiss = { textMenu = null },
            )
        }

        linkMenu?.let { menu ->
            LinkContextDialog(
                url = menu.url,
                linkText = menu.text,
                onOpenNewTab = {
                    openInNewTab(menu.url)
                    linkMenu = null
                },
                onOpenIncognito = {
                    openInIncognito(menu.url)
                    linkMenu = null
                },
                onCopyAddress = {
                    copyText(menu.url, R.string.context_copied)
                    linkMenu = null
                },
                onCopyText = {
                    copyText(menu.text, R.string.context_copied)
                    linkMenu = null
                },
                onDownload = {
                    downloadFromMenu(menu.url)
                    linkMenu = null
                },
                onShare = {
                    shareLink(menu.url)
                    linkMenu = null
                },
                onDismiss = { linkMenu = null },
            )
        }

        imageMenu?.let { menu ->
            ImageContextDialog(
                url = menu.url,
                name = menu.name,
                onOpenNewTab = {
                    openInNewTab(menu.url)
                    imageMenu = null
                },
                onCopy = {
                    copyImage(menu.url, menu.name)
                    imageMenu = null
                },
                onDownload = {
                    downloadFromMenu(menu.url)
                    imageMenu = null
                },
                onDismiss = { imageMenu = null },
            )
        }

        if (editingPageInfo) {
            val tab = activeTab
            AlertDialog(
                onDismissRequest = { editingPageInfo = false },
                title = { Text(stringResource(R.string.address_edit_page)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = pageInfoTitle,
                            onValueChange = { pageInfoTitle = it },
                            label = { Text(stringResource(R.string.address_page_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pageInfoUrl,
                            onValueChange = { pageInfoUrl = it },
                            label = { Text(stringResource(R.string.address_page_url)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val id = currentId
                            if (id != null) {
                                val title = pageInfoTitle.trim()
                                if (title.isNotBlank() && title != tab?.title) {
                                    viewModel.onTitle(id, title)
                                }
                                val url = pageInfoUrl.trim()
                                if (url.isNotBlank() && url != tab?.url) {
                                    viewModel.submitInput(url, browserPrefs.searchTemplate)
                                }
                            }
                            editingPageInfo = false
                            exitEdit()
                        },
                    ) {
                        Text(stringResource(R.string.dialog_allow))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingPageInfo = false }) {
                        Text(stringResource(R.string.dialog_cancel))
                    }
                },
            )
        }
    }
}
