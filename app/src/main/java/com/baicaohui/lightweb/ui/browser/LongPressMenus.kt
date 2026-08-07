package com.baicaohui.lightweb.ui.browser

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import coil.compose.AsyncImage
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.browser.SelectionInfo
import kotlin.math.roundToInt

/** 长按文字后的选区锚点弹窗：复制 / 全选 / 在新标签页中搜索。 */
@Composable
fun TextSelectionPopup(
    info: SelectionInfo,
    density: Float,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onSearch: () -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(
            x = (info.left * density).roundToInt(),
            y = ((info.top + info.height) * density).roundToInt(),
        ),
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                MenuActionItem(Icons.Filled.ContentCopy, R.string.context_copy, onCopy)
                MenuActionItem(Icons.Filled.SelectAll, R.string.context_select_all, onSelectAll)
                MenuActionItem(Icons.Filled.Search, R.string.context_search_new_tab, onSearch)
            }
        }
    }
}

/** 长按链接后的居中弹窗：链接图标 + 链接文字 + 链接地址 + 六个动作。 */
@Composable
fun LinkContextDialog(
    url: String,
    linkText: String,
    onOpenNewTab: () -> Unit,
    onOpenIncognito: () -> Unit,
    onCopyAddress: () -> Unit,
    onCopyText: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Filled.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = linkText.ifBlank { url },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.size(16.dp))
                MenuActionItem(Icons.Filled.OpenInNew, R.string.context_open_new_tab, onOpenNewTab)
                MenuActionItem(
                    Icons.Filled.VisibilityOff,
                    R.string.context_open_incognito,
                    onOpenIncognito,
                )
                MenuActionItem(Icons.Filled.Link, R.string.context_copy_link_address, onCopyAddress)
                MenuActionItem(
                    Icons.Filled.ContentCopy,
                    R.string.context_copy_link_text,
                    onCopyText,
                )
                MenuActionItem(Icons.Filled.FileDownload, R.string.context_download_link, onDownload)
                MenuActionItem(Icons.Filled.Share, R.string.context_share_link, onShare)
            }
        }
    }
}

/** 长按图片后的居中弹窗：小图 + 图片名 + 图片地址 + 三个动作。 */
@Composable
fun ImageContextDialog(
    url: String,
    name: String,
    onOpenNewTab: () -> Unit,
    onCopy: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    AsyncImage(
                        model = url,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.size(16.dp))
                MenuActionItem(Icons.Filled.OpenInNew, R.string.context_open_image, onOpenNewTab)
                MenuActionItem(Icons.Filled.ContentCopy, R.string.context_copy_image, onCopy)
                MenuActionItem(Icons.Filled.FileDownload, R.string.context_download_image, onDownload)
            }
        }
    }
}

@Composable
private fun MenuActionItem(
    icon: ImageVector,
    @StringRes labelRes: Int,
    onClick: () -> Unit,
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        headlineContent = { Text(stringResource(labelRes)) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
