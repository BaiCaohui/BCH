package com.baicaohui.lightweb.ui.cache

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.baicaohui.lightweb.data.db.CachedPageEntity
import com.baicaohui.lightweb.data.db.CachedPageFolderEntity
import com.baicaohui.lightweb.ui.bookmarks.ConfirmDialog
import com.baicaohui.lightweb.ui.bookmarks.TextInputDialog
import com.baicaohui.lightweb.ui.components.PlaceholderScreen
import com.baicaohui.lightweb.ui.home.Favicon
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CachedPagesScreen(onOpenCache: (CachedPageEntity) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val repo = app.cachedPageRepository
    val scope = rememberCoroutineScope()

    val folders by repo.folders.collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    val pages by remember(selectedFolderId) {
        if (selectedFolderId == null) repo.allPages else repo.pages(selectedFolderId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var itemMenu by remember { mutableStateOf<CachedPageEntity?>(null) }
    var moveTargets by remember { mutableStateOf<List<CachedPageEntity>?>(null) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showFolderManage by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<CachedPageFolderEntity?>(null) }
    var folderToDelete by remember { mutableStateOf<CachedPageFolderEntity?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.cached_pages_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (selectionMode) {
                    if (selectedIds.isNotEmpty()) {
                        TextButton(onClick = {
                            moveTargets = pages.filter { it.id in selectedIds }
                            selectedIds.clear()
                            selectionMode = false
                        }) {
                            Text(stringResource(R.string.cached_pages_move))
                        }
                    }
                    TextButton(onClick = {
                        val idsToDelete = selectedIds.toList()
                        if (idsToDelete.isNotEmpty()) {
                            scope.launch {
                                pages.filter { it.id in idsToDelete }.forEach { repo.deletePage(it) }
                            }
                        }
                        selectedIds.clear()
                        selectionMode = false
                    }) {
                        Text(stringResource(R.string.cached_pages_delete_selected))
                    }
                    TextButton(onClick = {
                        selectedIds.clear()
                        selectionMode = false
                    }) {
                        Text(stringResource(R.string.cached_pages_cancel))
                    }
                } else {
                    TextButton(onClick = { selectionMode = true }) {
                        Text(stringResource(R.string.cached_pages_select))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.bookmarks_more))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cached_pages_add_folder)) },
                                onClick = { menuOpen = false; showFolderDialog = true },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cached_pages_manage_folders)) },
                                onClick = { menuOpen = false; showFolderManage = true },
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

            if (pages.isEmpty()) {
                PlaceholderScreen(text = stringResource(R.string.cached_pages_empty))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(pages, key = { it.id }) { page ->
                        ListItem(
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    if (selectionMode) {
                                        if (page.id in selectedIds) selectedIds.remove(page.id)
                                        else selectedIds.add(page.id)
                                    } else {
                                        onOpenCache(page)
                                    }
                                },
                                onLongClick = { if (!selectionMode) itemMenu = page },
                            ),
                            leadingContent = {
                                if (selectionMode) {
                                    Checkbox(
                                        checked = page.id in selectedIds,
                                        onCheckedChange = {
                                            if (page.id in selectedIds) selectedIds.remove(page.id)
                                            else selectedIds.add(page.id)
                                        },
                                    )
                                } else {
                                    Favicon(
                                        url = page.url,
                                        title = page.title,
                                        iconUrl = page.iconUrl,
                                        size = 40.dp,
                                    )
                                }
                            },
                            headlineContent = {
                                Text(
                                    page.title.ifBlank { page.url },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            supportingContent = {
                                Text(page.url, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                        )
                    }
                }
            }
        }
    }

    itemMenu?.let { page ->
        DropdownMenu(expanded = true, onDismissRequest = { itemMenu = null }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.cached_pages_open)) },
                onClick = {
                    itemMenu = null
                    onOpenCache(page)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.cached_pages_move)) },
                onClick = {
                    itemMenu = null
                    moveTargets = listOf(page)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.cached_pages_delete)) },
                onClick = {
                    itemMenu = null
                    scope.launch { repo.deletePage(page) }
                },
            )
        }
    }

    if (showFolderDialog) {
        TextInputDialog(
            title = stringResource(R.string.cached_pages_add_folder),
            confirmLabel = stringResource(R.string.cached_pages_add_folder),
            onConfirm = { name ->
                showFolderDialog = false
                if (name.isNotBlank()) {
                    scope.launch {
                        repo.addFolder(name)
                        toast(context.getString(R.string.cached_pages_folder_created))
                    }
                }
            },
            onDismiss = { showFolderDialog = false },
        )
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.cached_pages_confirm_clear_title),
            text = stringResource(R.string.cached_pages_confirm_clear_text),
            onConfirm = {
                showClearConfirm = false
                scope.launch { repo.clearAll() }
            },
            onDismiss = { showClearConfirm = false },
        )
    }

    moveTargets?.let { targets ->
        CacheFolderPickerDialog(
            folders = folders,
            selectedFolderId = null,
            onSelect = { folderId, _ ->
                moveTargets = null
                scope.launch {
                    targets.forEach { repo.updatePage(it.copy(folderId = folderId)) }
                }
                toast(context.getString(R.string.cached_pages_moved))
            },
            onDismiss = { moveTargets = null },
        )
    }

    if (showFolderManage) {
        CacheFolderManageDialog(
            folders = folders,
            onRename = { folder ->
                showFolderManage = false
                folderToRename = folder
            },
            onDelete = { folder ->
                showFolderManage = false
                folderToDelete = folder
            },
            onDismiss = { showFolderManage = false },
        )
    }

    folderToRename?.let { folder ->
        TextInputDialog(
            title = stringResource(R.string.cached_pages_rename),
            confirmLabel = stringResource(R.string.cached_pages_rename),
            initial = folder.name,
            onConfirm = { name ->
                folderToRename = null
                if (name.isNotBlank()) {
                    scope.launch { repo.renameFolder(folder, name.trim()) }
                }
            },
            onDismiss = { folderToRename = null },
        )
    }

    folderToDelete?.let { folder ->
        ConfirmDialog(
            title = stringResource(R.string.cached_pages_folder_delete_confirm_title),
            text = stringResource(R.string.cached_pages_folder_delete_confirm_text),
            onConfirm = {
                folderToDelete = null
                scope.launch { repo.deleteFolder(folder) }
            },
            onDismiss = { folderToDelete = null },
        )
    }
}

