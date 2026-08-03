package com.baicaohui.lightweb.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.data.db.HistoryEntity
import com.baicaohui.lightweb.ui.components.PlaceholderScreen
import com.baicaohui.lightweb.ui.bookmarks.ConfirmDialog
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(onOpenUrl: (String) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val repo = app.historyRepository
    val scope = rememberCoroutineScope()
    val all by repo.all.collectAsStateWithLifecycle(initialValue = emptyList())

    var query by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }
    var itemMenu by remember { mutableStateOf<HistoryEntity?>(null) }

    val filtered = remember(all, query) {
        if (query.isBlank()) {
            all
        } else {
            all.filter {
                it.url.contains(query, ignoreCase = true) || it.title.contains(query, ignoreCase = true)
            }
        }
    }
    val grouped = remember(filtered) { groupByDay(filtered) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { showClearConfirm = true }, enabled = all.isNotEmpty()) {
                Text(stringResource(R.string.history_clear))
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.history_search)) },
        )
        if (grouped.isEmpty()) {
            PlaceholderScreen(text = stringResource(R.string.empty_history))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                grouped.forEach { (label, entries) ->
                    item(key = "header-$label") {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(entries, key = { it.id }) { entry ->
                        ListItem(
                            modifier = Modifier.combinedClickable(
                                onClick = { onOpenUrl(entry.url) },
                                onLongClick = { itemMenu = entry },
                            ),
                            headlineContent = {
                                Text(
                                    text = entry.title.ifBlank { entry.url },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            supportingContent = {
                                Text(entry.url, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                        )
                    }
                }
            }
        }
    }

    itemMenu?.let { entry ->
        DropdownMenu(expanded = true, onDismissRequest = { itemMenu = null }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.history_delete)) },
                onClick = {
                    itemMenu = null
                    scope.launch { repo.delete(entry.url) }
                },
            )
        }
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.history_confirm_clear_title),
            text = stringResource(R.string.history_confirm_clear_text),
            onConfirm = {
                showClearConfirm = false
                scope.launch { repo.clear() }
            },
            onDismiss = { showClearConfirm = false },
        )
    }
}

private fun groupByDay(entries: List<HistoryEntity>): List<Pair<String, List<HistoryEntity>>> {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    return entries.groupBy { entry ->
        val date = Instant.ofEpochMilli(entry.visitTime).atZone(ZoneId.systemDefault()).toLocalDate()
        when (date) {
            today -> "今天"
            yesterday -> "昨天"
            else -> "更早"
        }
    }.toList()
}
