package com.baicaohui.lightweb.ui.bookmarks

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.baicaohui.lightweb.data.db.BookmarkEntity
import com.baicaohui.lightweb.ui.components.PlaceholderScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookmarksScreen(onOpenUrl: (String) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val repo = app.bookmarkRepository
    val scope = rememberCoroutineScope()

    val folders by repo.folders.collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    val bookmarks by repo.bookmarks(selectedFolderId).collectAsStateWithLifecycle(initialValue = emptyList())

    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BookmarkEntity?>(null) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var itemMenu by remember { mutableStateOf<BookmarkEntity?>(null) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val text = runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                if (text.isNullOrBlank()) {
                    toast(context.getString(R.string.bookmarks_import_failed))
                } else {
                    val count = repo.importHtml(text)
                    toast(context.getString(R.string.bookmarks_imported, count))
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val html = repo.exportHtml()
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(html) }
                }
                toast(context.getString(R.string.bookmarks_exported))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.bookmarks_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (selectionMode) {
                    TextButton(onClick = {
                        if (selectedIds.isNotEmpty()) {
                            scope.launch {
                                bookmarks.filter { it.id in selectedIds }.forEach { repo.deleteBookmark(it) }
                            }
                        }
                        selectedIds.clear()
                        selectionMode = false
                    }) {
                        Text(stringResource(R.string.bookmarks_delete_selected))
                    }
                    TextButton(onClick = {
                        selectedIds.clear()
                        selectionMode = false
                    }) {
                        Text(stringResource(R.string.bookmarks_cancel))
                    }
                } else {
                    TextButton(onClick = { selectionMode = true }) {
                        Text(stringResource(R.string.bookmarks_select))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.bookmarks_more))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.bookmarks_add_folder)) },
                                onClick = { menuOpen = false; showFolderDialog = true },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.bookmarks_import)) },
                                onClick = { menuOpen = false; importLauncher.launch(arrayOf("text/html", "text/plain")) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.bookmarks_export)) },
                                onClick = { menuOpen = false; exportLauncher.launch("bookmarks.html") },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.bookmarks_clear)) },
                                onClick = { menuOpen = false; showClearConfirm = true },
                            )
                        }
                    }
                }
            }

            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedFolderId == null,
                        onClick = { selectedFolderId = null },
                        label = { Text(stringResource(R.string.bookmarks_all)) },
                    )
                }
                items(folders, key = { it.id }) { folder ->
                    FilterChip(
                        selected = selectedFolderId == folder.id,
                        onClick = { selectedFolderId = folder.id },
                        label = { Text(folder.name) },
                    )
                }
            }

            if (bookmarks.isEmpty()) {
                PlaceholderScreen(text = stringResource(R.string.empty_bookmarks))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(bookmarks, key = { it.id }) { bookmark ->
                        ListItem(
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    if (selectionMode) {
                                        if (bookmark.id in selectedIds) selectedIds.remove(bookmark.id)
                                        else selectedIds.add(bookmark.id)
                                    } else {
                                        onOpenUrl(bookmark.url)
                                    }
                                },
                                onLongClick = { if (!selectionMode) itemMenu = bookmark },
                            ),
                            leadingContent = {
                                if (selectionMode) {
                                    Checkbox(
                                        checked = bookmark.id in selectedIds,
                                        onCheckedChange = {
                                            if (bookmark.id in selectedIds) selectedIds.remove(bookmark.id)
                                            else selectedIds.add(bookmark.id)
                                        },
                                    )
                                }
                            },
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
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.bookmarks_add))
        }
    }

    if (showAddDialog) {
        BookmarkEditDialog(
            initialTitle = "",
            initialUrl = "",
            confirmLabel = stringResource(R.string.bookmarks_add),
            onConfirm = { title, url ->
                showAddDialog = false
                if (url.isNotBlank()) {
                    scope.launch { repo.addBookmark(title.ifBlank { url }, url, selectedFolderId) }
                }
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editing?.let { bookmark ->
        BookmarkEditDialog(
            initialTitle = bookmark.title,
            initialUrl = bookmark.url,
            confirmLabel = stringResource(R.string.bookmarks_edit),
            onConfirm = { title, url ->
                editing = null
                if (url.isNotBlank()) {
                    scope.launch {
                        repo.updateBookmark(bookmark.copy(title = title.ifBlank { url }, url = url))
                    }
                }
            },
            onDismiss = { editing = null },
        )
    }

    itemMenu?.let { bookmark ->
        DropdownMenu(expanded = true, onDismissRequest = { itemMenu = null }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.bookmarks_edit)) },
                onClick = {
                    itemMenu = null
                    editing = bookmark
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.bookmarks_delete)) },
                onClick = {
                    itemMenu = null
                    scope.launch { repo.deleteBookmark(bookmark) }
                },
            )
        }
    }

    if (showFolderDialog) {
        TextInputDialog(
            title = stringResource(R.string.bookmarks_add_folder),
            confirmLabel = stringResource(R.string.bookmarks_add_folder),
            onConfirm = { name ->
                showFolderDialog = false
                if (name.isNotBlank()) {
                    scope.launch {
                        repo.addFolder(name)
                        toast(context.getString(R.string.bookmarks_folder_created))
                    }
                }
            },
            onDismiss = { showFolderDialog = false },
        )
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.bookmarks_confirm_clear_title),
            text = stringResource(R.string.bookmarks_confirm_clear_text),
            onConfirm = {
                showClearConfirm = false
                scope.launch { repo.clearAll() }
            },
            onDismiss = { showClearConfirm = false },
        )
    }
}
