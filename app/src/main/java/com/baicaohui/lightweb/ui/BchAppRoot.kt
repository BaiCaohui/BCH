package com.baicaohui.lightweb.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.ui.browser.BrowserScreen
import com.baicaohui.lightweb.ui.bookmarks.BookmarksScreen
import com.baicaohui.lightweb.ui.components.PlaceholderScreen
import com.baicaohui.lightweb.ui.history.HistoryScreen
import com.baicaohui.lightweb.ui.navigation.BchRoute
import com.baicaohui.lightweb.ui.tabs.TabSwitcherScreen

@Composable
fun BchAppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val bottomRoutes = BchRoute.entries.filter { it.inBottomBar }

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomRoutes.forEach { dest ->
                    NavigationBarItem(
                        selected = if (dest == BchRoute.HOME) {
                            currentRoute == BchRoute.BROWSER.route
                        } else {
                            currentRoute == dest.route
                        },
                        onClick = {
                            if (dest == BchRoute.HOME) {
                                app.tabManager.newTab("")
                                navController.navigate(BchRoute.BROWSER.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = dest.icon!!,
                                contentDescription = stringResource(dest.labelRes),
                            )
                        },
                        label = { Text(stringResource(dest.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BchRoute.BROWSER.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(BchRoute.BROWSER.route) {
                BrowserScreen(
                    initialUrl = app.pendingUrl.also { app.pendingUrl = null },
                    onOpenTabs = { navController.navigate(BchRoute.TABS.route) },
                )
            }
            composable(BchRoute.TABS.route) {
                val tabs by app.tabManager.tabs.collectAsStateWithLifecycle()
                val currentId by app.tabManager.currentId.collectAsStateWithLifecycle()
                TabSwitcherScreen(
                    tabs = tabs,
                    currentId = currentId,
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
                PlaceholderScreen(text = stringResource(R.string.settings_placeholder))
            }
        }
    }
}
