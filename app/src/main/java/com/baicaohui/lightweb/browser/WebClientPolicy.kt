package com.baicaohui.lightweb.browser

import android.graphics.Bitmap
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.os.Build
import android.net.http.SslError
import android.os.Message
import android.util.Log
import android.webkit.PermissionRequest
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

interface WebCallbacks {
    fun onProgress(progress: Int)
    fun onPageStarted(url: String)
    fun onPageFinished(url: String)
    fun onTitleChanged(title: String)
    fun onIconChanged(icon: Bitmap)
    fun onDownloadStart(url: String, userAgent: String, contentDisposition: String?, mimeType: String?)
    fun onPermissionRequest(request: PermissionRequest)
    fun onExternalScheme(url: String)
    fun onMainFrameError(failingUrl: String, code: Int, description: String)
    fun onSslError(url: String, handler: SslErrorHandler)
    fun onSafeBrowsingHit(url: String, threatType: Int, handler: SafeBrowsingResponse)
    fun onGeolocationPrompt(origin: String, callback: GeolocationPermissions.Callback)
    fun onPopup(url: String)
    fun onHttpsBlocked(url: String)
}

class BchWebViewClient(
    private val adBlocker: AdBlocker,
    private val adLevel: () -> AdLevel,
    private val customRules: () -> List<String>,
    private val trackerBlocker: TrackerBlocker,
    private val blockTrackers: () -> Boolean,
    private val httpsMode: () -> String,
    private val callbacks: WebCallbacks,
    private val canOpenExternal: (String) -> Boolean,
    private val onResourceRequest: (String) -> Unit = {},
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        if (UrlSecurity.isSafeUrl(url)) return false
        // 自动触发的深链（如 B 站视频页的 bilibili:// h5awaken）静默取消，
        // 让网页继续留在当前页；只有用户主动点击且系统能处理时才弹确认框。
        if (ExternalSchemePolicy.shouldPrompt(url, request.hasGesture(), canOpenExternal(url))) {
            callbacks.onExternalScheme(url)
        }
        return true
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        if (request.isForMainFrame) return null
        val url = request.url.toString()
        onResourceRequest(url)
        val blockedByAds = adBlocker.isBlocked(url, adLevel(), customRules())
        val blockedByTracker = blockTrackers() && trackerBlocker.isTracker(url)
        return if (blockedByAds || blockedByTracker) {
            Log.d("BchBlock", "blocked url=$url ads=$blockedByAds tracker=$blockedByTracker")
            WebResourceResponse("text/plain", "utf-8", null)
        } else {
            null
        }
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        val current = url ?: return
        val mode = httpsMode()
        if (HttpsPolicy.shouldUpgrade(current, mode)) {
            val browserView = view as? BrowserWebView
            if (browserView != null && browserView.upgradingHttpUrl == current) {
                // 已回退/用户已确认：允许加载 http，清除升级守卫。
                browserView.upgradingHttpUrl = null
            } else {
                browserView?.upgradingHttpUrl = current
                view.loadUrl(HttpsPolicy.upgrade(current), mapOf("x-requested-with" to ""))
                return
            }
        } else if (view is BrowserWebView) {
            view.upgradingHttpUrl = null
        }
        callbacks.onPageStarted(current)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        url?.let { callbacks.onPageFinished(it) }
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        if (request.isForMainFrame) {
            val url = request.url.toString()
            val mode = httpsMode()
            if (HttpsPolicy.shouldFallback(url, mode)) {
                val browserView = view as? BrowserWebView
                if (browserView != null) {
                    val http = HttpsPolicy.toHttp(url)
                    // 标记为“已回退”，onPageStarted 不会再次升级。
                    browserView.upgradingHttpUrl = http
                    view.post {
                        view.loadUrl(http, mapOf("x-requested-with" to ""))
                    }
                    return
                }
            }
            if (HttpsPolicy.isHttps(url) && mode == HttpsMode.STRICT) {
                callbacks.onHttpsBlocked(url)
                return
            }
            callbacks.onMainFrameError(url, error.errorCode, error.description.toString())
        }
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        callbacks.onSslError(error.url, handler)
    }

    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.O_MR1)
    override fun onSafeBrowsingHit(
        view: WebView,
        request: WebResourceRequest,
        threatType: Int,
        callback: SafeBrowsingResponse,
    ) {
        callbacks.onSafeBrowsingHit(request.url.toString(), threatType, callback)
    }
}

class BchWebChromeClient(private val callbacks: WebCallbacks) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        callbacks.onProgress(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        title?.let { callbacks.onTitleChanged(it) }
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        callbacks.onIconChanged(icon)
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        callbacks.onPermissionRequest(request)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissions.Callback,
    ) {
        callbacks.onGeolocationPrompt(origin, callback)
    }

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message,
    ): Boolean {
        val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
        // WebViewTransport.getUrl() 是隐藏 API，用临时 WebView 接住导航以捕获弹窗 URL，
        // 捕获后立即销毁；BrowserScreen 按权限策略决定是否在新标签打开。
        val temp = WebView(view.context)
        temp.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(tempView: WebView, request: WebResourceRequest): Boolean {
                callbacks.onPopup(request.url.toString())
                runCatching {
                    tempView.stopLoading()
                    tempView.destroy()
                }
                return true
            }
        }
        transport.setWebView(temp)
        resultMsg.sendToTarget()
        return true
    }
}
