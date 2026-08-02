package com.baicaohui.lightweb.browser

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TabStatus { EMPTY, LOADING, READY, ERROR }

data class Tab(
    val id: Long,
    val url: String = "",
    val title: String = "",
    val status: TabStatus = TabStatus.EMPTY,
    val progress: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

class TabManager(private val maxTabs: Int = 12) {

    private val _tabs = MutableStateFlow<List<Tab>>(emptyList())
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()

    private val _currentId = MutableStateFlow<Long?>(null)
    val currentId: StateFlow<Long?> = _currentId.asStateFlow()

    private val accessOrder = ArrayDeque<Long>()
    private var nextId = 1L

    val current: Tab?
        get() = _tabs.value.firstOrNull { it.id == _currentId.value }

    fun newTab(url: String = ""): Tab {
        val id = nextId++
        val tab = Tab(id = id, url = url)
        _tabs.value = _tabs.value + tab
        accessOrder.addLast(id)
        if (_tabs.value.size > maxTabs) evictOldest()
        _currentId.value = id
        return tab
    }

    fun closeTab(id: Long) {
        _tabs.value = _tabs.value.filterNot { it.id == id }
        accessOrder.remove(id)
        if (_currentId.value == id) {
            _currentId.value = accessOrder.lastOrNull() ?: _tabs.value.lastOrNull()?.id
        }
    }

    fun select(id: Long) {
        if (_tabs.value.none { it.id == id }) return
        _currentId.value = id
        accessOrder.remove(id)
        accessOrder.addLast(id)
    }

    fun update(id: Long, transform: (Tab) -> Tab) {
        _tabs.value = _tabs.value.map { if (it.id == id) transform(it) else it }
    }

    fun touch(id: Long) {
        if (_tabs.value.any { it.id == id }) {
            accessOrder.remove(id)
            accessOrder.addLast(id)
        }
    }

    private fun evictOldest() {
        val victim = accessOrder.firstOrNull() ?: return
        closeTab(victim)
    }
}
