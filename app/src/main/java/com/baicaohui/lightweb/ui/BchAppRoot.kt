package com.baicaohui.lightweb.ui

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.NavigationState
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.browser.TabStatus
import com.baicaohui.lightweb.data.prefs.BrowserPrefs
import com.baicaohui.lightweb.ui.navigation.BchIcons
import com.baicaohui.lightweb.ui.browser.BrowserScreen
import com.baicaohui.lightweb.ui.bookmarks.BookmarksScreen
import com.baicaohui.lightweb.ui.components.PlaceholderScreen
import com.baicaohui.lightweb.ui.components.TabCountIcon
import com.baicaohui.lightweb.ui.history.HistoryScreen
import com.baicaohui.lightweb.ui.home.HomeEditScreen
import com.baicaohui.lightweb.ui.home.HomeConfig
import com.baicaohui.lightweb.ui.navigation.BchRoute
import com.baicaohui.lightweb.ui.settings.AboutScreen
import com.baicaohui.lightweb.ui.settings.AppearanceScreen
import com.baicaohui.lightweb.ui.settings.BrowseSettingsScreen
import com.baicaohui.lightweb.ui.settings.PrivacyScreen
import com.baicaohui.lightweb.ui.settings.SearchEngineScreen
import com.baicaohui.lightweb.ui.settings.SettingsScreen
import com.baicaohui.lightweb.ui.settings.SiteSettingsScreen
import com.baicaohui.lightweb.ui.settings.ToolbarSettingsScreen
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
    var menuOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentTab = tabCount.firstOrNull { it.id == currentTabId }

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
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_reload)) },
                            enabled = currentTabId != null,
                            leadingIcon = {
                                Icon(BchIcons.Refresh, contentDescription = null)
                            },
                            onClick = {
                                menuOpen = false
                                app.webViewStore.get(currentTabId ?: return@DropdownMenuItem)?.reload()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_bookmark)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Add, contentDescription = null)
                            },
                            enabled = currentTab?.url?.isNotBlank() == true,
                            onClick = {
                                menuOpen = false
                                val tab = currentTab ?: return@DropdownMenuItem
                                val url = tab.url
                                if (url.isNotBlank()) {
                                    scope.launch {
                                        app.bookmarkRepository.addBookmark(
                                            tab.title.ifBlank { url },
                                            url,
                                            null,
                                        )
                                    }
                                    Toast.makeText(context, R.string.bookmark_added, Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.nav_bookmarks)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Star, contentDescription = null)
                            },
                            onClick = {
                                menuOpen = false
                                navigateTo(BchRoute.BOOKMARKS.route)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.nav_history)) },
                            leadingIcon = {
                                Icon(Icons.Filled.DateRange, contentDescription = null)
                            },
                            onClick = {
                                menuOpen = false
                                navigateTo(BchRoute.HISTORY.route)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.nav_settings)) },
                            leadingIcon = {
                                Icon(Icons.Filled.Settings, contentDescription = null)
                            },
                            onClick = {
                                menuOpen = false
                                navController.navigate(BchRoute.SETTINGS.route) {
                                    popUpTo(navController.graph.findStartDestination().id)
                                    launchSingleTop = true
                                }
                            },
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
            composable(BchRoute.ABOUT.route) { AboutScreen() }
        }
    }
}
