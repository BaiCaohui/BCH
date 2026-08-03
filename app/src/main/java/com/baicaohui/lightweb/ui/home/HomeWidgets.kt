package com.baicaohui.lightweb.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.baicaohui.lightweb.R
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
    Column(modifier = modifier.fillMaxWidth()) {
        SearchPill(
            query = query,
            onQueryChange = { query = it },
            onSearch = { if (query.isNotBlank()) onSearch(query.trim()) },
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { if (query.isNotBlank()) onSearch(query.trim()) },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(stringResource(R.string.search_submit))
        }
    }
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
    Column(modifier = modifier.fillMaxWidth()) {
        WidgetHeader(stringResource(R.string.widget_speed_dial))
        Spacer(Modifier.height(8.dp))
        if (shortcuts.isEmpty()) {
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.widget_add_shortcut))
            }
        } else {
            shortcuts.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    row.forEach { shortcut ->
                        Column(
                            modifier = Modifier
                                .width(72.dp)
                                .combinedClickable(
                                    onClick = { onOpen(shortcut.url) },
                                    onLongClick = { onEdit(shortcut) },
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Favicon(
                                url = shortcut.url,
                                title = shortcut.title,
                                color = shortcut.color,
                                size = 48.dp,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = shortcut.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    repeat(columns - row.size) {
                        Spacer(Modifier.width(72.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
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
        WidgetHeader(stringResource(R.string.widget_recent))
        entries.take(limit).forEach { entry ->
            ListItem(
                modifier = Modifier.clickable { onOpen(entry.url) },
                headlineContent = {
                    Text(entry.title.ifBlank { entry.url }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = {
                    Text(entry.url, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
            )
        }
    }
}

@Composable
fun BookmarksWidget(
    bookmarks: List<com.baicaohui.lightweb.data.db.BookmarkEntity>,
    limit: Int,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (bookmarks.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        WidgetHeader(stringResource(R.string.widget_bookmarks))
        bookmarks.take(limit).forEach { bookmark ->
            ListItem(
                modifier = Modifier.clickable { onOpen(bookmark.url) },
                headlineContent = {
                    Text(bookmark.title.ifBlank { bookmark.url }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = {
                    Text(bookmark.url, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
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
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun WidgetHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
