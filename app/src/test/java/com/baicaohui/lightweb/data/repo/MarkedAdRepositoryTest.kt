package com.baicaohui.lightweb.data.repo

import com.baicaohui.lightweb.data.db.MarkedAdDao
import com.baicaohui.lightweb.data.db.MarkedAdEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkedAdRepositoryTest {

    @Test
    fun `enabled hosts derive from enabled records only`() = runTest {
        val repo = MarkedAdRepository(FakeMarkedAdDao())
        repo.insert(
            MarkedAdEntity(
                host = "example.com",
                selector = "#ad",
                adHosts = "doubleclick.net,adnxs.com",
            ),
        )
        repo.insert(
            MarkedAdEntity(
                host = "example.com",
                selector = "#ad2",
                adHosts = "criteo.com",
                enabled = false,
            ),
        )
        assertEquals(setOf("doubleclick.net", "adnxs.com"), repo.enabledHosts.first())
    }

    @Test
    fun `by host returns only enabled rules`() = runTest {
        val repo = MarkedAdRepository(FakeMarkedAdDao())
        repo.insert(MarkedAdEntity(host = "example.com", selector = "#a", adHosts = "x.com"))
        repo.insert(
            MarkedAdEntity(
                host = "example.com",
                selector = "#b",
                adHosts = "y.com",
                enabled = false,
            ),
        )
        repo.insert(MarkedAdEntity(host = "other.com", selector = "#c", adHosts = "z.com"))
        assertEquals(listOf("#a"), repo.byHost("example.com").map { it.selector })
    }

    private class FakeMarkedAdDao : MarkedAdDao {
        private val items = MutableStateFlow<List<MarkedAdEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAll(): Flow<List<MarkedAdEntity>> = items

        override suspend fun byHost(host: String): List<MarkedAdEntity> =
            items.value.filter { it.host == host }

        override suspend fun insert(entity: MarkedAdEntity): Long {
            val withId = entity.copy(id = nextId++)
            items.value = items.value + withId
            return withId.id
        }

        override suspend fun update(entity: MarkedAdEntity) {
            items.value = items.value.map { if (it.id == entity.id) entity else it }
        }

        override suspend fun delete(entity: MarkedAdEntity) {
            items.value = items.value.filterNot { it.id == entity.id }
        }

        override suspend fun clear() {
            items.value = emptyList()
        }
    }
}
