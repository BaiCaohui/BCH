package com.baicaohui.lightweb.ui.browser

import com.baicaohui.lightweb.browser.TabManager
import com.baicaohui.lightweb.browser.TabStatus
import com.baicaohui.lightweb.data.repo.HistoryRecorder
import com.baicaohui.lightweb.data.prefs.SearchRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

    private class FakeSearchRecorder : SearchRecorder {
        val queries = mutableListOf<String>()
        override suspend fun record(query: String) {
            queries += query
        }
    }

    private fun newViewModel(
        recorder: FakeHistoryRecorder = FakeHistoryRecorder(),
        searchRecorder: FakeSearchRecorder = FakeSearchRecorder(),
    ) = BrowserViewModel(TabManager(), recorder, searchRecorder)

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
    fun `submitInput with phrase records recent search`() = runTest {
        val searchRecorder = FakeSearchRecorder()
        val vm = newViewModel(searchRecorder = searchRecorder)
        vm.submitInput("hello world", template)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf("hello world"), searchRecorder.queries)
    }

    @Test
    fun `submitInput with url does not record recent search`() = runTest {
        val searchRecorder = FakeSearchRecorder()
        val vm = newViewModel(searchRecorder = searchRecorder)
        vm.submitInput("example.com", template)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(emptyList<String>(), searchRecorder.queries)
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

    @Test
    fun `page finished in incognito does not record history`() = runTest {
        val recorder = FakeHistoryRecorder()
        val manager = TabManager()
        val vm = BrowserViewModel(manager, recorder, FakeSearchRecorder())
        manager.enterIncognito()
        val tab = manager.current!!
        vm.onTitle(tab.id, "A 网站")
        vm.onPageFinished(tab.id, "https://a.com")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(emptyList<Pair<String, String>>(), recorder.calls)
        assertEquals(TabStatus.READY, vm.tabs.first().first().status)
    }

    @Test
    fun `submitInput in incognito stays in incognito stack`() = runTest {
        val manager = TabManager()
        manager.enterIncognito()
        val vm = BrowserViewModel(manager, FakeHistoryRecorder(), FakeSearchRecorder())
        vm.submitInput("example.com", template)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, manager.incognito.value)
        assertEquals(listOf("https://example.com"), vm.tabs.first().map { it.url })
    }

    @Test
    fun `toggle reader emits enter when not active`() = runTest {
        val vm = newViewModel()
        val tab = vm.newTab("https://a.com")
        val events = mutableListOf<BrowserEvent>()
        val job = launch { vm.events.collect { events += it } }
        vm.toggleReaderMode()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf<BrowserEvent>(BrowserEvent.EnterReader), events)
        assertEquals(false, vm.tabs.first().first { it.id == tab.id }.readerMode)
        job.cancel()
    }

    @Test
    fun `toggle reader emits exit when active`() = runTest {
        val vm = newViewModel()
        val tab = vm.newTab("https://a.com")
        vm.setReaderMode(tab.id, true)
        val events = mutableListOf<BrowserEvent>()
        val job = launch { vm.events.collect { events += it } }
        vm.toggleReaderMode()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf<BrowserEvent>(BrowserEvent.ExitReader), events)
        job.cancel()
    }

    @Test
    fun `toggle reader ignores blank url`() = runTest {
        val vm = newViewModel()
        vm.newTab("")
        val events = mutableListOf<BrowserEvent>()
        val job = launch { vm.events.collect { events += it } }
        vm.toggleReaderMode()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(emptyList<BrowserEvent>(), events)
        job.cancel()
    }

    @Test
    fun `page started resets reader flags`() = runTest {
        val vm = newViewModel()
        val tab = vm.newTab("https://a.com")
        vm.setReaderMode(tab.id, true)
        vm.setReaderOffline(tab.id, true)
        vm.onPageStarted(tab.id, "https://b.com")
        val updated = vm.tabs.first().first { it.id == tab.id }
        assertEquals(false, updated.readerMode)
        assertEquals(false, updated.readerOffline)
        assertEquals("https://b.com", updated.url)
    }
}
