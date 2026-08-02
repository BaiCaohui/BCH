package com.baicaohui.lightweb.ui.browser

import com.baicaohui.lightweb.browser.TabManager
import com.baicaohui.lightweb.browser.TabStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val template = "https://www.bing.com/search?q=%s"

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submitInput with domain loads https url`() = runTest {
        val vm = BrowserViewModel(TabManager())
        vm.submitInput("example.com", template)
        dispatcher.scheduler.advanceUntilIdle()
        val tab = vm.tabs.first().first()
        assertEquals("https://example.com", tab.url)
        assertEquals(TabStatus.LOADING, tab.status)
    }

    @Test
    fun `submitInput with phrase routes to search template`() = runTest {
        val vm = BrowserViewModel(TabManager())
        vm.submitInput("hello world", template)
        dispatcher.scheduler.advanceUntilIdle()
        val tab = vm.tabs.first().first()
        assertEquals("https://www.bing.com/search?q=hello+world", tab.url)
    }

    @Test
    fun `submitInput blank does nothing`() = runTest {
        val vm = BrowserViewModel(TabManager())
        vm.submitInput("   ", template)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, vm.tabs.first().size)
    }

    @Test
    fun `new tab and close tab delegate to manager`() = runTest {
        val vm = BrowserViewModel(TabManager())
        val a = vm.newTab("https://a.com")
        vm.newTab("https://b.com")
        vm.closeTab(a.id)
        assertEquals(listOf("https://b.com"), vm.tabs.first().map { it.url })
    }
}
