package com.baicaohui.lightweb.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.baicaohui.lightweb.R

enum class BchRoute(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector? = null,
    val inBottomBar: Boolean = false,
) {
    HOME("home", R.string.nav_home, Icons.Filled.Home, true),
    BROWSER("browser", R.string.app_name, null, false),
    TABS("tabs", R.string.nav_tabs, Icons.Filled.List, true),
    BOOKMARKS("bookmarks", R.string.nav_bookmarks, Icons.Filled.Star, true),
    HISTORY("history", R.string.nav_history, Icons.Filled.DateRange, true),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings, true),
    HOME_EDIT("homeEdit", R.string.home_edit, null, false),
    APPEARANCE("appearance", R.string.settings_appearance, null, false),
    TOOLBAR_SETTINGS("toolbarSettings", R.string.settings_toolbar, null, false),
    SEARCH_ENGINE("searchEngine", R.string.settings_search_engine, null, false),
    BROWSE_SETTINGS("browseSettings", R.string.settings_browsing, null, false),
    PRIVACY("privacy", R.string.settings_privacy, null, false),
    SITE_SETTINGS("siteSettings", R.string.settings_site_settings, null, false),
    ABOUT("about", R.string.settings_about, null, false),
}

/** M1 用到的工具栏图标，集中声明避免散落 import。 */
object BchIcons {
    val Back = Icons.AutoMirrored.Filled.ArrowBack
    val Forward = Icons.AutoMirrored.Filled.ArrowForward
    val Refresh = Icons.Filled.Refresh
    val Search = Icons.Filled.Search
}
