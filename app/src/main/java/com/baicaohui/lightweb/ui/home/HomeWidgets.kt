package com.baicaohui.lightweb.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.data.db.BookmarkEntity
import com.baicaohui.lightweb.data.db.HistoryEntity
import com.baicaohui.lightweb.data.db.ShortcutEntity
import com.baicaohui.lightweb.ui.components.SearchPill
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SearchWidget(onSearch: (String) -> Unit, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    SearchPill(
        query = query,
        onQueryChange = { query = it },
        onSearch = { if (query.isNotBlank()) onSearch(query.trim()) },
        modifier = modifier.widthIn(max = 420.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpeedDialWidget(
    shortcuts: List<ShortcutEntity>,
    columns: Int,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (ShortcutEntity) -> Unit,
    onDelete: (ShortcutEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onAdd),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (shortcuts.isEmpty()) {
            GhostAddItem(onAdd = onAdd)
        } else {
            shortcuts.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    row.forEach { shortcut ->
                        SpeedDialItem(
                            shortcut = shortcut,
                            onOpen = { onOpen(shortcut.url) },
                            onEdit = { onEdit(shortcut) },
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpeedDialItem(
    shortcut: ShortcutEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(68.dp)
            .combinedClickable(onClick = onOpen, onLongClick = onEdit),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Favicon(
            url = shortcut.url,
            title = shortcut.title,
            color = shortcut.color,
            size = 44.dp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = shortcut.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GhostAddItem(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .width(68.dp)
            .clickable(onClick = onAdd),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.widget_add_shortcut),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun RecentWidget(
    entries: List<HistoryEntity>,
    limit: Int,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        entries.take(limit).forEach { entry ->
            CompactRow(
                url = entry.url,
                title = entry.title.ifBlank { entry.url },
                onClick = { onOpen(entry.url) },
            )
        }
    }
}

@Composable
fun BookmarksWidget(
    bookmarks: List<BookmarkEntity>,
    limit: Int,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (bookmarks.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        bookmarks.take(limit).forEach { bookmark ->
            CompactRow(
                url = bookmark.url,
                title = bookmark.title.ifBlank { bookmark.url },
                onClick = { onOpen(bookmark.url) },
            )
        }
    }
}

@Composable
private fun CompactRow(
    url: String,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Favicon(url = url, title = title, size = 20.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = com.baicaohui.lightweb.browser.UrlSecurity.extractHost(url) ?: url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ClockWidget(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    val time = remember(now) {
        DateTimeFormatter.ofPattern("HH:mm").format(
            Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()),
        )
    }
    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            text = time,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
