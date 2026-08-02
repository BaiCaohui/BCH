package com.baicaohui.lightweb.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.ui.navigation.BchRoute

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name_full),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = { onNavigate(BchRoute.BROWSER.route) }) {
            Text(stringResource(R.string.action_open_browser))
        }
    }
}
