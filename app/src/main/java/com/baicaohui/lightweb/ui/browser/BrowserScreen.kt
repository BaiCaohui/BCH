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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.browser.AdLevel
import com.baicaohui.lightweb.browser.BrowserWebView
import com.baicaohui.lightweb.browser.DownloadHandler
import com.baicaohui.lightweb.browser.PermissionMapping
import com.baicaohui.lightweb.browser.TabStatus
import com.baicaohui.lightweb.browser.WebCallbacks
import com.baicaohui.lightweb.ui.components.ErrorPage

private const val SEARCH_TEMPLATE = "https://www.bing.com/search?q=%s"

@Composable
fun BrowserScreen(initialUrl: String? = null) {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val viewModel: BrowserViewModel = viewModel(
        factory = viewModelFactory {
            initializer { BrowserViewModel(app.tabManager) }
        },
    )
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val currentId by viewModel.currentId.collectAsStateWithLifecycle()
    val activeTab = tabs.firstOrNull { it.id == currentId }

    var addressText by remember { mutableStateOf("") }
    var pendingExternal by remember { mutableStateOf<String?>(null) }
    var pendingSsl by remember { mutableStateOf<Pair<String, SslErrorHandler>?>(null) }
    var pendingPermission by remember { mutableStateOf<PermissionRequest?>(null) }
    var webView by remember { mutableStateOf<BrowserWebView?>(null) }

    val downloadHandler = remember { DownloadHandler(context) }

    val callbacks = remember(viewModel) {
        object : WebCallbacks {
            override fun onProgress(progress: Int) = viewModel.onProgress(progress)

            override fun onPageStarted(url: String) {
                viewModel.onPageStarted(url)
                addressText = url
            }

            override fun onPageFinished(url: String) = viewModel.onPageFinished(url)

            override fun onTitleChanged(title: String) = viewModel.onTitle(title)

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
                viewModel.onError(failingUrl)

            override fun onSslError(url: String, handler: SslErrorHandler) =
                viewModel.onSslError(url, handler)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowserEvent.Reload -> webView?.reload()
                is BrowserEvent.Navigate -> webView?.loadUrl(event.url)
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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val request = pendingPermission ?: return@rememberLauncherForActivityResult
        if (grants.values.all { it }) request.grant(request.resources) else request.deny()
        pendingPermission = null
    }

    val canGoBack = webView?.canGoBack() == true
    BackHandler(enabled = canGoBack) { webView?.goBack() }

    Column(modifier = Modifier.fillMaxSize()) {
        BrowserToolbar(
            canGoBack = canGoBack,
            canGoForward = webView?.canGoForward() == true,
            tabCount = tabs.size,
            onBack = { webView?.goBack() },
            onForward = { webView?.goForward() },
            onReload = { webView?.reload() },
            onTabs = { /* M2 打开标签页总览 */ },
        )
        AddressBar(
            value = addressText,
            onValueChange = { addressText = it },
            onSubmit = {
                viewModel.submitInput(addressText, SEARCH_TEMPLATE)
            },
            progress = activeTab?.progress ?: 0,
        )
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    BrowserWebView(
                        context = ctx,
                        callbacks = callbacks,
                        adBlocker = app.adBlocker,
                        adLevel = { AdLevel.BASIC },
                    ).also { wv ->
                        webView = wv
                        if (!initialUrl.isNullOrBlank()) {
                            viewModel.submitInput(initialUrl, SEARCH_TEMPLATE)
                        } else {
                            val target = viewModel.tabs.value
                                .firstOrNull { it.id == viewModel.currentId.value }
                                ?.url
                            if (!target.isNullOrBlank()) wv.loadUrl(target)
                        }
                    }
                },
                update = {},
                modifier = Modifier.fillMaxSize(),
            )
            val showStartPage = activeTab == null ||
                (activeTab.status == TabStatus.EMPTY && activeTab.url.isBlank())
            if (showStartPage) {
                StartPage(
                    onSearch = { viewModel.submitInput(it, SEARCH_TEMPLATE) },
                    modifier = Modifier.matchParentSize(),
                )
            }
            if (activeTab?.status == TabStatus.ERROR) {
                ErrorPage(
                    onRetry = viewModel::retry,
                    modifier = Modifier.matchParentSize(),
                )
            }
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
