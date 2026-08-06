package com.baicaohui.lightweb.browser

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import com.baicaohui.lightweb.data.db.DownloadEntity
import com.baicaohui.lightweb.data.repo.DownloadStore
import java.io.File
import java.io.OutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class AppDownloadManagerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    private class FakeStore : DownloadStore {
        val entities = MutableStateFlow<List<DownloadEntity>>(emptyList())
        override val downloads: Flow<List<DownloadEntity>> = entities

        override suspend fun insert(entity: DownloadEntity): Long {
            val id = (entities.value.maxOfOrNull { it.id } ?: 0L) + 1
            entities.value = entities.value + entity.copy(id = id)
            return id
        }

        override suspend fun update(entity: DownloadEntity) {
            entities.value = entities.value.map { if (it.id == entity.id) entity else it }
        }

        override suspend fun delete(entity: DownloadEntity) {
            entities.value = entities.value.filterNot { it.id == entity.id }
        }

        override suspend fun clear() {
            entities.value = emptyList()
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newManager(
        store: FakeStore,
        dir: File,
        finalizer: suspend (File, String, String?) -> String = { staged, _, _ -> staged.absolutePath },
        downloader: suspend (String, String, OutputStream, suspend (Long, Long) -> Unit) -> Long,
    ) = AppDownloadManager(
        store = store,
        scope = CoroutineScope(dispatcher),
        stagingDir = { dir },
        downloader = downloader,
        finalizer = finalizer,
    )

    @Test
    fun `enqueue downloads to app dir and marks completed`() = runTest {
        val store = FakeStore()
        val dir = tmp.newFolder("downloads")
        val manager = newManager(store, dir) { _, _, out, onProgress ->
            out.write("hello".toByteArray())
            onProgress(5, 5)
            5L
        }

        manager.enqueue(
            url = "https://example.com/file.txt",
            userAgent = "BCH/1.0",
            mimeType = "text/plain",
            contentDisposition = "attachment; filename=\"a.txt\"",
        )
        dispatcher.scheduler.advanceUntilIdle()

        val entity = store.entities.value.single()
        assertEquals("a.txt", entity.fileName)
        assertEquals(DownloadStatus.COMPLETED.name, entity.status)
        assertEquals(5L, entity.downloadedBytes)
        assertEquals(5L, entity.totalBytes)
        assertEquals(dir.absolutePath, File(entity.destination!!).parentFile?.absolutePath)
        assertTrue(File(dir, "a.txt").exists())
    }

    @Test
    fun `failed download removes partial file and marks failed`() = runTest {
        val store = FakeStore()
        val dir = tmp.newFolder("downloads")
        val manager = newManager(store, dir) { _, _, _, _ ->
            throw DownloadException("boom")
        }

        manager.enqueue("https://example.com/fail.bin", "BCH/1.0", null, null)
        dispatcher.scheduler.advanceUntilIdle()

        val entity = store.entities.value.single()
        assertEquals(DownloadStatus.FAILED.name, entity.status)
        assertFalse(File(dir, entity.fileName).exists())
    }

    @Test
    fun `finalizer moves staged file and stores returned destination`() = runTest {
        val store = FakeStore()
        val dir = tmp.newFolder("downloads")
        val finalDir = tmp.newFolder("final")
        val manager = newManager(
            store = store,
            dir = dir,
            downloader = { _, _, out, _ ->
                out.write("data".toByteArray())
                4L
            },
            finalizer = { staged, fileName, _ ->
                val target = File(finalDir, "moved_$fileName")
                staged.copyTo(target)
                staged.delete()
                target.absolutePath
            },
        )

        manager.enqueue("https://example.com/file.txt", "BCH/1.0", "text/plain", null)
        dispatcher.scheduler.advanceUntilIdle()

        val entity = store.entities.value.single()
        assertEquals(DownloadStatus.COMPLETED.name, entity.status)
        assertTrue(File(entity.destination!!).exists())
        assertFalse(File(dir, entity.fileName).exists())
    }
}
