package com.baicaohui.lightweb.ui.browser

import android.webkit.SslErrorHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baicaohui.lightweb.browser.Tab
import com.baicaohui.lightweb.browser.TabManager
import com.baicaohui.lightweb.browser.TabStatus
import com.baicaohui.lightweb.browser.UrlSecurity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class BrowserViewModel(private val tabManager: TabManager) : ViewModel() {

    val tabs: StateFlow<List<Tab>> = tabManager.tabs
    val currentId: StateFlow<Long?> = tabManager.currentId

    private val _events = MutableSharedFlow<BrowserEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<BrowserEvent> = _events.asSharedFlow()

    fun newTab(url: String = ""): Tab = tabManager.newTab(url)

    fun closeTab(id: Long) = tabManager.closeTab(id)

    fun selectTab(id: Long) = tabManager.select(id)

    fun submitInput(raw: String, searchTemplate: String) {
        val input = raw.trim()
        if (input.isEmpty()) return
        val normalized = UrlSecurity.normalize(input)
        val url = if (UrlSecurity.isSafeUrl(normalized)) {
            normalized
        } else {
            UrlSecurity.toSearchUrl(input, searchTemplate)
        }
        val existing = tabManager.currentId.value?.takeIf { id ->
            tabManager.tabs.value.any { it.id == id }
        }
        val id = existing ?: tabManager.newTab().id
        tabManager.update(id) { it.copy(url = url, status = TabStatus.LOADING, progress = 5) }
        emit(BrowserEvent.Navigate(url))
    }

    fun retry() {
        updateCurrent { it.copy(status = TabStatus.LOADING, progress = 10) }
        emit(BrowserEvent.Reload)
    }

    fun onProgress(progress: Int) = updateCurrent { it.copy(progress = progress) }

    fun onPageStarted(url: String) = updateCurrent {
        it.copy(url = url, status = TabStatus.LOADING, progress = 10)
    }

    fun onPageFinished(url: String) = updateCurrent {
        it.copy(url = url, status = TabStatus.READY, progress = 100)
    }

    fun onTitle(title: String) = updateCurrent { it.copy(title = title) }

    fun onError(failingUrl: String) = updateCurrent {
        it.copy(status = TabStatus.ERROR, progress = 100)
    }

    fun onExternalScheme(url: String) = emit(BrowserEvent.ExternalScheme(url))

    fun onPermissionRequest(request: android.webkit.PermissionRequest) =
        emit(BrowserEvent.PermissionRequest(request))

    fun onSslError(url: String, handler: SslErrorHandler) =
        emit(BrowserEvent.SslError(url, handler))

    fun onDownload(url: String, userAgent: String, mimeType: String?) =
        emit(BrowserEvent.Download(url, userAgent, mimeType))

    private fun updateCurrent(transform: (Tab) -> Tab) {
        tabManager.currentId.value?.let { tabManager.update(it, transform) }
    }

    private fun emit(event: BrowserEvent) {
        viewModelScope.launch { _events.emit(event) }
    }
}

sealed interface BrowserEvent {
    data object Reload : BrowserEvent
    data class Navigate(val url: String) : BrowserEvent
    data class ExternalScheme(val url: String) : BrowserEvent
    data class PermissionRequest(val request: android.webkit.PermissionRequest) : BrowserEvent
    data class SslError(val url: String, val handler: SslErrorHandler) : BrowserEvent
    data class Download(val url: String, val userAgent: String, val mimeType: String?) : BrowserEvent
}