@Composable
private fun CacheFolderPickerDialog(
    folders: List<CachedPageFolderEntity>,
    selectedFolderId: Long?,
    onSelect: (folderId: Long?, folderName: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(selectedFolderId) }
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cached_pages_move)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.cached_pages_default_folder)) },
                    leadingContent = {
                        RadioButton(
                            selected = selected == null,
                            onClick = { selected = null },
                        )
                    },
                    modifier = Modifier.clickable { selected = null },
                )
                folders.forEach { folder ->
                    ListItem(
                        headlineContent = { Text(folder.name) },
                        leadingContent = {
                            RadioButton(
                                selected = selected == folder.id,
                                onClick = { selected = folder.id },
                            )
                        },
                        modifier = Modifier.clickable { selected = folder.id },
                    )
                }
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.cached_pages_new_folder_hint)) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (newName.isNotBlank()) {
                    val name = newName.trim()
                    scope.launch {
                        val id = app.cachedPageRepository.addFolder(name)
                        onSelect(id, name)
                    }
                } else {
                    val name = selected?.let { id -> folders.firstOrNull { it.id == id }?.name }
                        ?: context.getString(R.string.cached_pages_default_folder)
                    onSelect(selected, name)
                }
            }) {
                Text(stringResource(R.string.dialog_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.bookmarks_cancel))
            }
        },
    )
}

@Composable
private fun CacheFolderManageDialog(
    folders: List<CachedPageFolderEntity>,
    onRename: (CachedPageFolderEntity) -> Unit,
    onDelete: (CachedPageFolderEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cached_pages_manage_folders)) },
        text = {
            if (folders.isEmpty()) {
                Text(stringResource(R.string.cached_pages_empty))
            } else {
                Column {
                    folders.forEach { folder ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { onRename(folder) }) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = stringResource(R.string.cached_pages_rename),
                                )
                            }
                            IconButton(onClick = { onDelete(folder) }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.cached_pages_folder_delete),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}
