package com.baicaohui.lightweb.ui.settings

import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.data.db.SiteSettingEntity
import com.baicaohui.lightweb.browser.HttpsMode
import com.baicaohui.lightweb.data.prefs.BrowserPrefs
import com.baicaohui.lightweb.data.prefs.ToolbarPosition
import com.baicaohui.lightweb.data.prefs.UaMode
import com.baicaohui.lightweb.ui.browser.MenuItems
import com.baicaohui.lightweb.ui.browser.MenuOrder
import com.baicaohui.lightweb.ui.bookmarks.TextInputDialog
import com.baicaohui.lightweb.ui.components.PlaceholderScreen
import com.baicaohui.lightweb.ui.downloads.DownloadSettingsSection
import kotlinx.coroutines.launch

private val SEARCH_PRESETS = listOf(
    "https://www.bing.com/search?q=%s" to "Bing",
    "https://www.baidu.com/s?wd=%s" to "百度",
    "https://www.google.com/search?q=%s" to "Google",
)

@Composable
fun ToolbarSettingsScreen() {
    val app = LocalContext.current.applicationContext as BchApp
    val store = app.browserPrefsStore
    val prefs by store.prefs.collectAsStateWithLifecycle(initialValue = BrowserPrefs.DEFAULT)
    val scope = rememberCoroutineScope()
    fun update(transform: (BrowserPrefs) -> BrowserPrefs) {
        scope.launch { store.update(transform) }
    }

    SettingsColumn(title = stringResource(R.string.settings_toolbar)) {
        SectionTitle(stringResource(R.string.toolbar_position))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            FilterChip(
                selected = prefs.toolbarPosition == ToolbarPosition.TOP,
                onClick = { update { it.copy(toolbarPosition = ToolbarPosition.TOP) } },
                label = { Text(stringResource(R.string.toolbar_top)) },
            )
            FilterChip(
                selected = prefs.toolbarPosition == ToolbarPosition.BOTTOM,
                onClick = { update { it.copy(toolbarPosition = ToolbarPosition.BOTTOM) } },
                label = { Text(stringResource(R.string.toolbar_bottom)) },
            )
        }
        Spacer(Modifier.height(8.dp))
        SectionTitle(stringResource(R.string.toolbar_buttons))
        SettingSwitch(
            stringResource(R.string.toolbar_show_back),
            prefs.showBack,
        ) { enabled -> update { it.copy(showBack = enabled) } }
        SettingSwitch(
            stringResource(R.string.toolbar_show_forward),
            prefs.showForward,
        ) { enabled -> update { it.copy(showForward = enabled) } }
        SettingSwitch(
            stringResource(R.string.toolbar_show_reload),
            prefs.showReload,
        ) { enabled -> update { it.copy(showReload = enabled) } }
        SettingSwitch(
            stringResource(R.string.toolbar_auto_hide),
            prefs.autoHideToolbar,
        ) { enabled -> update { it.copy(autoHideToolbar = enabled) } }
        Spacer(Modifier.height(12.dp))
        SectionTitle(stringResource(R.string.toolbar_buttons))
        SettingSwitch(
            stringResource(R.string.bottom_nav_labels),
            prefs.showBottomBarLabels,
        ) { enabled -> update { it.copy(showBottomBarLabels = enabled) } }
        Spacer(Modifier.height(12.dp))
        SectionTitle(stringResource(R.string.menu_rows))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 2, 3).forEach { rows ->
                FilterChip(
                    selected = prefs.menuRows == rows,
                    onClick = { update { it.copy(menuRows = rows) } },
                    label = { Text(stringResource(R.string.menu_rows_count, rows)) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SectionTitle(stringResource(R.string.menu_icons))
        val visibleIds = remember(prefs.menuItemOrder) {
            MenuOrder.resolve(prefs.menuItemOrder)
        }
        val hiddenIds = MenuItems.SPECS.map { it.id }.filterNot { it in visibleIds }
        (visibleIds + hiddenIds).forEach { id ->
            val spec = MenuItems.byId(id) ?: return@forEach
            val visibleIndex = visibleIds.indexOf(id)
            val isVisible = visibleIndex >= 0
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = null,
                    tint = if (isVisible) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    },
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(spec.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    color = if (isVisible) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                )
                IconButton(
                    onClick = {
                        update {
                            it.copy(
                                menuItemOrder = MenuOrder.move(
                                    visibleIds,
                                    visibleIndex,
                                    visibleIndex - 1,
                                ),
                            )
                        }
                    },
                    enabled = isVisible && visibleIndex > 0,
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.home_edit_move_up),
                    )
                }
                IconButton(
                    onClick = {
                        update {
                            it.copy(
                                menuItemOrder = MenuOrder.move(
                                    visibleIds,
                                    visibleIndex,
                                    visibleIndex + 1,
                                ),
                            )
                        }
                    },
                    enabled = isVisible && visibleIndex < visibleIds.lastIndex,
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.home_edit_move_down),
                    )
                }
                Switch(
                    checked = isVisible,
                    onCheckedChange = { show ->
                        if (show) {
                            update { it.copy(menuItemOrder = visibleIds + id) }
                        } else if (visibleIds.size > 1) {
                            update {
                                it.copy(menuItemOrder = visibleIds.filterNot { item -> item == id })
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun SearchEngineScreen() {
    val app = LocalContext.current.applicationContext as BchApp
    val store = app.browserPrefsStore
    val prefs by store.prefs.collectAsStateWithLifecycle(initialValue = BrowserPrefs.DEFAULT)
    val scope = rememberCoroutineScope()
    var customTemplate by remember { mutableStateOf("") }

    SettingsColumn(title = stringResource(R.string.settings_search_engine)) {
        SectionTitle(stringResource(R.string.search_engine_presets))
        SEARCH_PRESETS.forEach { (template, name) ->
            ListItem(
                headlineContent = { Text(name) },
                modifier = Modifier.clickable {
                    scope.launch { store.update { it.copy(searchTemplate = template) } }
                },
                supportingContent = {
                    if (prefs.searchTemplate == template) {
                        Text("✓", color = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        SectionTitle(stringResource(R.string.search_custom_template))
        OutlinedTextField(
            value = customTemplate,
            onValueChange = { customTemplate = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("https://example.com/search?q=%s") },
        )
        TextButton(
            onClick = {
                if (customTemplate.isNotBlank() && customTemplate.contains("%s")) {
                    scope.launch { store.update { it.copy(searchTemplate = customTemplate.trim()) } }
                }
            },
        ) {
            Text(stringResource(R.string.search_save))
        }
    }
}

@Composable
fun BrowseSettingsScreen() {
    val app = LocalContext.current.applicationContext as BchApp
    val store = app.browserPrefsStore
    val prefs by store.prefs.collectAsStateWithLifecycle(initialValue = BrowserPrefs.DEFAULT)
    val scope = rememberCoroutineScope()
    var customUa by remember { mutableStateOf("") }
    fun update(transform: (BrowserPrefs) -> BrowserPrefs) {
        scope.launch { store.update(transform) }
    }

    SettingsColumn(title = stringResource(R.string.settings_browsing)) {
        SectionTitle(stringResource(R.string.browse_ua))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            listOf(
                UaMode.DEFAULT,
                UaMode.ANDROID,
                UaMode.IPHONE,
                UaMode.DESKTOP,
                UaMode.CUSTOM,
            ).forEach { mode ->
                FilterChip(
                    selected = prefs.uaMode == mode,
                    onClick = { update { it.copy(uaMode = mode) } },
                    label = { Text(uaLabel(mode)) },
                )
            }
        }
        if (prefs.uaMode == UaMode.CUSTOM) {
            OutlinedTextField(
                value = customUa,
                onValueChange = { customUa = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.browse_custom_ua)) },
            )
            TextButton(
                onClick = {
                    if (customUa.isNotBlank()) {
                        update { it.copy(customUa = customUa.trim()) }
                    }
                },
            ) {
                Text(stringResource(R.string.search_save))
            }
        }
        Spacer(Modifier.height(12.dp))
        SectionTitle(stringResource(R.string.browse_ad_level))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("OFF", "BASIC", "STRICT").forEach { level ->
                FilterChip(
                    selected = prefs.adLevel == level,
                    onClick = { update { it.copy(adLevel = level) } },
                    label = { Text(adLabel(level)) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        SettingSwitch(
            stringResource(R.string.browse_js_default),
            prefs.defaultJsEnabled,
        ) { enabled -> update { it.copy(defaultJsEnabled = enabled) } }
        SettingSwitch(
            stringResource(R.string.tab_preview_enabled),
            prefs.tabPreviewEnabled,
        ) { enabled -> update { it.copy(tabPreviewEnabled = enabled) } }
        Spacer(Modifier.height(12.dp))
        DownloadSettingsSection(
            prefs = prefs,
            onUpdate = { transform -> update(transform) },
        )
        Spacer(Modifier.height(12.dp))
        SectionTitle(stringResource(R.string.search_history_limit))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 1, 2, 3, 4, 5).forEach { limit ->
                FilterChip(
                    selected = prefs.historySuggestionLimit == limit,
                    onClick = { update { it.copy(historySuggestionLimit = limit) } },
                    label = {
                        Text(
                            if (limit == 0) {
                                stringResource(R.string.search_history_limit_off)
                            } else {
                                stringResource(R.string.search_history_limit_count, limit)
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun PrivacyScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val store = app.browserPrefsStore
    val prefs by store.prefs.collectAsStateWithLifecycle(initialValue = BrowserPrefs.DEFAULT)
    val scope = rememberCoroutineScope()
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun update(transform: (BrowserPrefs) -> BrowserPrefs) {
        scope.launch { store.update(transform) }
    }

    fun clearCookies() = CookieManager.getInstance().removeAllCookies(null)
    fun clearCache() {
        WebView(context).apply {
            clearCache(true)
            destroy()
        }
        scope.launch { app.readerCacheRepository.clear() }
    }

    SettingsColumn(title = stringResource(R.string.settings_privacy)) {
        SettingSwitch(
            stringResource(R.string.privacy_safe_browsing),
            prefs.safeBrowsing,
        ) { enabled -> update { it.copy(safeBrowsing = enabled) } }
        Text(
            text = stringResource(R.string.privacy_safe_browsing_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        SettingSwitch(
            stringResource(R.string.privacy_third_party_cookies),
            prefs.thirdPartyCookies,
        ) { enabled -> update { it.copy(thirdPartyCookies = enabled) } }
        SettingSwitch(
            stringResource(R.string.privacy_anti_tracking),
            prefs.antiTracking,
        ) { enabled -> update { it.copy(antiTracking = enabled) } }
        Text(
            text = stringResource(R.string.privacy_anti_tracking_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        SettingSwitch(
            stringResource(R.string.privacy_permission_prompt),
            prefs.permissionPromptEnabled,
        ) { enabled -> update { it.copy(permissionPromptEnabled = enabled) } }
        Text(
            text = stringResource(R.string.privacy_permission_prompt_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        SettingSwitch(
            stringResource(R.string.privacy_autoplay),
            prefs.autoplayAllowed,
        ) { enabled -> update { it.copy(autoplayAllowed = enabled) } }
        Text(
            text = stringResource(R.string.privacy_autoplay_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        SettingSwitch(
            stringResource(R.string.privacy_download_risk),
            prefs.downloadRiskWarnings,
        ) { enabled -> update { it.copy(downloadRiskWarnings = enabled) } }
        Text(
            text = stringResource(R.string.privacy_download_risk_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        SettingSwitch(
            stringResource(R.string.privacy_clear_on_exit),
            prefs.clearCookiesOnExit,
        ) { enabled -> update { it.copy(clearCookiesOnExit = enabled) } }
        Text(
            text = stringResource(R.string.privacy_clear_on_exit_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )

        Spacer(Modifier.height(12.dp))
        SectionTitle(stringResource(R.string.privacy_https_mode))
        Text(
            text = stringResource(R.string.privacy_https_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                HttpsMode.OFF to R.string.https_mode_off,
                HttpsMode.PREFER to R.string.https_mode_prefer,
                HttpsMode.STRICT to R.string.https_mode_strict,
            ).forEach { (mode, labelRes) ->
                FilterChip(
                    selected = prefs.httpsMode == mode,
                    onClick = { update { it.copy(httpsMode = mode) } },
                    label = { Text(stringResource(labelRes)) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionTitle(stringResource(R.string.privacy_clear_data))
        ClearDataRow(stringResource(R.string.privacy_clear_cookies)) {
            confirmAction = { clearCookies() }
        }
        ClearDataRow(stringResource(R.string.privacy_clear_cache)) {
            confirmAction = { clearCache() }
        }
        ClearDataRow(stringResource(R.string.privacy_clear_history)) {
            confirmAction = { scope.launch { app.historyRepository.clear() } }
        }
        ClearDataRow(stringResource(R.string.privacy_clear_sites)) {
            confirmAction = { scope.launch { app.siteSettingsRepository.clear() } }
        }
        ClearDataRow(stringResource(R.string.privacy_clear_all)) {
            confirmAction = {
                clearCookies()
                clearCache()
                scope.launch {
                    app.historyRepository.clear()
                    app.siteSettingsRepository.clear()
                }
            }
        }
    }

    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(stringResource(R.string.privacy_confirm_clear)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmAction = null
                    action()
                }) {
                    Text(stringResource(R.string.dialog_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) {
                    Text(stringResource(R.string.bookmarks_cancel))
                }
            },
        )
    }
}

@Composable
private fun ClearDataRow(label: String, onClear: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        modifier = Modifier.clickable(onClick = onClear),
    )
}

@Composable
fun SiteSettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val repo = app.siteSettingsRepository
    val all by repo.all.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<SiteSettingEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    SettingsColumn(title = stringResource(R.string.settings_site_settings)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { showAddDialog = true }) {
                Text(stringResource(R.string.site_add))
            }
        }
        if (all.isEmpty()) {
            PlaceholderScreen(text = stringResource(R.string.site_settings_none))
        } else {
            all.forEach { setting ->
                ListItem(
                    headlineContent = { Text(setting.host) },
                    supportingContent = { Text(describeSite(setting)) },
                    modifier = Modifier.clickable { editing = setting },
                )
            }
        }
    }

    editing?.let { setting ->
        var js by remember(setting) { mutableStateOf(setting.jsEnabled ?: true) }
        var desktop by remember(setting) { mutableStateOf(setting.desktopMode ?: false) }
        var adLevel by remember(setting) { mutableStateOf(setting.adLevel ?: "BASIC") }
        var safeBrowsing by remember(setting) { mutableStateOf(setting.safeBrowsing) }
        var thirdPartyCookies by remember(setting) { mutableStateOf(setting.thirdPartyCookies) }
        var location by remember(setting) { mutableStateOf(setting.location) }
        var camera by remember(setting) { mutableStateOf(setting.camera) }
        var microphone by remember(setting) { mutableStateOf(setting.microphone) }
        var notifications by remember(setting) { mutableStateOf(setting.notifications) }
        var popups by remember(setting) { mutableStateOf(setting.popups) }
        var autoplay by remember(setting) { mutableStateOf(setting.autoplay) }
        var httpsUpgrade by remember(setting) { mutableStateOf(setting.httpsUpgrade) }
        var clearOnExit by remember(setting) { mutableStateOf(setting.clearOnExit) }
        var antiTracking by remember(setting) { mutableStateOf(setting.antiTracking) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(setting.host) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(max = 480.dp),
                ) {
                    SettingSwitch(stringResource(R.string.site_js), js) { js = it }
                    SettingSwitch(stringResource(R.string.site_desktop), desktop) { desktop = it }
                    Text(
                        text = stringResource(R.string.site_safe_browsing),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TriStateChip(
                            label = stringResource(R.string.site_follow_global),
                            selected = safeBrowsing == null,
                            onClick = { safeBrowsing = null },
                        )
                        TriStateChip(
                            label = stringResource(R.string.site_enabled),
                            selected = safeBrowsing == true,
                            onClick = { safeBrowsing = true },
                        )
                        TriStateChip(
                            label = stringResource(R.string.site_disabled),
                            selected = safeBrowsing == false,
                            onClick = { safeBrowsing = false },
                        )
                    }
                    Text(
                        text = stringResource(R.string.site_cookies),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TriStateChip(
                            label = stringResource(R.string.site_follow_global),
                            selected = thirdPartyCookies == null,
                            onClick = { thirdPartyCookies = null },
                        )
                        TriStateChip(
                            label = stringResource(R.string.site_enabled),
                            selected = thirdPartyCookies == true,
                            onClick = { thirdPartyCookies = true },
                        )
                        TriStateChip(
                            label = stringResource(R.string.site_disabled),
                            selected = thirdPartyCookies == false,
                            onClick = { thirdPartyCookies = false },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("OFF", "BASIC", "STRICT").forEach { level ->
                            FilterChip(
                                selected = adLevel == level,
                                onClick = { adLevel = level },
                                label = { Text(adLabel(level)) },
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.site_location),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    SitePermissionTriState(location) { location = it }
                    Text(
                        text = stringResource(R.string.site_camera),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    SitePermissionTriState(camera) { camera = it }
                    Text(
                        text = stringResource(R.string.site_microphone),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    SitePermissionTriState(microphone) { microphone = it }
                    Text(
                        text = stringResource(R.string.site_notifications),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    SitePermissionTriState(notifications) { notifications = it }
                    Text(
                        text = stringResource(R.string.site_popups),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    SitePermissionTriState(popups) { popups = it }
                    Text(
                        text = stringResource(R.string.site_autoplay),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    SitePermissionTriState(autoplay) { autoplay = it }
                    Text(
                        text = stringResource(R.string.site_https),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    SitePermissionTriState(httpsUpgrade) { httpsUpgrade = it }
                    Text(
                        text = stringResource(R.string.site_anti_tracking),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    SitePermissionTriState(antiTracking) { antiTracking = it }
                    Text(
                        text = stringResource(R.string.site_clear_on_exit),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    SitePermissionTriState(clearOnExit) { clearOnExit = it }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repo.upsert(
                            setting.host,
                            jsEnabled = js,
                            adLevel = adLevel,
                            desktopMode = desktop,
                            safeBrowsing = safeBrowsing,
                            thirdPartyCookies = thirdPartyCookies,
                            location = location,
                            camera = camera,
                            microphone = microphone,
                            notifications = notifications,
                            popups = popups,
                            autoplay = autoplay,
                            httpsUpgrade = httpsUpgrade,
                            clearOnExit = clearOnExit,
                            antiTracking = antiTracking,
                        )
                    }
                    editing = null
                }) {
                    Text(stringResource(R.string.search_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) {
                    Text(stringResource(R.string.site_delete))
                }
            },
        )
    }

    if (showAddDialog) {
        TextInputDialog(
            title = stringResource(R.string.site_add),
            confirmLabel = stringResource(R.string.site_add),
            onConfirm = { host ->
                showAddDialog = false
                val normalized = host.trim().lowercase()
                if (normalized.isNotBlank()) {
                    scope.launch { repo.upsert(normalized) }
                }
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val version: String = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }.getOrDefault("?")
    }
    val webViewVersion: String = remember {
        runCatching {
            context.packageManager.getPackageInfo("com.google.android.webview", 0).versionName.orEmpty()
        }.getOrDefault("未知")
    }
    SettingsColumn(title = stringResource(R.string.settings_about)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.about_version)) },
            supportingContent = { Text("$version") },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.about_webview)) },
            supportingContent = { Text(webViewVersion) },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.about_license)) },
            supportingContent = {
                Text("Apache-2.0 · androidx · coil · coroutines · epublib(n/a) · jsoup · room")
            },
        )
    }
}

@Composable
fun SettingsColumn(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun uaLabel(mode: String): String = when (mode) {
    UaMode.DEFAULT -> stringResource(R.string.ua_default)
    UaMode.ANDROID -> stringResource(R.string.ua_android)
    UaMode.IPHONE -> stringResource(R.string.ua_iphone)
    UaMode.DESKTOP -> stringResource(R.string.ua_desktop)
    else -> stringResource(R.string.ua_custom)
}

@Composable
fun adLabel(level: String): String = when (level) {
    "OFF" -> stringResource(R.string.ad_off)
    "BASIC" -> stringResource(R.string.ad_basic)
    else -> stringResource(R.string.ad_strict)
}

@Composable
private fun describeSite(setting: SiteSettingEntity): String = buildString {
    append("JS: ${if (setting.jsEnabled != false) "开" else "关"}")
    append(" · 广告: ${adLabel(setting.adLevel ?: "BASIC")}")
    append(" · 桌面: ${if (setting.desktopMode == true) "是" else "否"}")
    append(" · 安全浏览: ${triStateLabel(setting.safeBrowsing)}")
    append(" · 第三方Cookie: ${triStateLabel(setting.thirdPartyCookies)}")
    append(" · 位置: ${triStateLabel(setting.location)}")
    append(" · 摄像头: ${triStateLabel(setting.camera)}")
    append(" · 弹窗: ${triStateLabel(setting.popups)}")
    append(" · HTTPS: ${triStateLabel(setting.httpsUpgrade)}")
    append(" · 反追踪: ${triStateLabel(setting.antiTracking)}")
}

@Composable
fun TriStateChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
fun triStateLabel(value: Boolean?): String = when (value) {
    null -> stringResource(R.string.site_follow_global)
    true -> stringResource(R.string.site_enabled)
    false -> stringResource(R.string.site_disabled)
}

@Composable
fun SitePermissionTriState(
    value: Boolean?,
    onChange: (Boolean?) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TriStateChip(
            label = stringResource(R.string.site_follow_global),
            selected = value == null,
            onClick = { onChange(null) },
        )
        TriStateChip(
            label = stringResource(R.string.site_enabled),
            selected = value == true,
            onClick = { onChange(true) },
        )
        TriStateChip(
            label = stringResource(R.string.site_disabled),
            selected = value == false,
            onClick = { onChange(false) },
        )
    }
}
