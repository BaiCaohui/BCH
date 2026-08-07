package com.baicaohui.lightweb.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.CookieManager
import com.baicaohui.lightweb.data.db.SiteSettingEntity
import com.baicaohui.lightweb.data.prefs.BrowserPrefs
import com.baicaohui.lightweb.data.prefs.UaMode

class BrowserWebView(
    context: Context,
    private val callbacks: WebCallbacks,
    private val adBlocker: AdBlocker,
    private val adLevel: () -> AdLevel = { AdLevel.BASIC },
    private val trackerBlocker: TrackerBlocker = TrackerBlocker(emptySet()),
    private val customRules: () -> List<String> = { emptyList() },
    private val blockTrackers: () -> Boolean = { false },
    private val httpsMode: () -> String = { HttpsMode.PREFER },
) : WebView(context) {

    /** 用户触摸/滑动网页时的回调（用于编辑态失焦退出）。 */
    var onUserInteract: (() -> Unit)? = null

    var onLongPressLink: ((url: String, x: Float, y: Float) -> Unit)? = null

    var onLongPressImage: ((url: String) -> Unit)? = null

    var onTextSelection: ((x: Float, y: Float) -> Unit)? = null

    val resourceSniffing = ResourceSniffController()

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var longPressHandledByListener = false

    /** 最近一次实际应用到本 WebView 的设置指纹，用于检测 UA/站点设置变化后是否需要重载。 */
    var appliedSettingsKey: String? = null
        private set

    /** 当前生效的广告拦截级别（站点优先，回退全局），供 shouldInterceptRequest 使用。 */
    private var requestAdLevel: AdLevel = AdLevel.OFF

    /** 当前生效的反追踪开关（站点优先，回退全局）。 */
    private var requestTrackerBlocking: Boolean = false

    /** 当前生效的 HTTPS 模式（站点优先，回退全局）。 */
    private var requestHttpsMode: String = HttpsMode.PREFER

    /** 正在升级到 HTTPS 的原始 HTTP URL，防止重复升级形成循环。 */
    var upgradingHttpUrl: String? = null

    init {
        // Compose AndroidView 不会把高度传给 WebView 的 LayoutParams，导致 CSS vh/vmin 视口高度为 0；
        // 显式设置 MATCH_PARENT 让 Chromium 拿到真实视口尺寸。
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
            settings.apply {
                userAgentString = ANDROID_UA
                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true)
                domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            allowUniversalAccessFromFileURLs = false
            allowFileAccessFromFileURLs = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            setSafeBrowsingEnabled(true)
            saveFormData = false
        }
        setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
            callbacks.onDownloadStart(url, userAgent ?: "", contentDisposition, mimetype)
        }
        requestAdLevel = adLevel()
        webViewClient = BchWebViewClient(
            adBlocker = adBlocker,
            adLevel = { requestAdLevel },
            customRules = customRules,
            trackerBlocker = trackerBlocker,
            blockTrackers = { requestTrackerBlocking },
            httpsMode = { requestHttpsMode },
            callbacks = callbacks,
            canOpenExternal = { url ->
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .resolveActivity(context.packageManager) != null
            },
            onResourceRequest = { url -> resourceSniffing.add(url, url) },
        )
        webChromeClient = BchWebChromeClient(callbacks)
        setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> onUserInteract?.invoke()
            }
            false
        }
        isLongClickable = true
        setOnLongClickListener {
            val hit = hitTestResult
            when (hit.type) {
                WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                    hit.extra?.takeIf { it.isNotBlank() }?.let {
                        onLongPressLink?.invoke(it, lastTouchX, lastTouchY)
                    }
                    true
                }
                WebView.HitTestResult.IMAGE_TYPE,
                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    hit.extra?.takeIf { it.isNotBlank() }?.let {
                        onLongPressImage?.invoke(it)
                    }
                    true
                }
                WebView.HitTestResult.EDIT_TEXT_TYPE -> false
                else -> {
                    longPressHandledByListener = true
                    onTextSelection?.invoke(lastTouchX, lastTouchY)
                    true
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            lastTouchX = event.x
            lastTouchY = event.y
            longPressHandledByListener = false
        }
        return super.onTouchEvent(event)
    }

    fun applySiteSettings(url: String, prefs: BrowserPrefs, site: SiteSettingEntity?) {
        val resolved = SiteSettingsPolicy.resolve(prefs, site)
        requestAdLevel = resolved.adLevel
        requestTrackerBlocking = resolved.antiTracking
        requestHttpsMode = resolved.httpsMode
        settings.userAgentString = uaFor(prefs, site)
        settings.javaScriptEnabled = resolved.javaScriptEnabled
        settings.setSafeBrowsingEnabled(resolved.safeBrowsing)
        settings.mediaPlaybackRequiresUserGesture = !resolved.autoplay
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, resolved.thirdPartyCookies)
        appliedSettingsKey = settingsKey(prefs, site)
    }

    /** 用户明确选择“仍然访问”时，绕过 HTTPS 升级加载指定 HTTP URL。 */
    fun bypassHttpsUpgrade(url: String) {
        upgradingHttpUrl = url
        loadUrl(url, mapOf("x-requested-with" to ""))
    }

    fun startResourceSniffing() = resourceSniffing.start()

    fun stopResourceSniffing() = resourceSniffing.stop()

    fun clearSniffedResources() = resourceSniffing.clear()

    fun scanPageResources() {
        val base = url ?: return
        evaluateJavascript(ResourceSniffer.domScanScript()) { raw ->
            resourceSniffing.addDomResult(raw, base)
        }
    }

    override fun startActionMode(callback: ActionMode.Callback?): ActionMode? =
        interceptActionMode(callback, ActionMode.TYPE_FLOATING)

    override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? =
        interceptActionMode(callback, type)

    private fun interceptActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
        if (callback == null) return null
        if (type != ActionMode.TYPE_FLOATING) return super.startActionMode(callback, type)
        if (hitTestResult.type == WebView.HitTestResult.UNKNOWN_TYPE) {
            if (!longPressHandledByListener) {
                onTextSelection?.invoke(lastTouchX, lastTouchY)
            }
            return super.startActionMode(SilentActionModeCallback(callback, this), type)
        }
        return super.startActionMode(callback, type)
    }

    private class SilentActionModeCallback(
        private val delegate: ActionMode.Callback,
        private val view: BrowserWebView,
    ) : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            delegate.onCreateActionMode(mode, menu)
            clearMenu(mode)
            view.postDelayed({ clearMenu(mode) }, 150)
            view.postDelayed({ clearMenu(mode) }, 500)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            clearMenu(mode)
            return delegate.onPrepareActionMode(mode, menu)
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean =
            delegate.onActionItemClicked(mode, item)

        override fun onDestroyActionMode(mode: ActionMode) = delegate.onDestroyActionMode(mode)

        private fun clearMenu(mode: ActionMode) {
            mode.menu.clear()
        }
    }

    companion object {
        const val ANDROID_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

        const val IPHONE_UA =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"

        val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

        fun uaFor(prefs: BrowserPrefs, site: SiteSettingEntity?): String {
            val desktop = site?.desktopMode ?: (prefs.uaMode == UaMode.DESKTOP)
            return when {
                desktop -> DESKTOP_UA
                prefs.uaMode == UaMode.CUSTOM && prefs.customUa.isNotBlank() -> prefs.customUa
                prefs.uaMode == UaMode.IPHONE -> IPHONE_UA
                else -> ANDROID_UA
            }
        }

        fun settingsKey(prefs: BrowserPrefs, site: SiteSettingEntity?): String =
            listOf(
                uaFor(prefs, site),
                SiteSettingsPolicy.resolve(prefs, site).let {
                    listOf(
                        it.javaScriptEnabled.toString(),
                        it.adLevel.name,
                        it.safeBrowsing.toString(),
                        it.thirdPartyCookies.toString(),
                        it.antiTracking.toString(),
                        it.httpsMode,
                        it.autoplay.toString(),
                    )
                }.joinToString("|"),
            ).joinToString("|")
    }
}
