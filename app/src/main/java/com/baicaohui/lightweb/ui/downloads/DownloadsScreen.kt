package com.baicaohui.lightweb.ui.downloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.browser.DownloadStatus
import com.baicaohui.lightweb.browser.UrlSecurity
import com.baicaohui.lightweb.data.db.DownloadEntity
import com.baicaohui.lightweb.data.prefs.BrowserPrefs
import com.baicaohui.lightweb.ui.bookmarks.ConfirmDialog
import com.baicaohui.lightweb.ui.components.PlaceholderScreen
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

private const val FILE_PROVIDER_AUTHORITY = "com.baicaohui.lightweb.fileprovider"

@Composable
fun DownloadsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val scope = rememberCoroutineScope()
    val downloads by app.downloadRepository.downloads.collectAsStateWithLifecycle(
        initialValue = emptyList(),
    )
    var showClearConfirm by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val browserPrefs by app.browserPrefsStore.prefs.collectAsStateWithLifecycle(
        initialValue = app.currentBrowserPrefs,
    )

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.downloads_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { showSettings = true }) {
                Text(stringResource(R.string.downloads_settings))
            }
            TextButton(
                onClick = { showClearConfirm = true },
                enabled = downloads.isNotEmpty(),
            ) {
                Text(stringResource(R.string.downloads_clear))
            }
        }
        if (downloads.isEmpty()) {
            PlaceholderScreen(text = stringResource(R.string.downloads_empty))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(downloads, key = { it.id }) { entity ->
                    DownloadItem(
                        entity = entity,
                        onDelete = {
                            scope.launch { app.downloadRepository.delete(entity) }
                        },
                        onRetry = {
                            scope.launch {
                                app.downloadRepository.delete(entity)
                                app.appDownloadManager.enqueue(
                                    entity.url,
                                    entity.userAgent,
                                    entity.mimeType,
                                    null,
                                )
                            }
                        },
                        onOpen = { openDownload(context, entity) },
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.downloads_confirm_clear_title),
            text = stringResource(R.string.downloads_confirm_clear_text),
            onConfirm = {
                showClearConfirm = false
                scope.launch { app.downloadRepository.clear() }
            },
            onDismiss = { showClearConfirm = false },
        )
    }

    if (showSettings) {
        DownloadSettingsDialog(
            prefs = browserPrefs,
            onUpdate = { transform ->
                scope.launch { app.browserPrefsStore.update(transform) }
            },
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun DownloadItem(
    entity: DownloadEntity,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
) {
    val status = runCatching { DownloadStatus.valueOf(entity.status) }
        .getOrDefault(DownloadStatus.QUEUED)
    val statusLabel = stringResource(
        when (status) {
            DownloadStatus.QUEUED -> R.string.downloads_status_queued
            DownloadStatus.RUNNING -> R.string.downloads_status_running
            DownloadStatus.COMPLETED -> R.string.downloads_status_completed
            DownloadStatus.FAILED -> R.string.downloads_status_failed
        },
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = statusIcon(status),
                contentDescription = null,
                tint = statusColor(status),
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entity.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${UrlSecurity.extractHost(entity.url) ?: entity.url} · $statusLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (status == DownloadStatus.COMPLETED) {
                IconButton(onClick = onOpen) {
                    Icon(
                        Icons.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.downloads_open),
                    )
                }
            }
            if (status == DownloadStatus.FAILED) {
                IconButton(onClick = onRetry) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.downloads_retry),
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.downloads_delete),
                )
            }
        }
        if (status == DownloadStatus.QUEUED || status == DownloadStatus.RUNNING) {
            Spacer(Modifier.height(8.dp))
            if (entity.totalBytes > 0) {
                LinearProgressIndicator(
                    progress = {
                        (entity.downloadedBytes.toFloat() / entity.totalBytes).coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${formatBytes(entity.downloadedBytes)} / ${formatBytes(entity.totalBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp))
            }
        }
    }
}

private fun statusIcon(status: DownloadStatus): ImageVector = when (status) {
    DownloadStatus.QUEUED, DownloadStatus.RUNNING -> Icons.Filled.Download
    DownloadStatus.COMPLETED -> Icons.Filled.Check
    DownloadStatus.FAILED -> Icons.Filled.Close
}

@Composable
private fun statusColor(status: DownloadStatus): Color = when (status) {
    DownloadStatus.QUEUED, DownloadStatus.RUNNING -> MaterialTheme.colorScheme.primary
    DownloadStatus.COMPLETED -> Color(0xFF2E7D32)
    DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_048_576 -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
    bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}

private fun openDownload(context: Context, entity: DownloadEntity) {
    val destination = entity.destination
    if (destination.isNullOrBlank()) {
        Toast.makeText(context, R.string.downloads_open_failed, Toast.LENGTH_SHORT).show()
        return
    }
    if (destination.startsWith("content://")) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(Uri.parse(destination), entity.mimeType ?: "*/*")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
        }.onFailure {
            Toast.makeText(context, R.string.downloads_open_failed, Toast.LENGTH_SHORT).show()
        }
        return
    }
    val file = File(destination)
    if (!file.exists()) {
        Toast.makeText(context, R.string.downloads_open_failed, Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, entity.mimeType ?: "*/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }.onFailure {
        Toast.makeText(context, R.string.downloads_open_failed, Toast.LENGTH_SHORT).show()
    }
}
