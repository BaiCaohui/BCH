package com.baicaohui.lightweb.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.baicaohui.lightweb.R
import com.baicaohui.lightweb.ui.navigation.BchRoute

@Composable
fun SettingsScreen(onNavigate: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            text = stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )
        listOf(
            R.string.settings_appearance to BchRoute.APPEARANCE.route,
            R.string.settings_home to BchRoute.HOME_EDIT.route,
            R.string.settings_toolbar to BchRoute.TOOLBAR_SETTINGS.route,
            R.string.settings_search_engine to BchRoute.SEARCH_ENGINE.route,
            R.string.settings_browsing to BchRoute.BROWSE_SETTINGS.route,
            R.string.settings_privacy to BchRoute.PRIVACY.route,
            R.string.settings_site_settings to BchRoute.SITE_SETTINGS.route,
            R.string.settings_adblock to BchRoute.ADBLOCK.route,
            R.string.settings_site_data to BchRoute.SITE_DATA.route,
            R.string.settings_about to BchRoute.ABOUT.route,
        ).forEach { (labelRes, route) ->
            ListItem(
                headlineContent = { Text(stringResource(labelRes)) },
                modifier = Modifier.clickable { onNavigate(route) },
            )
        }
    }
}
