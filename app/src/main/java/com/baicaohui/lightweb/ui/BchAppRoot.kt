package com.baicaohui.lightweb.ui

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.IncognitoActivity
import com.baicaohui.lightweb.NavigationState
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.browser.TabStatus
import com.baicaohui.lightweb.browser.BrowserWebView
import com.baicaohui.lightweb.browser.DownloadNames
import com.baicaohui.lightweb.browser.PageHtmlCapture
import com.baicaohui.lightweb.browser.UrlSecurity
import com.baicaohui.lightweb.data.db.CachedPageEntity
import com.baicaohui.lightweb.data.prefs.BrowserPrefs
import com.baicaohui.lightweb.data.prefs.UaMode
import com.baicaohui.lightweb.ui.navigation.BchIcons
import com.baicaohui.lightweb.ui.browser.MoreMenuSheet
import com.baicaohui.lightweb.ui.browser.MoreMenuItem
import com.baicaohui.lightweb.ui.browser.MenuOrder
import com.baicaohui.lightweb.ui.browser.BrowserScreen
import com.baicaohui.lightweb.ui.browser.BrowserViewModel
import com.baicaohui.lightweb.ui.browser.browserViewModelFactory
import com.baicaohui.lightweb.ui.bookmarks.BookmarksScreen
import com.baicaohui.lightweb.ui.bookmarks.AddBookmarkDialog
import com.baicaohui.lightweb.ui.cache.CachedPagesScreen
import com.baicaohui.lightweb.ui.components.PlaceholderScreen
import com.baicaohui.lightweb.ui.components.TabCountIcon
import com.baicaohui.lightweb.ui.console.ConsoleScreen
import com.baicaohui.lightweb.ui.downloads.DownloadsScreen
import com.baicaohui.lightweb.ui.history.HistoryScreen
import com.baicaohui.lightweb.ui.home.HomeEditScreen
import com.baicaohui.lightweb.ui.home.HomeConfig
import com.baicaohui.lightweb.ui.navigation.BchRoute
import com.baicaohui.lightweb.ui.settings.AboutScreen
import com.baicaohui.lightweb.ui.settings.AdBlockSettingsScreen
import com.baicaohui.lightweb.ui.settings.AppearanceScreen
import com.baicaohui.lightweb.ui.settings.BrowseSettingsScreen
import com.baicaohui.lightweb.ui.settings.PrivacyScreen
import com.baicaohui.lightweb.ui.settings.SearchEngineScreen
import com.baicaohui.lightweb.ui.settings.SettingsScreen
import com.baicaohui.lightweb.ui.settings.SiteDataScreen
import com.baicaohui.lightweb.ui.settings.SiteSettingsScreen
import com.baicaohui.lightweb.ui.settings.ToolbarSettingsScreen
import com.baicaohui.lightweb.ui.sniffer.ResourceSniffScreen
import com.baicaohui.lightweb.ui.tabs.TabSwitcherScreen
import kotlinx.coroutines.launch

