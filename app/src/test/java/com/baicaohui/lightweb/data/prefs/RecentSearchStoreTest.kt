package com.baicaohui.lightweb.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RecentSearchStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun newStore() = RecentSearchStore(
        PreferenceDataStoreFactory.create { tmp.newFile("recent-${System.nanoTime()}.preferences_pb") },
    )

    @Test
    fun `empty by default`() = runTest {
        assertEquals(emptyList<String>(), newStore().recent.first())
    }

    @Test
    fun `record keeps most recent first and dedupes`() = runTest {
        val store = newStore()
        store.record("hello")
        store.record("world")
        store.record("hello")
        assertEquals(listOf("hello", "world"), store.recent.first())
    }

    @Test
    fun `record ignores blank`() = runTest {
        val store = newStore()
        store.record("   ")
        assertEquals(emptyList<String>(), store.recent.first())
    }

    @Test
    fun `record caps at ten`() = runTest {
        val store = newStore()
        repeat(15) { store.record("q$it") }
        val recent = store.recent.first()
        assertEquals(10, recent.size)
        assertEquals("q14", recent.first())
        assertEquals("q5", recent.last())
    }
}
