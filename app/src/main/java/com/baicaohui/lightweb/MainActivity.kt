package com.baicaohui.lightweb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baicaohui.lightweb.ui.BchAppRoot
import com.baicaohui.lightweb.ui.theme.BchTheme
import com.baicaohui.lightweb.ui.theme.ThemeConfig

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as BchApp
            val themeConfig by app.themePrefs.config.collectAsStateWithLifecycle(
                initialValue = ThemeConfig.DEFAULT,
            )
            BchTheme(config = themeConfig) {
                BchAppRoot()
            }
        }
    }
}
