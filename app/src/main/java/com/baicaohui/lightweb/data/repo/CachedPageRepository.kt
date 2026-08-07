package com.baicaohui.lightweb.data.repo

import com.baicaohui.lightweb.data.db.CachedPageDao
import com.baicaohui.lightweb.data.db.CachedPageEntity
import com.baicaohui.lightweb.data.db.CachedPageFolderDao
import com.baicaohui.lightweb.data.db.CachedPageFolderEntity
import kotlinx.coroutines.flow.Flow

class CachedPageRepository(
    private val pageDao: CachedPageDao,
    private val folderDao: CachedPageFolderDao,
) {
    val folders: Flow<List<CachedPageFolderEntity>> = folderDao.observe()

    val allPages: Flow<List<CachedPageEntity>> = pageDao.observeAll()

    fun pages(folderId: Long?): Flow<List<CachedPageEntity>> =
        if (folderId == null) pageDao.observeRoot() else pageDao.observeByFolder(folderId)

    suspend fun addPage(
        title: String,
        url: String,
        folderId: Long?,
        iconUrl: String?,
        html: String,
    ) {
        val existing = pageDao.findByUrl(url)
        if (existing != null) {
            pageDao.update(
                existing.copy(
                    title = title.ifBlank { existing.title },
                    folderId = folderId,
                    iconUrl = iconUrl ?: existing.iconUrl,
                    html = html,
                    savedAt = System.currentTimeMillis(),
                ),
            )
        } else {
            pageDao.insert(
                CachedPageEntity(
                    title = title,
                    url = url,
                    folderId = folderId,
                    iconUrl = iconUrl,
                    html = html,
                ),
            )
        }
    }

    suspend fun updatePage(page: CachedPageEntity) = pageDao.update(page)

    suspend fun deletePage(page: CachedPageEntity) = pageDao.delete(page)

    suspend fun addFolder(name: String): Long =
        folderDao.insert(CachedPageFolderEntity(name = name))

    suspend fun renameFolder(folder: CachedPageFolderEntity, name: String) =
        folderDao.update(folder.copy(name = name))

    suspend fun deleteFolder(folder: CachedPageFolderEntity) {
        pageDao.deleteByFolder(folder.id)
        folderDao.delete(folder)
    }

    suspend fun clearAll() {
        pageDao.clear()
        folderDao.clear()
    }
}