@Composable
fun BchAppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val browserPrefs by app.browserPrefsStore.prefs.collectAsStateWithLifecycle(initialValue = BrowserPrefs.DEFAULT)
    val navState by app.navigationState.collectAsStateWithLifecycle()
    val currentTabId by app.tabManager.currentId.collectAsStateWithLifecycle()
    val tabCount by app.tabManager.tabs.collectAsStateWithLifecycle(initialValue = emptyList())
    val homeConfig by app.homePrefs.config.collectAsStateWithLifecycle(initialValue = HomeConfig.DEFAULT)
    val folders by app.bookmarkRepository.folders.collectAsStateWithLifecycle(initialValue = emptyList())
    val pageIcons by app.pageIcons.collectAsStateWithLifecycle()
    val browserViewModel: BrowserViewModel = viewModel(
        factory = browserViewModelFactory(app.tabManager, app.historyRepository, app.recentSearchStore),
    )
    var menuOpen by remember { mutableStateOf(false) }
    var bookmarkDraft by remember { mutableStateOf<BookmarkDraft?>(null) }
    val scope = rememberCoroutineScope()
    val currentTab = tabCount.firstOrNull { it.id == currentTabId }
    var showUaPicker by remember { mutableStateOf(false) }

    fun goHome() {
        val current = app.tabManager.current
        val onHomePage = current == null ||
            (current.url.isBlank() && current.status == TabStatus.EMPTY)
        if (!onHomePage) app.tabManager.newTab("")
        navController.navigate(BchRoute.BROWSER.route) {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
            launchSingleTop = true
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val menuItemsById = mapOf(
        "reload" to MoreMenuItem(
            id = "reload",
            label = stringResource(R.string.action_reload),
            icon = BchIcons.Refresh,
            enabled = currentTab?.url?.isNotBlank() == true,
            onClick = {
                menuOpen = false
                val id = currentTabId
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
        "incognito" to MoreMenuItem(
            id = "incognito",
            label = stringResource(R.string.menu_incognito),
            icon = Icons.Filled.VisibilityOff,
            onClick = {
                menuOpen = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    app.purgeIncognitoData()
                    context.startActivity(Intent(context, IncognitoActivity::class.java))
                } else {
                    Toast.makeText(
                        context,
                        R.string.incognito_unsupported,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
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
                navigateTo(BchRoute.DOWNLOADS.route)
            },
        ),
        "download_page" to MoreMenuItem(
            id = "download_page",
            label = stringResource(R.string.menu_download_page),
            icon = Icons.Filled.FileDownload,
            enabled = currentTab?.url?.isNotBlank() == true,
            onClick = {
                menuOpen = false
                val tab = currentTab ?: return@MoreMenuItem
                val id = currentTabId ?: return@MoreMenuItem
                val url = tab.url
                val ua = app.webViewStore.get(id)?.settings?.userAgentString
                    ?: BrowserWebView.ANDROID_UA
                scope.launch {
                    app.appDownloadManager.enqueue(
                        url,
                        ua,
                        "text/html",
                        null,
                        explicitFileName = DownloadNames.fromTitle(tab.title, url, "text/html"),
                    )
                }
                Toast.makeText(context, R.string.download_started, Toast.LENGTH_SHORT).show()
            },
        ),
        "cache_page" to MoreMenuItem(
            id = "cache_page",
            label = stringResource(R.string.menu_cache_page),
            icon = Icons.Filled.Archive,
            enabled = currentTab?.url?.isNotBlank() == true,
            onClick = {
                menuOpen = false
                val tab = currentTab ?: return@MoreMenuItem
                val id = currentTabId ?: return@MoreMenuItem
                val wv = app.webViewStore.get(id) ?: return@MoreMenuItem
                wv.evaluateJavascript(PageHtmlCapture.outerHtmlScript()) { raw ->
                    val html = PageHtmlCapture.parseHtml(raw)
                    if (html.isBlank()) {
                        Toast.makeText(
                            context,
                            R.string.cache_page_save_failed,
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        scope.launch {
                            app.cachedPageRepository.addPage(
                                title = tab.title.ifBlank {
                                    UrlSecurity.extractHost(tab.url) ?: tab.url
                                },
                                url = tab.url,
                                folderId = null,
                                iconUrl = pageIcons[id],
                                html = html,
                            )
                            Toast.makeText(
                                context,
                                R.string.cache_page_saved,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
            },
        ),
        "cached_pages" to MoreMenuItem(
            id = "cached_pages",
            label = stringResource(R.string.menu_cached_pages),
            icon = Icons.Filled.Folder,
            onClick = {
                menuOpen = false
                navigateTo(BchRoute.CACHED_PAGES.route)
            },
        ),
        "sniffer" to MoreMenuItem(
            id = "sniffer",
            label = stringResource(R.string.menu_sniffer),
            icon = Icons.Filled.Radar,
            enabled = currentTab?.url?.isNotBlank() == true,
            onClick = {
                menuOpen = false
                navigateTo(BchRoute.SNIFFER.route)
            },
        ),
        "console" to MoreMenuItem(
            id = "console",
            label = stringResource(R.string.nav_console),
            icon = Icons.Filled.Terminal,
            onClick = {
                menuOpen = false
                navigateTo(BchRoute.CONSOLE.route)
            },
        ),
        "mark_ad" to MoreMenuItem(
            id = "mark_ad",
            label = stringResource(R.string.menu_mark_ad),
            icon = Icons.Filled.Crop,
            enabled = currentTab?.url?.isNotBlank() == true && !app.tabManager.incognito.value,
            onClick = {
                menuOpen = false
                app.markAdRequested.value = true
            },
        ),
        "add_bookmark" to MoreMenuItem(
            id = "add_bookmark",
            label = stringResource(R.string.add_bookmark),
            icon = Icons.Filled.Add,
            enabled = currentTab?.url?.isNotBlank() == true,
            onClick = {
                menuOpen = false
                val tab = currentTab
                val url = tab?.url.orEmpty()
                if (url.isNotBlank() && tab != null) {
                    bookmarkDraft = BookmarkDraft(
                        title = tab.title.ifBlank { url },
                        url = url,
                        folderId = null,
                        iconUrl = pageIcons[currentTabId],
                    )
                }
            },
        ),
        "bookmarks" to MoreMenuItem(
            id = "bookmarks",
            label = stringResource(R.string.nav_bookmarks),
            icon = Icons.Filled.Star,
            onClick = {
                menuOpen = false
                navigateTo(BchRoute.BOOKMARKS.route)
            },
        ),
        "history" to MoreMenuItem(
            id = "history",
            label = stringResource(R.string.nav_history),
            icon = Icons.Filled.DateRange,
            onClick = {
                menuOpen = false
                navigateTo(BchRoute.HISTORY.route)
            },
        ),
        "settings" to MoreMenuItem(
            id = "settings",
            label = stringResource(R.string.nav_settings),
            icon = Icons.Filled.Settings,
            onClick = {
                menuOpen = false
                navController.navigate(BchRoute.SETTINGS.route) {
                    popUpTo(navController.graph.findStartDestination().id)
                    launchSingleTop = true
                }
            },
        ),
    )

    val menuItems = MenuOrder.resolve(browserPrefs.menuItemOrder)
        .mapNotNull { menuItemsById[it] }

    Scaffold(
        bottomBar = {
            NavigationBar(modifier = Modifier.height(60.dp)) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = {
                            app.webViewStore.get(currentTabId ?: return@IconButton)?.goBack()
                            val wv = app.webViewStore.get(currentTabId ?: return@IconButton)
                            app.navigationState.value = NavigationState(
                                canGoBack = wv?.canGoBack() == true,
                                canGoForward = wv?.canGoForward() == true,
                            )
                        },
                        enabled = navState.canGoBack,
                    ) {
                        Icon(BchIcons.Back, contentDescription = stringResource(R.string.action_back))
                    }
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = {
                            app.webViewStore.get(currentTabId ?: return@IconButton)?.goForward()
                            val wv = app.webViewStore.get(currentTabId ?: return@IconButton)
                            app.navigationState.value = NavigationState(
                                canGoBack = wv?.canGoBack() == true,
                                canGoForward = wv?.canGoForward() == true,
                            )
                        },
                        enabled = navState.canGoForward,
                    ) {
                        Icon(BchIcons.Forward, contentDescription = stringResource(R.string.action_forward))
                    }
                }
                NavigationBarItem(
                    selected = currentRoute == BchRoute.BROWSER.route,
                    onClick = { goHome() },
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = BchRoute.HOME.icon!!,
                            contentDescription = stringResource(BchRoute.HOME.labelRes),
                        )
                    },
                    label = if (browserPrefs.showBottomBarLabels) {
                        { Text(stringResource(BchRoute.HOME.labelRes)) }
                    } else {
                        null
                    },
                )
                NavigationBarItem(
                    selected = currentRoute == BchRoute.TABS.route,
                    onClick = { navigateTo(BchRoute.TABS.route) },
                    modifier = Modifier.weight(1f),
                    icon = {
                        TabCountIcon(
                            count = tabCount.size,
                            contentDescription = stringResource(BchRoute.TABS.labelRes),
                        )
                    },
                    label = if (browserPrefs.showBottomBarLabels) {
                        { Text(stringResource(BchRoute.TABS.labelRes)) }
                    } else {
                        null
                    },
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.bottom_menu_more),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BchRoute.BROWSER.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable(BchRoute.BROWSER.route) {
                BrowserScreen(
                    initialUrl = app.pendingUrl.also { app.pendingUrl = null },
                    cachedPage = app.pendingCachedPage.also { app.pendingCachedPage = null },
                    sharedViewModel = browserViewModel,
                )
            }
            composable(BchRoute.HOME_EDIT.route) { HomeEditScreen() }
            composable(BchRoute.TABS.route) {
                val tabs by app.tabManager.tabs.collectAsStateWithLifecycle()
                val currentId by app.tabManager.currentId.collectAsStateWithLifecycle()
                val thumbnails by app.tabThumbnailStore.thumbnails.collectAsStateWithLifecycle()
                TabSwitcherScreen(
                    tabs = tabs,
                    currentId = currentId,
                    thumbnails = thumbnails,
                    homeConfig = homeConfig,
                    onSelect = { id ->
                        app.tabManager.select(id)
                        navController.navigate(BchRoute.BROWSER.route) { launchSingleTop = true }
                    },
                    onClose = { id -> app.tabManager.closeTab(id) },
                    onNewTab = {
                        app.tabManager.newTab("")
                        navController.navigate(BchRoute.BROWSER.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCloseAll = {
                        app.tabManager.tabs.value.map { it.id }.forEach { app.tabManager.closeTab(it) }
                    },
                )
            }
            composable(BchRoute.BOOKMARKS.route) {
                BookmarksScreen(
                    onOpenUrl = { url ->
                        app.pendingUrl = url
                        navController.navigate(BchRoute.BROWSER.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(BchRoute.HISTORY.route) {
                HistoryScreen(
                    onOpenUrl = { url ->
                        app.pendingUrl = url
                        navController.navigate(BchRoute.BROWSER.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(BchRoute.SETTINGS.route) {
                SettingsScreen(onNavigate = { route -> navController.navigate(route) })
            }
            composable(BchRoute.APPEARANCE.route) { AppearanceScreen() }
            composable(BchRoute.TOOLBAR_SETTINGS.route) { ToolbarSettingsScreen() }
            composable(BchRoute.SEARCH_ENGINE.route) { SearchEngineScreen() }
            composable(BchRoute.BROWSE_SETTINGS.route) { BrowseSettingsScreen() }
            composable(BchRoute.PRIVACY.route) { PrivacyScreen() }
            composable(BchRoute.SITE_SETTINGS.route) { SiteSettingsScreen() }
            composable(BchRoute.ADBLOCK.route) { AdBlockSettingsScreen() }
            composable(BchRoute.SITE_DATA.route) { SiteDataScreen() }
            composable(BchRoute.ABOUT.route) { AboutScreen() }
            composable(BchRoute.DOWNLOADS.route) { DownloadsScreen() }
            composable(BchRoute.CONSOLE.route) { ConsoleScreen() }
            composable(BchRoute.SNIFFER.route) {
                ResourceSniffScreen(
                    tabId = currentTabId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(BchRoute.CACHED_PAGES.route) {
                CachedPagesScreen(
                    onOpenCache = { entity: CachedPageEntity ->
                        app.pendingCachedPage = entity
                        app.tabManager.newTab(entity.url)
                        navController.navigate(BchRoute.BROWSER.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
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
                                    navigateTo(BchRoute.BROWSER.route)
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

        bookmarkDraft?.let { draft ->
            AddBookmarkDialog(
                initialTitle = draft.title,
                initialUrl = draft.url,
                initialIconUrl = draft.iconUrl,
                initialFolderId = draft.folderId,
                folders = folders,
                confirmLabel = stringResource(R.string.bookmarks_add),
                pageIconUrl = draft.iconUrl,
                onConfirm = { title, url, folderId, iconUrl ->
                    bookmarkDraft = null
                    scope.launch {
                        app.bookmarkRepository.addBookmark(
                            title.ifBlank { url },
                            url,
                            folderId,
                            iconUrl,
                        )
                    }
                    Toast.makeText(context, R.string.bookmark_added, Toast.LENGTH_SHORT).show()
                },
                onDismiss = { bookmarkDraft = null },
            )
        }
    }
}

private data class BookmarkDraft(
    val title: String,
    val url: String,
    val folderId: Long?,
    val iconUrl: String?,
)
