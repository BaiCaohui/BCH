package com.baicaohui.lightweb.ui.bookmarks

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.data.db.FolderEntity
import com.baicaohui.lightweb.ui.home.Favicon
import com.baicaohui.lightweb.util.BookmarkIconStore
import kotlinx.coroutines.launch

/**
 * 添加/编辑书签对话框：标题、网址、图标（网页图标/选择图片/清除）、收藏夹（二级弹窗选择或新建）。
 */
@Composable
fun AddBookmarkDialog(
    initialTitle: String,
    initialUrl: String,
    initialIconUrl: String?,
    initialFolderId: Long?,
    folders: List<FolderEntity>,
    confirmLabel: String,
    pageIconUrl: String? = null,
    onConfirm: (title: String, url: String, folderId: Long?, iconUrl: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(initialTitle) }
    var url by remember { mutableStateOf(initialUrl) }
    var iconUrl by remember { mutableStateOf(initialIconUrl) }
    var folderId by remember { mutableStateOf(initialFolderId) }
    var showFolderPicker by remember { mutableStateOf(false) }
    val folderName = remember(folders, folderId) {
        folderId?.let { id -> folders.firstOrNull { it.id == id }?.name }
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                BookmarkIconStore.savePickedImage(context, uri)?.let { iconUrl = it }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(confirmLabel) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.bookmarks_title_label)) },
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.bookmarks_url_label)) },
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Favicon(
                        url = url.ifBlank { "https://example.com" },
                        title = title,
                        iconUrl = iconUrl,
                        size = 40.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    if (pageIconUrl != null) {
                        TextButton(
                            onClick = { iconUrl = pageIconUrl },
                            enabled = iconUrl != pageIconUrl,
                        ) {
                            Text(stringResource(R.string.bookmarks_icon_use_page))
                        }
                    }
                    TextButton(
                        onClick = {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    ) {
                        Text(stringResource(R.string.bookmarks_icon_choose))
                    }
                    if (iconUrl != null) {
                        TextButton(onClick = { iconUrl = null }) {
                            Text(stringResource(R.string.bookmarks_icon_clear))
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFolderPicker = true }
                        .padding(vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = folderName ?: stringResource(R.string.bookmarks_default_folder),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title.trim(), url.trim(), folderId, iconUrl) },
                enabled = url.isNotBlank(),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.bookmarks_cancel))
            }
        },
    )

    if (showFolderPicker) {
        FolderPickerDialog(
            folders = folders,
            selectedFolderId = folderId,
            onSelect = { id, _ ->
                folderId = id
                showFolderPicker = false
            },
            onDismiss = { showFolderPicker = false },
        )
    }
}

/** 二级弹窗：选择收藏夹（含默认收藏夹）或输入名称新建。 */
@Composable
fun FolderPickerDialog(
    folders: List<FolderEntity>,
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
        title = { Text(stringResource(R.string.bookmarks_folder)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.bookmarks_default_folder)) },
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
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.bookmarks_new_folder_hint)) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isNotBlank()) {
                        val name = newName.trim()
                        scope.launch {
                            val id = app.bookmarkRepository.addFolder(name)
                            onSelect(id, name)
                        }
                    } else {
                        val name = selected?.let { id -> folders.firstOrNull { it.id == id }?.name }
                            ?: context.getString(R.string.bookmarks_default_folder)
                        onSelect(selected, name)
                    }
                },
            ) {
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

/** 收藏夹管理：重命名 / 删除用户自建收藏夹。 */
@Composable
fun FolderManageDialog(
    folders: List<FolderEntity>,
    onRename: (FolderEntity) -> Unit,
    onDelete: (FolderEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bookmarks_manage_folders)) },
        text = {
            if (folders.isEmpty()) {
                Text(stringResource(R.string.empty_bookmarks))
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
                                    contentDescription = stringResource(R.string.bookmarks_rename),
                                )
                            }
                            IconButton(onClick = { onDelete(folder) }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.bookmarks_folder_delete),
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
