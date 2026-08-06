package com.baicaohui.lightweb.ui.downloads

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.data.prefs.BrowserPrefs
import com.baicaohui.lightweb.data.prefs.DownloadLocation
import com.baicaohui.lightweb.data.prefs.DownloadMode

/** 下载设置弹窗（下载管理界面入口）。 */
@Composable
fun DownloadSettingsDialog(
    prefs: BrowserPrefs,
    onUpdate: (transform: (BrowserPrefs) -> BrowserPrefs) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.downloads_settings)) },
        text = {
            DownloadSettingsSection(
                prefs = prefs,
                onUpdate = onUpdate,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.download_settings_done))
            }
        },
    )
}

/** 下载方式 + 下载位置设置（下载管理界面与设置 → 浏览共用）。 */
@Composable
fun DownloadSettingsSection(
    prefs: BrowserPrefs,
    onUpdate: (transform: (BrowserPrefs) -> BrowserPrefs) -> Unit,
) {
    val context = LocalContext.current
    Column {
        Text(
            text = stringResource(R.string.download_mode),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = prefs.downloadMode == DownloadMode.APP,
                onClick = { onUpdate { it.copy(downloadMode = DownloadMode.APP) } },
                label = { Text(stringResource(R.string.download_mode_app)) },
            )
            FilterChip(
                selected = prefs.downloadMode == DownloadMode.SYSTEM,
                onClick = { onUpdate { it.copy(downloadMode = DownloadMode.SYSTEM) } },
                label = { Text(stringResource(R.string.download_mode_system)) },
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.download_location),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = prefs.downloadLocation == DownloadLocation.APP,
                onClick = { onUpdate { it.copy(downloadLocation = DownloadLocation.APP) } },
                label = { Text(stringResource(R.string.download_location_app)) },
            )
            FilterChip(
                selected = prefs.downloadLocation == DownloadLocation.PUBLIC,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        onUpdate { it.copy(downloadLocation = DownloadLocation.PUBLIC) }
                    } else {
                        Toast.makeText(
                            context,
                            R.string.download_location_public_unsupported,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
                label = { Text(stringResource(R.string.download_location_public)) },
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.download_location_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
