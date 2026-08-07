package com.baicaohui.lightweb.browser

import com.baicaohui.lightweb.data.db.DownloadEntity
import com.baicaohui.lightweb.data.repo.DownloadStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
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
import java.io.File

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

        override suspend fun get(id: Long): DownloadEntity? =
            entities.value.firstOrNull { it.id == id }

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
        downloader: suspend (
            url: String,
            userAgent: String,
            file: File,
            startOffset: Long,
            onProgress: suspend (Long, Long) -> Unit,
        ) -> Long,
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
        val manager = newManager(store, dir) { _, _, file, _, onProgress ->
            file.writeBytes("hello".toByteArray())
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
        val manager = newManager(store, dir) { _, _, _, _, _ ->
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
            downloader = { _, _, file, _, onProgress ->
                file.writeBytes("data".toByteArray())
                onProgress(4, 4)
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

    @Test
    fun `pause keeps partial file and marks paused`() = runTest {
        val store = FakeStore()
        val dir = tmp.newFolder("downloads")
        val manager = newManager(store, dir) { _, _, file, _, _ ->
            file.writeBytes("abc".toByteArray())
            awaitCancellation()
        }

        manager.enqueue("https://example.com/part.bin", "BCH/1.0", null, null)
        dispatcher.scheduler.advanceUntilIdle()
        val id = store.entities.value.single().id
        assertEquals(DownloadStatus.RUNNING.name, store.entities.value.single().status)

        manager.pause(id)
        dispatcher.scheduler.advanceUntilIdle()

        val entity = store.entities.value.single()
        assertEquals(DownloadStatus.PAUSED.name, entity.status)
        assertEquals(3L, entity.downloadedBytes)
        assertTrue(File(dir, entity.fileName).exists())
    }

    @Test
    fun `resume appends to existing partial file`() = runTest {
        val store = FakeStore()
        val dir = tmp.newFolder("downloads")
        val manager = newManager(store, dir) { _, _, file, startOffset, onProgress ->
            if (startOffset > 0) {
                file.appendBytes("def".toByteArray())
                onProgress(6, 6)
                6L
            } else {
                file.writeBytes("abc".toByteArray())
                awaitCancellation()
            }
        }

        manager.enqueue("https://example.com/part.bin", "BCH/1.0", null, null)
        dispatcher.scheduler.advanceUntilIdle()
        val id = store.entities.value.single().id
        manager.pause(id)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(DownloadStatus.PAUSED.name, store.entities.value.single().status)

        manager.resume(id)
        dispatcher.scheduler.advanceUntilIdle()

        val entity = store.entities.value.single()
        assertEquals(DownloadStatus.COMPLETED.name, entity.status)
        assertEquals(6L, entity.downloadedBytes)
        assertEquals("abcdef", File(dir, entity.fileName).readText())
    }

    @Test
    fun `pauseAll pauses every running download`() = runTest {
        val store = FakeStore()
        val dir = tmp.newFolder("downloads")
        val manager = newManager(store, dir) { _, _, file, _, _ ->
            file.writeBytes("x".toByteArray())
            awaitCancellation()
        }

        manager.enqueue("https://example.com/1.bin", "BCH/1.0", null, null)
        manager.enqueue("https://example.com/2.bin", "BCH/1.0", null, null)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, store.entities.value.size)

        manager.pauseAll()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(store.entities.value.all { it.status == DownloadStatus.PAUSED.name })
    }

    @Test
    fun `liveProgress reports running progress and clears after pause`() = runTest {
        val store = FakeStore()
        val dir = tmp.newFolder("downloads")
        val manager = newManager(store, dir) { _, _, file, _, onProgress ->
            file.writeBytes("abc".toByteArray())
            onProgress(3, 10)
            awaitCancellation()
        }

        manager.enqueue("https://example.com/live.bin", "BCH/1.0", null, null)
        dispatcher.scheduler.advanceUntilIdle()
        val id = store.entities.value.single().id

        val live = manager.liveProgress.value[id]
        assertEquals(3L, live?.downloadedBytes)
        assertEquals(10L, live?.totalBytes)

        manager.pause(id)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(manager.liveProgress.value.containsKey(id))
    }
}
