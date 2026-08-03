package com.baicaohui.lightweb.data.repo

import com.baicaohui.lightweb.data.db.BookmarkDao
import com.baicaohui.lightweb.data.db.BookmarkEntity
import com.baicaohui.lightweb.data.db.FolderDao
import com.baicaohui.lightweb.data.db.FolderEntity
import com.baicaohui.lightweb.util.BookmarkHtmlIO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BookmarkRepository(
    private val bookmarkDao: BookmarkDao,
    private val folderDao: FolderDao,
) {
    val folders: Flow<List<FolderEntity>> = folderDao.observe()

    fun bookmarks(folderId: Long?): Flow<List<BookmarkEntity>> =
        if (folderId == null) bookmarkDao.observeRoot() else bookmarkDao.observeByFolder(folderId)

    suspend fun addBookmark(title: String, url: String, folderId: Long?): Long {
        val existing = bookmarkDao.findByUrl(url)
        return if (existing != null) {
            existing.id
        } else {
            bookmarkDao.insert(BookmarkEntity(title = title, url = url, folderId = folderId))
        }
    }

    suspend fun updateBookmark(bookmark: BookmarkEntity) = bookmarkDao.update(bookmark)

    suspend fun deleteBookmark(bookmark: BookmarkEntity) = bookmarkDao.delete(bookmark)

    suspend fun addFolder(name: String): Long = folderDao.insert(FolderEntity(name = name))

    suspend fun deleteFolder(folder: FolderEntity) {
        bookmarkDao.deleteByFolder(folder.id)
        folderDao.delete(folder)
    }

    suspend fun exportHtml(): String {
        val folders = folderDao.observe().first()
        val bookmarks = bookmarkDao.all()
        return BookmarkHtmlIO.export(folders, bookmarks)
    }

    suspend fun importHtml(html: String): Int {
        val imported = BookmarkHtmlIO.import(html)
        val folderIds = mutableMapOf<String, Long>()
        var count = 0
        for (item in imported) {
            val folderId = item.folderName?.let { name ->
                folderIds.getOrPut(name) { folderDao.insert(FolderEntity(name = name)) }
            }
            if (bookmarkDao.findByUrl(item.url) == null) {
                bookmarkDao.insert(
                    BookmarkEntity(
                        title = item.title.ifBlank { item.url },
                        url = item.url,
                        folderId = folderId,
                    ),
                )
                count++
            }
        }
        return count
    }
}
