package com.baicaohui.lightweb.ui.browser

import android.content.Intent
import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.browser.DownloadHandler
import com.baicaohui.lightweb.browser.PermissionMapping
import com.baicaohui.lightweb.browser.Tab
import com.baicaohui.lightweb.browser.TabStatus
import com.baicaohui.lightweb.browser.UrlSecurity
import com.baicaohui.lightweb.browser.WebCallbacks
import com.baicaohui.lightweb.data.prefs.ToolbarPosition
import com.baicaohui.lightweb.ui.components.ErrorPage
import kotlinx.coroutines.launch

private const val SEARCH_TEMPLATE = "https://www.bing.com/search?q=%s"

@Composable
fun BrowserScreen(
    initialUrl: String? = null,
    onOpenTabs: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val viewModel: BrowserViewModel = viewModel(
        factory = viewModelFactory {
            initializer { BrowserViewModel(app.tabManager, app.historyRepository) }
        },
    )
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val currentId by viewModel.currentId.collectAsStateWithLifecycle()
    val activeTab = tabs.firstOrNull { it.id == currentId }

    var addressText by remember { mutableStateOf("") }
    var pendingExternal by remember { mutableStateOf<String?>(null) }
    var pendingSsl by remember { mutableStateOf<Pair<String, SslErrorHandler>?>(null) }
    var pendingPermission by remember { mutableStateOf<PermissionRequest?>(null) }

    val downloadHandler = remember { DownloadHandler(context) }
    val webViewStore = app.webViewStore
    val browserPrefs = app.currentBrowserPrefs
    val scope = rememberCoroutineScope()
    val online by app.networkMonitor.online.collectAsStateWithLifecycle(initialValue = true)

    fun tabCallbacks(tabId: Long) = object : WebCallbacks {
        override fun onProgress(progress: Int) = viewModel.onProgress(tabId, progress)

        override fun onPageStarted(url: String) {
            viewModel.onPageStarted(tabId, url)
            webViewStore.markLoaded(tabId, url)
            val host = UrlSecurity.extractHost(url)
            if (host != null) {
                scope.launch {
                    val site = app.siteSettingsRepository.get(host)
                    webViewStore.get(tabId)?.applySiteSettings(url, app.currentBrowserPrefs, site)
                }
            }
            if (tabId == viewModel.currentId.value) addressText = url
        }

        override fun onPageFinished(url: String) = viewModel.onPageFinished(tabId, url)

        override fun onTitleChanged(title: String) = viewModel.onTitle(tabId, title)

        override fun onDownloadStart(
            url: String,
            userAgent: String,
            contentDisposition: String?,
            mimeType: String?,
        ) = viewModel.onDownload(url, userAgent, mimeType)

        override fun onPermissionRequest(request: PermissionRequest) =
            viewModel.onPermissionRequest(request)

        override fun onExternalScheme(url: String) = viewModel.onExternalScheme(url)

        override fun onMainFrameError(failingUrl: String, code: Int, description: String) =
            viewModel.onError(tabId, failingUrl)

        override fun onSslError(url: String, handler: SslErrorHandler) =
            viewModel.onSslError(url, handler)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowserEvent.Reload ->
                    webViewStore.get(viewModel.currentId.value ?: return@collect)?.reload()
                is BrowserEvent.Navigate -> Unit
                is BrowserEvent.Download -> {
                    downloadHandler.start(event.url, event.userAgent, event.mimeType)
                    Toast.makeText(context, R.string.download_started, Toast.LENGTH_SHORT).show()
                }
                is BrowserEvent.ExternalScheme -> pendingExternal = event.url
                is BrowserEvent.PermissionRequest -> pendingPermission = event.request
                is BrowserEvent.SslError -> pendingSsl = event.url to event.handler
            }
        }
    }

    LaunchedEffect(tabs) {
        webViewStore.destroyRemoved(tabs.map { it.id }.toSet())
    }

    LaunchedEffect(Unit) {
        if (!initialUrl.isNullOrBlank()) {
            viewModel.submitInput(initialUrl, SEARCH_TEMPLATE)
        }
    }

    LaunchedEffect(activeTab?.id) {
        addressText = activeTab?.url.orEmpty()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val request = pendingPermission ?: return@rememberLauncherForActivityResult
        if (grants.values.all { it }) request.grant(request.resources) else request.deny()
        pendingPermission = null
    }

    val currentWebView = currentId?.let { webViewStore.get(it) }
    val canGoBack = currentWebView?.canGoBack() == true
    BackHandler(enabled = canGoBack) { currentWebView?.goBack() }

    Column(modifier = Modifier.fillMaxSize()) {
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
        val topBar = @Composable {
            Column {
                BrowserToolbar(
                    canGoBack = canGoBack,
                    canGoForward = currentWebView?.canGoForward() == true,
                    tabCount = tabs.size,
                    showBack = browserPrefs.showBack,
                    showForward = browserPrefs.showForward,
                    showReload = browserPrefs.showReload,
                    onBack = { currentWebView?.goBack() },
                    onForward = { currentWebView?.goForward() },
                    onReload = { currentWebView?.reload() },
                    onTabs = onOpenTabs,
                )
                AddressBar(
                    value = addressText,
                    onValueChange = { addressText = it },
                    onSubmit = {
                        viewModel.submitInput(addressText, app.currentBrowserPrefs.searchTemplate)
                    },
                    progress = activeTab?.progress ?: 0,
                )
            }
        }
        if (browserPrefs.toolbarPosition == ToolbarPosition.TOP) {
            topBar()
        }
        Box(modifier = Modifier.fillMaxSize()) {
            val tab = activeTab
            if (tab != null) {
                key(tab.id) {
                    AndroidView(
                        factory = { ctx ->
                            webViewStore.getOrCreate(tab.id, ctx, tabCallbacks(tab.id))
                        },
                        update = { wv ->
                            webViewStore.ensureLoaded(tab.id, tab.url)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            val showStartPage = tab == null ||
                (tab.status == TabStatus.EMPTY && tab.url.isBlank())
            if (showStartPage) {
                StartPage(
                    onSearch = { viewModel.submitInput(it, app.currentBrowserPrefs.searchTemplate) },
                    onOpenUrl = { viewModel.submitInput(it, app.currentBrowserPrefs.searchTemplate) },
                    modifier = Modifier.matchParentSize(),
                )
            }
            if (tab?.status == TabStatus.ERROR) {
                ErrorPage(
                    onRetry = viewModel::retry,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        if (browserPrefs.toolbarPosition == ToolbarPosition.BOTTOM) {
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
    }
}
