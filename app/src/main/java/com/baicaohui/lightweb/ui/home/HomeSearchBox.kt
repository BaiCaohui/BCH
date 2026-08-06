package com.baicaohui.lightweb.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.baicaohui.lightweb.browser.SuggestionEngine
import com.baicaohui.lightweb.browser.UrlSecurity
import com.baicaohui.lightweb.data.db.HistoryEntity
import com.baicaohui.lightweb.ui.components.SearchPill
import com.baicaohui.lightweb.ui.navigation.BchIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 首页搜索/网址框：未输入时展示最近历史；输入后展示当前搜索引擎联想 + 匹配历史。
 * 历史条目统一用历史图标标注，联想条目用搜索图标。
 */
@Composable
fun HomeSearchBox(
    onSearch: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val scope = rememberCoroutineScope()
    val browserPrefs by app.browserPrefsStore.prefs.collectAsStateWithLifecycle(
        initialValue = app.currentBrowserPrefs,
    )
    val history by app.historyRepository.all.collectAsStateWithLifecycle(initialValue = emptyList())
    var query by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    var engineSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(query, browserPrefs.searchTemplate) {
        if (query.isBlank()) {
            engineSuggestions = emptyList()
            return@LaunchedEffect
        }
        delay(250)
        val q = query.trim()
        if (q.isEmpty()) return@LaunchedEffect
        engineSuggestions = SuggestionEngine.fetch(browserPrefs.searchTemplate, q)
    }

    val trimmedQuery = query.trim()
    val historyLimit = browserPrefs.historySuggestionLimit.coerceIn(0, 10)
    val historySuggestions = remember(history, trimmedQuery, historyLimit) {
        HistorySuggestions.suggest(
            history,
            trimmedQuery,
            limit = if (trimmedQuery.isEmpty()) 8 else historyLimit,
        )
    }
    val showPanel = focused &&
        (engineSuggestions.isNotEmpty() || historySuggestions.isNotEmpty())

    fun hidePanel() {
        focused = false
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        SearchPill(
            query = query,
            onQueryChange = { query = it },
            onSearch = {
                val q = query.trim()
                if (q.isNotEmpty()) {
                    onSearch(q)
                    query = ""
                    hidePanel()
                }
            },
            onFocusChanged = { isFocused ->
                if (isFocused) {
                    focused = true
                } else {
                    // 延迟收起，确保点击联想/历史行时点击事件先完成。
                    scope.launch {
                        delay(150)
                        focused = false
                    }
                }
            },
            modifier = Modifier.widthIn(max = 420.dp),
        )

        if (showPanel) {
            Surface(
                modifier = Modifier
                    .padding(top = 58.dp)
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 8.dp,
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    if (trimmedQuery.isNotEmpty() && engineSuggestions.isNotEmpty()) {
                        SectionLabel(stringResource(R.string.search_suggestion_header))
                        engineSuggestions.take(5).forEach { suggestion ->
                            SuggestionRow(
                                icon = BchIcons.Search,
                                text = suggestion,
                                onClick = {
                                    onSearch(suggestion)
                                    query = ""
                                    hidePanel()
                                },
                            )
                        }
                        if (historySuggestions.isNotEmpty()) HorizontalDivider()
                    }
                    if (historySuggestions.isNotEmpty()) {
                        SectionLabel(stringResource(R.string.search_history_header))
                        historySuggestions.take(
                            if (trimmedQuery.isEmpty()) 5 else historyLimit,
                        ).forEach { entry ->
                            HistoryRow(
                                entry = entry,
                                onClick = {
                                    onOpenUrl(entry.url)
                                    query = ""
                                    hidePanel()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun SuggestionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntity,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = stringResource(R.string.search_history_header),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title.ifBlank { entry.url },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = UrlSecurity.extractHost(entry.url) ?: entry.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
