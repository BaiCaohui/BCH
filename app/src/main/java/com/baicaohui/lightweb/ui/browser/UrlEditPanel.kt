package com.baicaohui.lightweb.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.ui.components.CopyIcon
import com.baicaohui.lightweb.ui.navigation.BchIcons

/** 网址编辑/搜索界面的建议面板：最近搜索记录 + 当前页面名称/网址（带复制、修改快捷按钮）。 */
@Composable
fun UrlEditPanel(
    recentSearches: List<String>,
    pageTitle: String,
    pageUrl: String,
    onOpenSearch: (String) -> Unit,
    onCopyPage: () -> Unit,
    onEditPage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(vertical = 4.dp),
    ) {
        if (pageUrl.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
            ) {
                Text(
                    text = pageTitle.ifBlank { pageUrl },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = pageUrl,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1.4f),
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onCopyPage) {
                    Icon(
                        imageVector = CopyIcon,
                        contentDescription = stringResource(R.string.address_copy),
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onEditPage) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.address_edit),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            HorizontalDivider()
        }
        if (recentSearches.isNotEmpty()) {
            Text(
                text = stringResource(R.string.address_recent_searches),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            recentSearches.forEach { query ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSearch(query) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Icon(
                        imageVector = BchIcons.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = query,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
