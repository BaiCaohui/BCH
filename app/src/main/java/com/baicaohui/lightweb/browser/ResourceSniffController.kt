package com.baicaohui.lightweb.browser

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class DomResource(val u: String, val k: String)

class ResourceSniffController(
    private val kindOf: (String) -> ResourceKind? = ResourceSniffer::kindOf,
    private val nameFor: (String) -> String = ResourceSniffer::nameFor,
    private val resolve: (String, String) -> String? = ResourceSniffer::resolveUrl,
) {
    private val _resources = MutableStateFlow<List<SniffedResource>>(emptyList())
    val resources: StateFlow<List<SniffedResource>> = _resources.asStateFlow()

    private val seen = linkedSetOf<String>()
    private var active = false

    fun start() {
        active = true
    }

    fun stop() {
        active = false
    }

    fun clear() {
        seen.clear()
        _resources.value = emptyList()
    }

    fun onPageStarted() {
        if (active) clear()
    }

    fun updateSize(url: String, sizeBytes: Long?) {
        _resources.value = _resources.value.map {
            if (it.url == url) it.copy(sizeBytes = sizeBytes) else it
        }
    }

    fun add(url: String, baseUrl: String) {
        if (!active) return
        val normalized = resolve(baseUrl, url) ?: return
        val kind = kindOf(normalized) ?: return
        if (seen.add(normalized)) {
            _resources.value = _resources.value +
                SniffedResource(normalized, kind, nameFor(normalized))
        }
    }

    fun addDomResult(raw: String?, baseUrl: String) {
        if (!active || raw.isNullOrBlank()) return
        val inner = runCatching { Json.decodeFromString<String>(raw) }.getOrNull() ?: return
        val list = runCatching {
            Json.decodeFromString<List<DomResource>>(inner)
        }.getOrNull() ?: return
        list.forEach { add(it.u, baseUrl) }
    }
}
