package com.baicaohui.lightweb.ui.browser

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.ui.navigation.BchIcons

@Composable
fun BrowserToolbar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onTabs: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, enabled = canGoBack) {
            Icon(BchIcons.Back, contentDescription = stringResource(R.string.action_back))
        }
        IconButton(onClick = onForward, enabled = canGoForward) {
            Icon(BchIcons.Forward, contentDescription = stringResource(R.string.action_forward))
        }
        IconButton(onClick = onReload) {
            Icon(BchIcons.Refresh, contentDescription = stringResource(R.string.action_reload))
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onTabs) {
            Text(
                text = tabCount.toString(),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
