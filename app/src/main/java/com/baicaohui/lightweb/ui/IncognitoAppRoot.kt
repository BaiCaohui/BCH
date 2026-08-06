package com.baicaohui.lightweb.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.NavigationState
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.browser.TabStatus
import com.baicaohui.lightweb.data.prefs.BrowserPrefs
import com.baicaohui.lightweb.data.prefs.UaMode
import com.baicaohui.lightweb.ui.browser.BrowserScreen
import com.baicaohui.lightweb.ui.browser.BrowserViewModel
import com.baicaohui.lightweb.ui.browser.browserViewModelFactory
import com.baicaohui.lightweb.ui.browser.MoreMenuSheet
import com.baicaohui.lightweb.ui.browser.MoreMenuItem
import com.baicaohui.lightweb.ui.browser.MenuOrder
import com.baicaohui.lightweb.ui.components.TabCountIcon
import com.baicaohui.lightweb.ui.console.ConsoleScreen
import com.baicaohui.lightweb.ui.downloads.DownloadsScreen
import com.baicaohui.lightweb.ui.home.HomeConfig
import com.baicaohui.lightweb.ui.navigation.BchIcons
import com.baicaohui.lightweb.ui.tabs.TabSwitcherScreen
import kotlinx.coroutines.launch

private const val SCREEN_BROWSER = "browser"
private const val SCREEN_TABS = "tabs"
private const val SCREEN_DOWNLOADS = "downloads"
private const val SCREEN_CONSOLE = "console"

