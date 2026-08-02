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
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.ui.browser.BrowserScreen
import com.baicaohui.lightweb.ui.components.PlaceholderScreen
import com.baicaohui.lightweb.ui.home.HomeScreen
import com.baicaohui.lightweb.ui.navigation.BchRoute

@Composable
fun BchAppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val startDestination = if (app.pendingUrl.isNullOrBlank()) {
        BchRoute.HOME.route
    } else {
        BchRoute.BROWSER.route
    }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val bottomRoutes = BchRoute.entries.filter { it.inBottomBar }

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomRoutes.map { it.route }) {
                NavigationBar {
                    bottomRoutes.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
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
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(BchRoute.HOME.route) {
                HomeScreen(onNavigate = { route -> navController.navigate(route) })
            }
            composable(BchRoute.BROWSER.route) {
                BrowserScreen(initialUrl = app.pendingUrl.also { app.pendingUrl = null })
            }
            composable(BchRoute.TABS.route) {
                PlaceholderScreen(text = stringResource(R.string.empty_tabs))
            }
            composable(BchRoute.BOOKMARKS.route) {
                PlaceholderScreen(text = stringResource(R.string.empty_bookmarks))
            }
            composable(BchRoute.HISTORY.route) {
                PlaceholderScreen(text = stringResource(R.string.empty_history))
            }
            composable(BchRoute.SETTINGS.route) {
                PlaceholderScreen(text = stringResource(R.string.settings_placeholder))
            }
        }
    }
}
