package com.baicaohui.lightweb.ui.browser

import com.baicaohui.lightweb.browser.TabManager
import com.baicaohui.lightweb.browser.TabStatus
import com.baicaohui.lightweb.data.repo.HistoryRecorder
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

    private class FakeHistoryRecorder : HistoryRecorder {
        val calls = mutableListOf<Pair<String, String>>()
        override suspend fun record(url: String, title: String) {
            calls += url to title
        }
    }

    private fun newViewModel(recorder: FakeHistoryRecorder = FakeHistoryRecorder()) =
        BrowserViewModel(TabManager(), recorder)

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
        val vm = newViewModel()
        vm.submitInput("example.com", template)
        dispatcher.scheduler.advanceUntilIdle()
        val tab = vm.tabs.first().first()
        assertEquals("https://example.com", tab.url)
        assertEquals(TabStatus.LOADING, tab.status)
    }

    @Test
    fun `submitInput with phrase routes to search template`() = runTest {
        val vm = newViewModel()
        vm.submitInput("hello world", template)
        dispatcher.scheduler.advanceUntilIdle()
        val tab = vm.tabs.first().first()
        assertEquals("https://www.bing.com/search?q=hello+world", tab.url)
    }

    @Test
    fun `submitInput blank does nothing`() = runTest {
        val vm = newViewModel()
        vm.submitInput("   ", template)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, vm.tabs.first().size)
    }

    @Test
    fun `new tab and close tab delegate to manager`() = runTest {
        val vm = newViewModel()
        val a = vm.newTab("https://a.com")
        vm.newTab("https://b.com")
        vm.closeTab(a.id)
        assertEquals(listOf("https://b.com"), vm.tabs.first().map { it.url })
    }

    @Test
    fun `page finished records history with tab title`() = runTest {
        val recorder = FakeHistoryRecorder()
        val vm = newViewModel(recorder)
        val tab = vm.newTab("https://a.com")
        vm.onTitle(tab.id, "A 网站")
        vm.onPageFinished(tab.id, "https://a.com")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf("https://a.com" to "A 网站"), recorder.calls)
        assertEquals(TabStatus.READY, vm.tabs.first().first().status)
    }
}