@Composable
fun IncognitoAppRoot(initialUrl: String? = null) {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(SCREEN_BROWSER) }
    var menuOpen by remember { mutableStateOf(false) }
    var showUaPicker by remember { mutableStateOf(false) }

    val browserPrefs by app.browserPrefsStore.prefs.collectAsStateWithLifecycle(
        initialValue = app.currentBrowserPrefs,
    )
    val tabs by app.tabManager.tabs.collectAsStateWithLifecycle()
    val currentId by app.tabManager.currentId.collectAsStateWithLifecycle()
    val currentTab = tabs.firstOrNull { it.id == currentId }
    val navState by app.navigationState.collectAsStateWithLifecycle()
    val thumbnails by app.tabThumbnailStore.thumbnails.collectAsStateWithLifecycle()
    val homeConfig by app.homePrefs.config.collectAsStateWithLifecycle(initialValue = HomeConfig.DEFAULT)
    val browserViewModel: BrowserViewModel = viewModel(
        factory = browserViewModelFactory(app.tabManager, app.historyRepository, app.recentSearchStore),
    )

    fun updateNavState() {
        val id = currentId
        if (id == null) return
        val wv = app.webViewStore.get(id)
        app.navigationState.value = NavigationState(
            canGoBack = wv?.canGoBack() == true,
            canGoForward = wv?.canGoForward() == true,
        )
    }

    fun goBrowser() {
        screen = SCREEN_BROWSER
    }

    BackHandler(enabled = screen != SCREEN_BROWSER) { goBrowser() }

    val newTabItem = MoreMenuItem(
            id = "new_tab",
            label = stringResource(R.string.tabs_new),
            icon = Icons.Filled.Add,
            onClick = {
                menuOpen = false
                app.tabManager.newTab("")
                goBrowser()
            },
        )
    val sharedMenuItemsById = mapOf(
        "reload" to MoreMenuItem(
            id = "reload",
            label = stringResource(R.string.action_reload),
            icon = BchIcons.Refresh,
            enabled = currentTab?.url?.isNotBlank() == true,
            onClick = {
                menuOpen = false
                val id = currentId
                if (id != null) app.webViewStore.get(id)?.reload()
            },
        ),
        "reader" to MoreMenuItem(
            id = "reader",
            label = stringResource(
                if (currentTab?.readerMode == true) R.string.menu_reader_exit else R.string.menu_reader,
            ),
            icon = Icons.Filled.MenuBook,
            enabled = currentTab?.url?.isNotBlank() == true,
            highlighted = currentTab?.readerMode == true,
            onClick = {
                menuOpen = false
                browserViewModel.toggleReaderMode()
            },
        ),
        "ua" to MoreMenuItem(
            id = "ua",
            label = stringResource(R.string.menu_ua),
            icon = Icons.Filled.Language,
            onClick = {
                menuOpen = false
                showUaPicker = true
            },
        ),
        "downloads" to MoreMenuItem(
            id = "downloads",
            label = stringResource(R.string.menu_downloads),
            icon = Icons.Filled.Download,
            onClick = {
                menuOpen = false
                screen = SCREEN_DOWNLOADS
            },
        ),
        "console" to MoreMenuItem(
            id = "console",
            label = stringResource(R.string.nav_console),
            icon = Icons.Filled.Terminal,
            onClick = {
                menuOpen = false
                screen = SCREEN_CONSOLE
            },
        ),
    )
    val exitIncognitoItem = MoreMenuItem(
            id = "exit_incognito",
            label = stringResource(R.string.menu_incognito_exit),
            icon = Icons.Filled.VisibilityOff,
            highlighted = true,
            onClick = {
                menuOpen = false
                (context as? Activity)?.finish()
            },
    )
    val menuItems = listOf(newTabItem) +
        MenuOrder.resolve(browserPrefs.menuItemOrder).mapNotNull { sharedMenuItemsById[it] } +
        listOf(exitIncognitoItem)

    Scaffold(
        bottomBar = {
            NavigationBar(modifier = Modifier.height(60.dp)) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = {
                            val id = currentId ?: return@IconButton
                            app.webViewStore.get(id)?.goBack()
                            updateNavState()
                        },
                        enabled = navState.canGoBack,
                    ) {
                        Icon(BchIcons.Back, contentDescription = stringResource(R.string.action_back))
                    }
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = {
                            val id = currentId ?: return@IconButton
                            app.webViewStore.get(id)?.goForward()
                            updateNavState()
                        },
                        enabled = navState.canGoForward,
                    ) {
                        Icon(BchIcons.Forward, contentDescription = stringResource(R.string.action_forward))
                    }
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = {
                            val current = app.tabManager.current
                            val onHomePage = current == null ||
                                (current.url.isBlank() && current.status == TabStatus.EMPTY)
                            if (!onHomePage) app.tabManager.newTab("")
                            goBrowser()
                        },
                    ) {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = stringResource(R.string.nav_home),
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = { screen = SCREEN_TABS },
                    ) {
                        TabCountIcon(
                            count = tabs.size,
                            contentDescription = stringResource(R.string.nav_tabs),
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Filled.VisibilityOff,
                            contentDescription = stringResource(R.string.menu_incognito_exit),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
            when (screen) {
                SCREEN_TABS -> TabSwitcherScreen(
                    tabs = tabs,
                    currentId = currentId,
                    thumbnails = thumbnails,
                    homeConfig = homeConfig,
                    onSelect = { id ->
                        app.tabManager.select(id)
                        goBrowser()
                    },
                    onClose = { id -> app.tabManager.closeTab(id) },
                    onNewTab = {
                        app.tabManager.newTab("")
                        goBrowser()
                    },
                    onCloseAll = {
                        tabs.map { it.id }.forEach { app.tabManager.closeTab(it) }
                    },
                )
                SCREEN_DOWNLOADS -> DownloadsScreen()
                SCREEN_CONSOLE -> ConsoleScreen()
                else -> BrowserScreen(initialUrl = initialUrl, sharedViewModel = browserViewModel)
            }
        }
    }

    if (menuOpen) {
        MoreMenuSheet(
            rows = browserPrefs.menuRows.coerceIn(1, 3),
            items = menuItems,
            onDismiss = { menuOpen = false },
        )
    }

    if (showUaPicker) {
        AlertDialog(
            onDismissRequest = { showUaPicker = false },
            title = { Text(stringResource(R.string.menu_ua_picker_title)) },
            text = {
                Column {
                    listOf(
                        UaMode.ANDROID to R.string.ua_android,
                        UaMode.IPHONE to R.string.ua_iphone,
                        UaMode.DESKTOP to R.string.ua_desktop,
                    ).forEach { (mode, labelRes) ->
                        ListItem(
                            headlineContent = { Text(stringResource(labelRes)) },
                            trailingContent = {
                                if (browserPrefs.uaMode == mode) {
                                    Icon(Icons.Filled.Check, contentDescription = null)
                                }
                            },
                            modifier = Modifier.clickable {
                                showUaPicker = false
                                scope.launch {
                                    app.browserPrefsStore.update { it.copy(uaMode = mode) }
                                }
                                Toast.makeText(context, R.string.ua_switched, Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUaPicker = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}
