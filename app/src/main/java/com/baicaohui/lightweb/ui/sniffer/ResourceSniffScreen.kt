package com.baicaohui.lightweb.ui.sniffer

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.browser.BrowserWebView
import com.baicaohui.lightweb.browser.DownloadFormat
import com.baicaohui.lightweb.browser.DownloadHandler
import com.baicaohui.lightweb.browser.HttpDownloader
import com.baicaohui.lightweb.browser.ResourceKind
import com.baicaohui.lightweb.browser.ResourceSniffer
import com.baicaohui.lightweb.browser.SniffedResource
import com.baicaohui.lightweb.data.prefs.DownloadMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ResourceSniffScreen(
    tabId: Long?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val wv = remember(tabId) { tabId?.let { app.webViewStore.get(it) } }
    val controller = wv?.resourceSniffing
    val resources = controller?.resources
        ?.collectAsStateWithLifecycle(initialValue = emptyList())
        ?.value
        ?: emptyList()
    val downloadHandler = remember { DownloadHandler(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(wv) {
        val view = wv ?: return@LaunchedEffect
        val sniff = view.resourceSniffing
        sniff.start()
        sniff.clear()
        view.scanPageResources()
    }
    val sizeUserAgent = wv?.settings?.userAgentString ?: BrowserWebView.ANDROID_UA
    resources.forEach { resource ->
        LaunchedEffect(resource.url) {
            if (resource.sizeBytes == null) {
                val size = withContext(Dispatchers.IO) {
                    HttpDownloader.contentLength(resource.url, sizeUserAgent)
                }
                controller?.updateSize(resource.url, size)
            }
        }
    }
    DisposableEffect(wv) {
        onDispose { wv?.resourceSniffing?.stop() }
    }

    fun download(resource: SniffedResource) {
        val view = wv ?: return
        val ua = view.settings.userAgentString
        val mime = ResourceSniffer.mimeFor(resource.kind, resource.url)
        if (app.currentBrowserPrefs.downloadMode == DownloadMode.SYSTEM) {
            downloadHandler.start(resource.url, ua, mime)
        } else {
            scope.launch {
                app.appDownloadManager.enqueue(resource.url, ua, mime, null)
            }
        }
        Toast.makeText(context, R.string.download_started, Toast.LENGTH_SHORT).show()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            Text(
                text = stringResource(R.string.sniffer_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                controller?.clear()
                wv?.scanPageResources()
            }) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.sniffer_refresh),
                )
            }
        }
        if (resources.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.sniffer_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(resources, key = { it.url }) { resource ->
                    ListItem(
                        leadingContent = { ResourcePreview(resource) },
                        headlineContent = {
                            Text(resource.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        supportingContent = {
                            Column {
                                Text(resource.url, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                if (resource.sizeBytes != null) {
                                    Text(
                                        text = stringResource(
                                            R.string.file_size,
                                            DownloadFormat.formatBytes(resource.sizeBytes),
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { download(resource) }) {
                                Icon(
                                    Icons.Filled.FileDownload,
                                    contentDescription = stringResource(R.string.action_download),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourcePreview(resource: SniffedResource) {
    if (resource.kind == ResourceKind.IMAGE) {
        AsyncImage(
            model = resource.url,
            contentDescription = resource.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
        )
    } else {
        val icon: ImageVector = when (resource.kind) {
            ResourceKind.VIDEO -> Icons.Filled.Movie
            ResourceKind.AUDIO -> Icons.Filled.AudioFile
            ResourceKind.IMAGE -> Icons.Filled.Image
        }
        Icon(
            imageVector = icon,
            contentDescription = resource.name,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp).padding(12.dp),
        )
    }
}
