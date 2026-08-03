package com.baicaohui.lightweb.ui.settings

import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import com.baicaohui.lightweb.data.prefs.BrowserPrefs
import com.baicaohui.lightweb.data.prefs.ToolbarPosition
import com.baicaohui.lightweb.data.prefs.UaMode
import com.baicaohui.lightweb.ui.components.PlaceholderScreen
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(UaMode.DEFAULT, UaMode.DESKTOP, UaMode.CUSTOM).forEach { mode ->
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
    }

    SettingsColumn(title = stringResource(R.string.settings_privacy)) {
        SettingSwitch(
            stringResource(R.string.privacy_safe_browsing),
            prefs.safeBrowsing,
        ) { enabled -> update { it.copy(safeBrowsing = enabled) } }
        SettingSwitch(
            stringResource(R.string.privacy_third_party_cookies),
            prefs.thirdPartyCookies,
        ) { enabled -> update { it.copy(thirdPartyCookies = enabled) } }

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
    val app = LocalContext.current.applicationContext as BchApp
    val repo = app.siteSettingsRepository
    val all by repo.all.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<SiteSettingEntity?>(null) }

    SettingsColumn(title = stringResource(R.string.settings_site_settings)) {
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
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(setting.host) },
            text = {
                Column {
                    SettingSwitch(stringResource(R.string.site_js), js) { js = it }
                    SettingSwitch(stringResource(R.string.site_desktop), desktop) { desktop = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("OFF", "BASIC", "STRICT").forEach { level ->
                            FilterChip(
                                selected = adLevel == level,
                                onClick = { adLevel = level },
                                label = { Text(adLabel(level)) },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repo.upsert(setting.host, jsEnabled = js, adLevel = adLevel, desktopMode = desktop)
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
private fun SettingsColumn(title: String, content: @Composable () -> Unit) {
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
    UaMode.DESKTOP -> stringResource(R.string.ua_desktop)
    else -> stringResource(R.string.ua_custom)
}

@Composable
private fun adLabel(level: String): String = when (level) {
    "OFF" -> stringResource(R.string.ad_off)
    "BASIC" -> stringResource(R.string.ad_basic)
    else -> stringResource(R.string.ad_strict)
}

@Composable
private fun describeSite(setting: SiteSettingEntity): String = buildString {
    append("JS: ${if (setting.jsEnabled != false) "开" else "关"}")
    append(" · 广告: ${adLabel(setting.adLevel ?: "BASIC")}")
    append(" · 桌面: ${if (setting.desktopMode == true) "是" else "否"}")
}
