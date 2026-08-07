package com.baicaohui.lightweb.ui.downloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import com.baicaohui.lightweb.browser.DownloadFormat
import com.baicaohui.lightweb.browser.DownloadStatus
import com.baicaohui.lightweb.browser.LiveDownloadProgress
import com.baicaohui.lightweb.browser.UrlSecurity
import com.baicaohui.lightweb.data.db.DownloadEntity
import com.baicaohui.lightweb.data.prefs.BrowserPrefs
import com.baicaohui.lightweb.ui.bookmarks.ConfirmDialog
import com.baicaohui.lightweb.ui.components.PlaceholderScreen
import kotlinx.coroutines.launch
import java.io.File

private const val FILE_PROVIDER_AUTHORITY = "com.baicaohui.lightweb.fileprovider"

@Composable
fun DownloadsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val scope = rememberCoroutineScope()
    val downloads by app.downloadRepository.downloads.collectAsStateWithLifecycle(
        initialValue = emptyList(),
    )
    val liveProgress by app.appDownloadManager.liveProgress.collectAsStateWithLifecycle(
        initialValue = emptyMap(),
    )
    var showClearConfirm by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var actionEntity by remember { mutableStateOf<DownloadEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<DownloadEntity?>(null) }
    var deleteSourceFile by remember { mutableStateOf(false) }
    val browserPrefs by app.browserPrefsStore.prefs.collectAsStateWithLifecycle(
        initialValue = app.currentBrowserPrefs,
    )
    val hasActive = downloads.any {
        it.status == DownloadStatus.RUNNING.name || it.status == DownloadStatus.QUEUED.name
    }

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
            TextButton(
                onClick = { app.appDownloadManager.pauseAll() },
                enabled = hasActive,
            ) {
                Text(stringResource(R.string.downloads_pause_all))
            }
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
                        live = liveProgress[entity.id],
                        onDelete = {
                            app.appDownloadManager.deleteTaskFiles(entity)
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
                        onPauseResume = {
                            if (entity.status == DownloadStatus.PAUSED.name) {
                                app.appDownloadManager.resume(entity.id)
                            } else {
                                app.appDownloadManager.pause(entity.id)
                            }
                        },
                        onOpen = { openDownload(context, entity) },
                        onLongPress = { actionEntity = entity },
                    )
                }
            }
        }
    }

    actionEntity?.let { entity ->
        DownloadActionsDialog(
            entity = entity,
            onOpen = {
                actionEntity = null
                openDownload(context, entity)
            },
            onOpenWith = {
                actionEntity = null
                openDownloadWithChooser(context, entity)
            },
            onShare = {
                actionEntity = null
                shareDownload(context, entity)
            },
            onDelete = {
                actionEntity = null
                deleteSourceFile = false
                pendingDelete = entity
            },
            onDismiss = { actionEntity = null },
        )
    }

    pendingDelete?.let { entity ->
        DeleteDownloadDialog(
            entity = entity,
            deleteSource = deleteSourceFile,
            onDeleteSourceChange = { deleteSourceFile = it },
            onConfirm = {
                val removeSource = deleteSourceFile
                app.appDownloadManager.deleteTaskFiles(entity)
                scope.launch {
                    if (removeSource) {
                        deleteDownloadSource(context, entity)
                    }
                    app.downloadRepository.delete(entity)
                }
                pendingDelete = null
                deleteSourceFile = false
            },
            onDismiss = {
                pendingDelete = null
                deleteSourceFile = false
            },
        )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadItem(
    entity: DownloadEntity,
    live: LiveDownloadProgress?,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onPauseResume: () -> Unit,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
) {
    val status = runCatching { DownloadStatus.valueOf(entity.status) }
        .getOrDefault(DownloadStatus.QUEUED)
    val statusLabel = stringResource(
        when (status) {
            DownloadStatus.QUEUED -> R.string.downloads_status_queued
            DownloadStatus.RUNNING -> R.string.downloads_status_running
            DownloadStatus.PAUSED -> R.string.downloads_status_paused
            DownloadStatus.COMPLETED -> R.string.downloads_status_completed
            DownloadStatus.FAILED -> R.string.downloads_status_failed
        },
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (status == DownloadStatus.COMPLETED) onOpen()
                },
                onLongClick = {
                    if (status == DownloadStatus.PAUSED ||
                        status == DownloadStatus.COMPLETED
                    ) {
                        onLongPress()
                    }
                },
            )
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
                    text = "${UrlSecurity.extractHost(entity.url) ?: entity.url} 路 $statusLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (status == DownloadStatus.QUEUED ||
                status == DownloadStatus.RUNNING ||
                status == DownloadStatus.PAUSED
            ) {
                IconButton(onClick = onPauseResume) {
                    Icon(
                        imageVector = if (status == DownloadStatus.PAUSED) {
                            Icons.Filled.PlayArrow
                        } else {
                            Icons.Filled.Pause
                        },
                        contentDescription = stringResource(
                            if (status == DownloadStatus.PAUSED) {
                                R.string.downloads_resume
                            } else {
                                R.string.downloads_pause
                            },
                        ),
                    )
                }
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
        if (status == DownloadStatus.QUEUED ||
            status == DownloadStatus.RUNNING ||
            status == DownloadStatus.PAUSED
        ) {
            Spacer(Modifier.height(8.dp))
            val done = live?.downloadedBytes ?: entity.downloadedBytes
            val total = live?.totalBytes ?: entity.totalBytes
            if (total > 0) {
                LinearProgressIndicator(
                    progress = {
                        (done.toFloat() / total).coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
                Spacer(Modifier.height(4.dp))
                val speedText = live
                    ?.takeIf { it.speedBps > 0 }
                    ?.let { DownloadFormat.formatSpeed(it.speedBps) }
                Text(
                    text = buildString {
                        append(DownloadFormat.formatBytes(done))
                        append(" / ")
                        append(DownloadFormat.formatBytes(total))
                        if (speedText != null) {
                            append(" 路 ")
                            append(speedText)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp))
            }
        }
    }
}

@Composable
private fun DownloadActionsDialog(
    entity: DownloadEntity,
    onOpen: () -> Unit,
    onOpenWith: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val status = runCatching { DownloadStatus.valueOf(entity.status) }
        .getOrDefault(DownloadStatus.QUEUED)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = entity.fileName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column {
                if (status == DownloadStatus.COMPLETED) {
                    MenuActionRow(Icons.Filled.OpenInNew, R.string.downloads_open, onOpen)
                    MenuActionRow(Icons.Filled.OpenWith, R.string.downloads_open_with, onOpenWith)
                    MenuActionRow(Icons.Filled.Share, R.string.downloads_share, onShare)
                }
                MenuActionRow(
                    Icons.Filled.Delete,
                    if (status == DownloadStatus.PAUSED) {
                        R.string.downloads_delete_task
                    } else {
                        R.string.downloads_delete
                    },
                    onDelete,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun DeleteDownloadDialog(
    entity: DownloadEntity,
    deleteSource: Boolean,
    onDeleteSourceChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val completed = entity.status == DownloadStatus.COMPLETED.name
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.downloads_delete_confirm_title)) },
        text = {
            Column {
                Text(
                    stringResource(
                        if (completed) {
                            R.string.downloads_delete_confirm_text
                        } else {
                            R.string.downloads_delete_paused_confirm_text
                        },
                    ),
                )
                if (completed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeleteSourceChange(!deleteSource) }
                            .padding(top = 8.dp),
                    ) {
                        Checkbox(
                            checked = deleteSource,
                            onCheckedChange = onDeleteSourceChange,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.downloads_delete_source))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.downloads_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun MenuActionRow(
    icon: ImageVector,
    labelRes: Int,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun statusIcon(status: DownloadStatus): ImageVector = when (status) {
    DownloadStatus.QUEUED, DownloadStatus.RUNNING -> Icons.Filled.Download
    DownloadStatus.PAUSED -> Icons.Filled.Pause
    DownloadStatus.COMPLETED -> Icons.Filled.Check
    DownloadStatus.FAILED -> Icons.Filled.Close
}

@Composable
private fun statusColor(status: DownloadStatus): Color = when (status) {
    DownloadStatus.QUEUED, DownloadStatus.RUNNING -> MaterialTheme.colorScheme.primary
    DownloadStatus.PAUSED -> MaterialTheme.colorScheme.tertiary
    DownloadStatus.COMPLETED -> Color(0xFF2E7D32)
    DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
}

private fun openDownload(context: Context, entity: DownloadEntity) {
    val intent = openDownloadIntent(context, entity)
    if (intent == null) {
        Toast.makeText(context, R.string.downloads_open_failed, Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, R.string.downloads_open_failed, Toast.LENGTH_SHORT).show()
    }
}

private fun openDownloadIntent(context: Context, entity: DownloadEntity): Intent? {
    val destination = entity.destination
    if (destination.isNullOrBlank()) {
        return null
    }
    if (destination.startsWith("content://")) {
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.parse(destination), entity.mimeType ?: "*/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val file = File(destination)
    if (!file.exists()) {
        return null
    }
    val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
    return Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, entity.mimeType ?: "*/*")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}

private fun openDownloadWithChooser(context: Context, entity: DownloadEntity) {
    val intent = openDownloadIntent(context, entity)
    if (intent == null) {
        Toast.makeText(context, R.string.downloads_open_failed, Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.downloads_open_with_chooser)),
        )
    }.onFailure {
        Toast.makeText(context, R.string.downloads_open_failed, Toast.LENGTH_SHORT).show()
    }
}

private fun shareDownload(context: Context, entity: DownloadEntity) {
    val intent = openDownloadIntent(context, entity)
    if (intent == null) {
        Toast.makeText(context, R.string.downloads_open_failed, Toast.LENGTH_SHORT).show()
        return
    }
    val share = Intent(Intent.ACTION_SEND).apply {
        type = entity.mimeType ?: "*/*"
        putExtra(Intent.EXTRA_STREAM, intent.data)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(
            Intent.createChooser(share, context.getString(R.string.downloads_share_chooser)),
        )
    }.onFailure {
        Toast.makeText(context, R.string.downloads_open_failed, Toast.LENGTH_SHORT).show()
    }
}

private fun deleteDownloadSource(context: Context, entity: DownloadEntity) {
    val destination = entity.destination
    if (destination.isNullOrBlank()) return
    if (destination.startsWith("content://")) {
        runCatching {
            context.contentResolver.delete(Uri.parse(destination), null, null)
        }
    } else {
        runCatching { File(destination).delete() }
    }
}
