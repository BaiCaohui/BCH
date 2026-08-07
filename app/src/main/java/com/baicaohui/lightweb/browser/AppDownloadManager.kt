package com.baicaohui.lightweb.browser

import com.baicaohui.lightweb.data.db.DownloadEntity
import com.baicaohui.lightweb.data.repo.DownloadStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class LiveDownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBps: Long,
)

class AppDownloadManager(
    private val store: DownloadStore,
    private val scope: CoroutineScope,
    private val stagingDir: () -> File,
    private val finalizer: suspend (staged: File, fileName: String, mimeType: String?) -> String =
        { staged, _, _ -> staged.absolutePath },
    private val downloader: suspend (
        url: String,
        userAgent: String,
        file: File,
        startOffset: Long,
        onProgress: suspend (Long, Long) -> Unit,
    ) -> Long = { url, userAgent, file, startOffset, onProgress ->
        HttpDownloader.downloadResumable(url, userAgent, file, startOffset, onProgress)
    },
) {

    private val jobs = ConcurrentHashMap<Long, Job>()

    private val _liveProgress = MutableStateFlow<Map<Long, LiveDownloadProgress>>(emptyMap())
    val liveProgress: StateFlow<Map<Long, LiveDownloadProgress>> = _liveProgress.asStateFlow()

    suspend fun enqueue(
        url: String,
        userAgent: String,
        mimeType: String?,
        contentDisposition: String?,
    ) {
        val dir = stagingDir().apply { mkdirs() }
        val fileName = DownloadNames.from(url, contentDisposition, mimeType)
        val file = uniqueFile(dir, fileName)
        val now = System.currentTimeMillis()
        val id = store.insert(
            DownloadEntity(
                url = url,
                fileName = file.name,
                mimeType = mimeType,
                status = DownloadStatus.QUEUED.name,
                userAgent = userAgent,
                createdAt = now,
                updatedAt = now,
            ),
        )
        val entity = DownloadEntity(
            id = id,
            url = url,
            fileName = file.name,
            mimeType = mimeType,
            status = DownloadStatus.RUNNING.name,
            userAgent = userAgent,
            createdAt = now,
            updatedAt = now,
        )
        startJob(entity, file, startOffset = 0)
    }

    fun pause(id: Long) {
        jobs.remove(id)?.cancel()
    }

    fun resume(id: Long) {
        if (jobs.containsKey(id)) return
        scope.launch {
            val entity = store.get(id) ?: return@launch
            if (entity.status != DownloadStatus.PAUSED.name) return@launch
            val file = File(stagingDir(), entity.fileName)
            val startOffset = file.length().coerceAtLeast(entity.downloadedBytes)
            startJob(
                entity.copy(
                    status = DownloadStatus.RUNNING.name,
                    downloadedBytes = startOffset,
                    updatedAt = System.currentTimeMillis(),
                ),
                file,
                startOffset,
            )
        }
    }

    fun pauseAll() {
        jobs.values.toList().forEach { it.cancel() }
    }

    fun deleteTaskFiles(entity: DownloadEntity) {
        pause(entity.id)
        if (entity.status == DownloadStatus.QUEUED.name ||
            entity.status == DownloadStatus.RUNNING.name ||
            entity.status == DownloadStatus.PAUSED.name
        ) {
            runCatching { File(stagingDir(), entity.fileName).delete() }
        }
    }

    private fun startJob(entity: DownloadEntity, file: File, startOffset: Long) {
        val job = scope.launch {
            store.update(
                entity.copy(updatedAt = System.currentTimeMillis()),
            )
            var lastReported = startOffset
            var lastBytes = startOffset
            var lastTime = System.nanoTime()
            var speed = 0L
            try {
                val total = downloader(
                    entity.url,
                    entity.userAgent,
                    file,
                    startOffset,
                ) { done, totalBytes ->
                    val nowNanos = System.nanoTime()
                    val dtMs = (nowNanos - lastTime) / 1_000_000
                    if (dtMs > 0) {
                        speed = ((done - lastBytes) * 1000 / dtMs).coerceAtLeast(0)
                    }
                    lastBytes = done
                    lastTime = nowNanos
                    _liveProgress.value = _liveProgress.value +
                        (entity.id to LiveDownloadProgress(done, totalBytes, speed))
                    if (done - lastReported >= PROGRESS_STEP || done == totalBytes) {
                        lastReported = done
                        store.update(
                            entity.copy(
                                status = DownloadStatus.RUNNING.name,
                                downloadedBytes = done,
                                totalBytes = totalBytes,
                                updatedAt = System.currentTimeMillis(),
                            ),
                        )
                    }
                }
                store.update(
                    entity.copy(
                        status = DownloadStatus.COMPLETED.name,
                        totalBytes = total,
                        downloadedBytes = total,
                        destination = finalizer(file, entity.fileName, entity.mimeType),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            } catch (e: CancellationException) {
                withContext(NonCancellable) {
                    store.update(
                        entity.copy(
                            status = DownloadStatus.PAUSED.name,
                            downloadedBytes = file.length().coerceAtLeast(entity.downloadedBytes),
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                }
                throw e
            } catch (e: Exception) {
                runCatching { file.delete() }
                store.update(
                    entity.copy(
                        status = DownloadStatus.FAILED.name,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            } finally {
                _liveProgress.value = _liveProgress.value - entity.id
                jobs.remove(entity.id)
            }
        }
        jobs[entity.id] = job
    }

    private fun uniqueFile(dir: File, name: String): File {
        val base = name.substringBeforeLast('.', "")
        val ext = name.substringAfterLast('.', "").let { if (it == name || it.isBlank()) "" else ".$it" }
        var candidate = File(dir, name)
        var index = 1
        while (candidate.exists()) {
            candidate = File(dir, "$base ($index)$ext")
            index++
        }
        return candidate
    }

    private companion object {
        const val PROGRESS_STEP = 128L * 1024L
    }
}
