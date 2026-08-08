package com.baicaohui.lightweb.ui.settings

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baicaohui.lightweb.BchApp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.browser.CookieDataManager
import com.baicaohui.lightweb.browser.UrlSecurity
import com.baicaohui.lightweb.data.prefs.BrowserPrefs
import com.baicaohui.lightweb.ui.components.PlaceholderScreen
import kotlinx.coroutines.launch

@Composable
fun AdBlockSettingsScreen() {
    val app = LocalContext.current.applicationContext as BchApp
    val store = app.browserPrefsStore
    val prefs by store.prefs.collectAsStateWithLifecycle(initialValue = BrowserPrefs.DEFAULT)
    val markedAds by app.markedAdRepository.all.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var newRule by remember { mutableStateOf("") }

    SettingsColumn(title = stringResource(R.string.settings_adblock)) {
        SectionTitle(stringResource(R.string.browse_ad_level))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("OFF", "BASIC", "STRICT").forEach { level ->
                FilterChip(
                    selected = prefs.adLevel == level,
                    onClick = { scope.launch { store.update { it.copy(adLevel = level) } } },
                    label = { Text(adLabel(level)) },
                )
            }
        }
        Text(
            text = stringResource(R.string.adblock_builtin_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(12.dp))
        SectionTitle(stringResource(R.string.marked_ads_title))
        if (markedAds.isEmpty()) {
            Text(
                text = stringResource(R.string.marked_ads_empty),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            markedAds.forEach { ad ->
                ListItem(
                    headlineContent = { Text(ad.host) },
                    supportingContent = {
                        Text(
                            text = buildString {
                                append(ad.selector)
                                if (ad.adHosts.isNotBlank()) append(" · ${ad.adHosts}")
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = ad.enabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        app.markedAdRepository.setEnabled(ad, enabled)
                                    }
                                },
                            )
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        app.markedAdRepository.delete(ad)
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.marked_ads_delete),
                                )
                            }
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SectionTitle(stringResource(R.string.adblock_custom_rules))
        Text(
            text = stringResource(R.string.adblock_custom_rules_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (prefs.customAdRules.isEmpty()) {
            Text(
                text = stringResource(R.string.adblock_rules_none),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            prefs.customAdRules.forEach { rule ->
                ListItem(
                    headlineContent = { Text(rule) },
                    trailingContent = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    store.update {
                                        it.copy(customAdRules = it.customAdRules.filterNot { r -> r == rule })
                                    }
                                }
                            },
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.adblock_rule_delete),
                            )
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = newRule,
            onValueChange = { newRule = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.adblock_rule_hint)) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = {
                    val rule = newRule.trim()
                    if (rule.isNotEmpty()) {
                        scope.launch {
                            store.update { it.copy(customAdRules = it.customAdRules + rule) }
                        }
                        newRule = ""
                    }
                },
            ) {
                Text(stringResource(R.string.adblock_rule_add))
            }
        }
    }
}

@Composable
fun SiteDataScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as BchApp
    val store = app.browserPrefsStore
    val prefs by store.prefs.collectAsStateWithLifecycle(initialValue = BrowserPrefs.DEFAULT)
    val scope = rememberCoroutineScope()
    var storageOrigins by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedHost by remember { mutableStateOf<String?>(null) }
    var confirmClearAll by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val origins = mutableListOf<String>()
        runCatching {
            WebStorage.getInstance().getOrigins { map ->
                origins += map.keys.map { it.toString() }
            }
        }
        storageOrigins = origins
    }

    val hosts = (prefs.trackedHosts +
        storageOrigins.mapNotNull { UrlSecurity.extractHost(it) })
        .distinct()
        .sorted()

    fun cookieHeader(host: String): String =
        listOf("https://$host/", "http://$host/")
            .mapNotNull { CookieManager.getInstance().getCookie(it) }
            .joinToString("; ")

    fun clearSiteCookies(host: String) {
        val header = cookieHeader(host)
        CookieDataManager.expiredSetCookieEntries(host, header)
            .forEach { (url, value) ->
                runCatching { CookieManager.getInstance().setCookie(url, value) }
            }
    }

    SettingsColumn(title = stringResource(R.string.settings_site_data)) {
        if (hosts.isEmpty()) {
            PlaceholderScreen(text = stringResource(R.string.site_data_none))
        } else {
            hosts.forEach { host ->
                val names = remember(host, refreshKey) {
                    CookieDataManager.cookieNames(cookieHeader(host))
                }
                ListItem(
                    headlineContent = { Text(host) },
                    supportingContent = {
                        Text(stringResource(R.string.site_data_cookies_count, names.size))
                    },
                    modifier = Modifier.clickable { selectedHost = host },
                )
            }
        }
    }

    selectedHost?.let { host ->
        val names = remember(host, refreshKey) {
            CookieDataManager.cookieNames(cookieHeader(host))
        }
        var clearOnExit by remember(host) { mutableStateOf<Boolean?>(null) }
        LaunchedEffect(host) {
            clearOnExit = app.siteSettingsRepository.get(host)?.clearOnExit
        }
        AlertDialog(
            onDismissRequest = { selectedHost = null },
            title = { Text(stringResource(R.string.site_data_detail_title)) },
            text = {
                Column {
                    Text(host, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.site_data_cookies_count, names.size),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (names.isNotEmpty()) {
                        Text(
                            text = stringResource(
                                R.string.site_data_cookie_names,
                                names.take(30).joinToString(", "),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.site_clear_on_exit),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    SitePermissionTriState(clearOnExit) { value ->
                        clearOnExit = value
                        scope.launch {
                            val existing = app.siteSettingsRepository.get(host)
                            app.siteSettingsRepository.upsert(
                                host,
                                jsEnabled = existing?.jsEnabled,
                                adLevel = existing?.adLevel,
                                desktopMode = existing?.desktopMode,
                                safeBrowsing = existing?.safeBrowsing,
                                thirdPartyCookies = existing?.thirdPartyCookies,
                                location = existing?.location,
                                camera = existing?.camera,
                                microphone = existing?.microphone,
                                notifications = existing?.notifications,
                                popups = existing?.popups,
                                autoplay = existing?.autoplay,
                                httpsUpgrade = existing?.httpsUpgrade,
                                antiTracking = existing?.antiTracking,
                                clearOnExit = value,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            selectedHost = null
                            confirmClearAll = host
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.site_data_delete_all),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearSiteCookies(host)
                        refreshKey++
                        Toast.makeText(context, R.string.site_data_deleted, Toast.LENGTH_SHORT).show()
                    },
                    enabled = names.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.site_data_delete_cookies))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedHost = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    confirmClearAll?.let { host ->
        AlertDialog(
            onDismissRequest = { confirmClearAll = null },
            title = { Text(stringResource(R.string.site_data_delete_all)) },
            text = { Text(stringResource(R.string.site_data_confirm_delete)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearAll = null
                        selectedHost = null
                        scope.launch {
                            clearSiteCookies(host)
                            runCatching { WebStorage.getInstance().deleteOrigin("https://$host/") }
                            runCatching { WebStorage.getInstance().deleteOrigin("http://$host/") }
                            app.readerCacheRepository.deleteByHost(host)
                            app.historyRepository.deleteByHost(host)
                            app.siteSettingsRepository.get(host)?.let {
                                app.siteSettingsRepository.delete(it)
                            }
                            refreshKey++
                            Toast.makeText(context, R.string.site_data_deleted, Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    Text(stringResource(R.string.dialog_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}
