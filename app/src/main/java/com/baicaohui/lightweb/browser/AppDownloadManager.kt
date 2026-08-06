package com.baicaohui.lightweb.browser

import com.baicaohui.lightweb.data.db.DownloadEntity
import com.baicaohui.lightweb.data.repo.DownloadStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream

class AppDownloadManager(
    private val store: DownloadStore,
    private val scope: CoroutineScope,
    private val stagingDir: () -> File,
    private val finalizer: suspend (staged: File, fileName: String, mimeType: String?) -> String =
        { staged, _, _ -> staged.absolutePath },
    private val downloader: suspend (
        url: String,
        userAgent: String,
        output: OutputStream,
        onProgress: suspend (Long, Long) -> Unit,
    ) -> Long = { url, userAgent, out, onProgress ->
        HttpDownloader.download(url, userAgent, out, onProgress = onProgress)
    },
) {

    fun enqueue(url: String, userAgent: String, mimeType: String?, contentDisposition: String?) {
        val dir = stagingDir().apply { mkdirs() }
        val fileName = DownloadNames.from(url, contentDisposition, mimeType)
        val file = uniqueFile(dir, fileName)
        scope.launch {
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
            var entity = DownloadEntity(
                id = id,
                url = url,
                fileName = file.name,
                mimeType = mimeType,
                status = DownloadStatus.RUNNING.name,
                userAgent = userAgent,
                createdAt = now,
                updatedAt = now,
            )
            store.update(entity)
            try {
                var lastReported = 0L
                file.outputStream().use { out ->
                    val total = downloader(url, userAgent, out) { done, totalBytes ->
                        if (done - lastReported >= PROGRESS_STEP || done == totalBytes) {
                            lastReported = done
                            store.update(
                                entity.copy(
                                    downloadedBytes = done,
                                    totalBytes = totalBytes,
                                    updatedAt = System.currentTimeMillis(),
                                ),
                            )
                        }
                    }
                    entity = entity.copy(
                        status = DownloadStatus.COMPLETED.name,
                        totalBytes = total,
                        downloadedBytes = total,
                        destination = finalizer(file, file.name, mimeType),
                        updatedAt = System.currentTimeMillis(),
                    )
                }
                store.update(entity)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                runCatching { file.delete() }
                store.update(
                    entity.copy(
                        status = DownloadStatus.FAILED.name,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
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
