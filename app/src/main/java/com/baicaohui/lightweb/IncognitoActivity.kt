package com.baicaohui.lightweb

import android.os.Bundle
import android.os.Process
import android.webkit.CookieManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baicaohui.lightweb.ui.IncognitoAppRoot
import com.baicaohui.lightweb.ui.theme.BchTheme
import com.baicaohui.lightweb.ui.theme.ThemeConfig

/**
 * 无痕浏览器界面，运行在独立进程 `:incognito` 中。
 * 该进程的 WebView 使用独立数据目录（Cookie/网站数据与普通模式完全隔离），
 * 退出（onDestroy）时销毁全部无痕 WebView 并删除该数据目录后结束进程。
 */
class IncognitoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 若进程被系统复用，这里兜底清除上次会话残留数据（此时尚无新的 WebView）。
        (application as BchApp).purgeIncognitoData()
        enableEdgeToEdge()
        setContent {
            val app = application as BchApp
            val themeConfig by app.themePrefs.config.collectAsStateWithLifecycle(
                initialValue = ThemeConfig.DEFAULT,
            )
            BchTheme(config = themeConfig) {
                IncognitoAppRoot(initialUrl = intent?.getStringExtra(EXTRA_URL))
            }
        }
    }

    override fun onDestroy() {
        val app = application as BchApp
        runCatching {
            app.webViewStore.destroyRemoved(emptySet())
            CookieManager.getInstance().flush()
        }
        app.purgeIncognitoData()
        super.onDestroy()
        // 无痕会话结束后立即结束进程，保证下次进入是全新进程并重新清除数据目录。
        Process.killProcess(Process.myPid())
    }

    companion object {
        const val EXTRA_URL = "extra_url"
    }
}
