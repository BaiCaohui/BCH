package com.baicaohui.lightweb.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.data.db.ShortcutEntity
import com.baicaohui.lightweb.ui.home.BackgroundType
import com.baicaohui.lightweb.ui.home.BookmarksWidget
import com.baicaohui.lightweb.ui.home.ClockWidget
import com.baicaohui.lightweb.ui.home.HomeSearchBox
import com.baicaohui.lightweb.ui.home.HomeConfig
import com.baicaohui.lightweb.ui.home.HomeWidgetType
import com.baicaohui.lightweb.ui.home.RecentWidget
import com.baicaohui.lightweb.ui.home.SearchWidget
import com.baicaohui.lightweb.ui.home.ShortcutDialog
import com.baicaohui.lightweb.ui.home.SpeedDialWidget
import kotlinx.coroutines.launch

@Composable
fun StartPage(
    onSearch: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    incognito: Boolean = false,
) {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    if (incognito) {
        IncognitoHome(onSearch = onSearch, modifier = modifier)
        return
    }
    val config by app.homePrefs.config.collectAsStateWithLifecycle(initialValue = HomeConfig.DEFAULT)
    val shortcuts by app.shortcutRepository.shortcuts.collectAsStateWithLifecycle(initialValue = emptyList())
    val recent by app.historyRepository.recent(12).collectAsStateWithLifecycle(initialValue = emptyList())
    val bookmarks by app.bookmarkRepository.bookmarks(null).collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    var shortcutDialog by remember { mutableStateOf<ShortcutEntity?>(null) }
    var showAddShortcut by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        when (config.background.type) {
            BackgroundType.COLOR -> {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
                if (config.background.color != 0x00000000L) {
                    Box(Modifier.fillMaxSize().background(Color(config.background.color)))
                }
            }
            BackgroundType.GRADIENT -> Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(
                        listOf(
                            Color(config.background.gradientStart),
                            Color(config.background.gradientEnd),
                        ),
                    ),
                ),
            )
            BackgroundType.IMAGE -> {
                val uri = config.background.imageUri
                if (uri != null) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Color.Transparent))
                }
            }
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = config.overlayAlpha)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            config.widgets.filter { it.enabled }.forEach { widget ->
                when (widget.type) {
                    HomeWidgetType.SEARCH -> HomeSearchBox(
                        onSearch = onSearch,
                        onOpenUrl = onOpenUrl,
                    )
                    HomeWidgetType.SPEED_DIAL -> SpeedDialWidget(
                        shortcuts = shortcuts,
                        columns = widget.columns,
                        onOpen = onOpenUrl,
                        onAdd = { showAddShortcut = true },
                        onEdit = { shortcutDialog = it },
                        onDelete = { s -> scope.launch { app.shortcutRepository.delete(s) } },
                    )
                    HomeWidgetType.RECENT -> RecentWidget(
                        entries = recent,
                        limit = widget.limit,
                        onOpen = onOpenUrl,
                    )
                    HomeWidgetType.BOOKMARKS -> BookmarksWidget(
                        bookmarks = bookmarks,
                        limit = widget.limit,
                        onOpen = onOpenUrl,
                    )
                    HomeWidgetType.CLOCK -> ClockWidget()
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showAddShortcut) {
        ShortcutDialog(
            initial = null,
            onConfirm = { title, url, color ->
                showAddShortcut = false
                if (url.isNotBlank()) {
                    scope.launch { app.shortcutRepository.add(title, url, color) }
                }
            },
            onDismiss = { showAddShortcut = false },
        )
    }

    shortcutDialog?.let { shortcut ->
        ShortcutDialog(
            initial = shortcut,
            onConfirm = { title, url, color ->
                shortcutDialog = null
                if (url.isNotBlank()) {
                    scope.launch {
                        app.shortcutRepository.update(
                            shortcut.copy(title = title, url = url, color = color),
                        )
                    }
                }
            },
            onDelete = {
                shortcutDialog = null
                scope.launch { app.shortcutRepository.delete(shortcut) }
            },
            onDismiss = { shortcutDialog = null },
        )
    }
}

@Composable
private fun IncognitoHome(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(Color(0xFF1B1C1E))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(96.dp))
            Icon(
                imageVector = Icons.Filled.VisibilityOff,
                contentDescription = null,
                tint = Color(0xFF8AB4F8),
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.incognito_home_title),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.incognito_home_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9AA0A6),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(48.dp))
            SearchWidget(onSearch = onSearch)
        }
    }
}
