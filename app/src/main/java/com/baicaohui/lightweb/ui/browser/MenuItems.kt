package com.baicaohui.lightweb.ui.browser

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.graphics.vector.ImageVector
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.ui.navigation.BchIcons

/** 三杠菜单全部可配置项的元数据（图标 + 文案），供设置页与菜单构建共用。 */
object MenuItems {

    data class Spec(
        val id: String,
        @StringRes val labelRes: Int,
        val icon: ImageVector,
    )

    val SPECS = listOf(
        Spec("reload", R.string.action_reload, BchIcons.Refresh),
        Spec("reader", R.string.menu_reader, Icons.Filled.MenuBook),
        Spec("incognito", R.string.menu_incognito, Icons.Filled.VisibilityOff),
        Spec("ua", R.string.menu_ua, Icons.Filled.Language),
        Spec("downloads", R.string.menu_downloads, Icons.Filled.Download),
        Spec("console", R.string.nav_console, Icons.Filled.Terminal),
        Spec("add_bookmark", R.string.add_bookmark, Icons.Filled.Add),
        Spec("bookmarks", R.string.nav_bookmarks, Icons.Filled.Star),
        Spec("history", R.string.nav_history, Icons.Filled.DateRange),
        Spec("settings", R.string.nav_settings, Icons.Filled.Settings),
    )

    fun byId(id: String): Spec? = SPECS.firstOrNull { it.id == id }
}
