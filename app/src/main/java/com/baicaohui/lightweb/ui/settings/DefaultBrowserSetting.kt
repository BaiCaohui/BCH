package com.baicaohui.lightweb.ui.settings

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.browser.DefaultBrowser
import com.baicaohui.lightweb.browser.DefaultBrowserAction

@Composable
fun DefaultBrowserSettingItem() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var status by remember { mutableStateOf<Boolean?>(null) }

    fun refresh() {
        status = DefaultBrowser.isDefault(
            apiLevel = Build.VERSION.SDK_INT,
            ownPackage = context.packageName,
            isRoleHeld = {
                Build.VERSION.SDK_INT >= 30 &&
                    context.getSystemService(RoleManager::class.java)
                        ?.isRoleHeld(RoleManager.ROLE_BROWSER) == true
            },
            resolvedWebPackage = { resolveWebPackage(context) },
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { refresh() }

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_default_browser)) },
        supportingContent = {
            Text(
                stringResource(
                    when (status) {
                        true -> R.string.settings_default_browser_status_default
                        false -> R.string.settings_default_browser_status_not_default
                        null -> R.string.settings_default_browser_status_checking
                    },
                ),
            )
        },
        modifier = Modifier.clickable {
            val intent = defaultBrowserIntent(context)
            if (intent != null) {
                runCatching { launcher.launch(intent) }.onFailure {
                    openDefaultAppsSettingsIntent()?.let { fallback ->
                        runCatching { launcher.launch(fallback) }
                    }
                }
            }
        },
    )
}

private fun defaultBrowserIntent(context: Context): Intent? =
    when (DefaultBrowser.actionFor(Build.VERSION.SDK_INT)) {
        DefaultBrowserAction.REQUEST_ROLE -> {
            context.getSystemService(RoleManager::class.java)
                ?.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
                ?: openDefaultAppsSettingsIntent()
        }

        DefaultBrowserAction.OPEN_SETTINGS -> openDefaultAppsSettingsIntent()
    }

private fun openDefaultAppsSettingsIntent(): Intent? = runCatching {
    Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
}.getOrNull()

private fun resolveWebPackage(context: Context): String? = runCatching {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com"))
        .addCategory(Intent.CATEGORY_BROWSABLE)
    context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
}.getOrNull()
