package com.baicaohui.lightweb.browser

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
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
    fun onDownloadStart(url: String, userAgent: String, contentDisposition: String?, mimeType: String?)
    fun onPermissionRequest(request: PermissionRequest)
    fun onExternalScheme(url: String)
    fun onMainFrameError(failingUrl: String, code: Int, description: String)
    fun onSslError(url: String, handler: SslErrorHandler)
}

class BchWebViewClient(
    private val adBlocker: AdBlocker,
    private val adLevel: () -> AdLevel,
    private val callbacks: WebCallbacks,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        return if (UrlSecurity.isSafeUrl(url)) {
            false
        } else {
            callbacks.onExternalScheme(url)
            true
        }
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? =
        if (adBlocker.isBlocked(request.url.toString(), adLevel())) {
            WebResourceResponse("text/plain", "utf-8", null)
        } else {
            null
        }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        url?.let { callbacks.onPageStarted(it) }
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
            callbacks.onMainFrameError(request.url.toString(), error.errorCode, error.description.toString())
        }
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        callbacks.onSslError(error.url, handler)
    }
}

class BchWebChromeClient(private val callbacks: WebCallbacks) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        callbacks.onProgress(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        title?.let { callbacks.onTitleChanged(it) }
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        callbacks.onPermissionRequest(request)
    }
}
