package com.baicaohui.lightweb.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("DELETE FROM folders")
    suspend fun clear()

    @Query("SELECT * FROM folders ORDER BY createdAt")
    fun observe(): Flow<List<FolderEntity>>

    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Update
    suspend fun update(folder: FolderEntity)

    @Delete
    suspend fun delete(folder: FolderEntity)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY folderId, orderIndex, createdAt")
    suspend fun all(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks ORDER BY folderId, orderIndex, createdAt")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE folderId IS NULL ORDER BY orderIndex, createdAt")
    fun observeRoot(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE folderId = :folderId ORDER BY orderIndex, createdAt")
    fun observeByFolder(folderId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): BookmarkEntity?

    @Insert
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Update
    suspend fun update(bookmark: BookmarkEntity)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE folderId = :folderId")
    suspend fun deleteByFolder(folderId: Long)

    @Query("DELETE FROM bookmarks")
    suspend fun clear()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY visitTime DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY visitTime DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): HistoryEntity?

    @Insert
    suspend fun insert(history: HistoryEntity): Long

    @Update
    suspend fun update(history: HistoryEntity)

    @Query("DELETE FROM history WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("DELETE FROM history WHERE url LIKE :pattern ESCAPE '\\'")
    suspend fun deleteByHost(pattern: String)

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcuts ORDER BY position, createdAt")
    fun observe(): Flow<List<ShortcutEntity>>

    @Insert
    suspend fun insert(shortcut: ShortcutEntity): Long

    @Update
    suspend fun update(shortcut: ShortcutEntity)

    @Delete
    suspend fun delete(shortcut: ShortcutEntity)

    @Query("DELETE FROM shortcuts")
    suspend fun clear()
}

@Dao
interface SiteSettingDao {
    @Query("SELECT * FROM site_settings WHERE host = :host LIMIT 1")
    suspend fun getByHost(host: String): SiteSettingEntity?

    @Query("SELECT * FROM site_settings WHERE host = :host LIMIT 1")
    fun observe(host: String): Flow<SiteSettingEntity?>

    @Query("SELECT * FROM site_settings ORDER BY host")
    fun observeAll(): Flow<List<SiteSettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: SiteSettingEntity)

    @Delete
    suspend fun delete(setting: SiteSettingEntity)

    @Query("DELETE FROM site_settings")
    suspend fun clear()
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): DownloadEntity?

    @Insert
    suspend fun insert(entity: DownloadEntity): Long

    @Update
    suspend fun update(entity: DownloadEntity)

    @Delete
    suspend fun delete(entity: DownloadEntity)

    @Query("DELETE FROM downloads")
    suspend fun clear()
}

@Dao
interface ReaderCacheDao {
    @Query("SELECT * FROM reader_cache WHERE url = :url LIMIT 1")
    suspend fun get(url: String): ReaderCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ReaderCacheEntity)

    @Query("DELETE FROM reader_cache WHERE url = :url")
    suspend fun delete(url: String)

    @Query("DELETE FROM reader_cache WHERE url LIKE :pattern ESCAPE '\\'")
    suspend fun deleteByHost(pattern: String)

    @Query("DELETE FROM reader_cache")
    suspend fun clear()
}

@Dao
interface CachedPageFolderDao {
    @Query("DELETE FROM cached_page_folders")
    suspend fun clear()

    @Query("SELECT * FROM cached_page_folders ORDER BY createdAt")
    fun observe(): Flow<List<CachedPageFolderEntity>>

    @Insert
    suspend fun insert(folder: CachedPageFolderEntity): Long

    @Update
    suspend fun update(folder: CachedPageFolderEntity)

    @Delete
    suspend fun delete(folder: CachedPageFolderEntity)
}

@Dao
interface CachedPageDao {
    @Query("SELECT * FROM cached_pages ORDER BY folderId, savedAt DESC")
    fun observeAll(): Flow<List<CachedPageEntity>>

    @Query("SELECT * FROM cached_pages WHERE folderId IS NULL ORDER BY savedAt DESC")
    fun observeRoot(): Flow<List<CachedPageEntity>>

    @Query("SELECT * FROM cached_pages WHERE folderId = :folderId ORDER BY savedAt DESC")
    fun observeByFolder(folderId: Long): Flow<List<CachedPageEntity>>

    @Query("SELECT * FROM cached_pages WHERE url = :url LIMIT 1")
    suspend fun findByUrl(url: String): CachedPageEntity?

    @Insert
    suspend fun insert(page: CachedPageEntity): Long

    @Update
    suspend fun update(page: CachedPageEntity)

    @Delete
    suspend fun delete(page: CachedPageEntity)

    @Query("DELETE FROM cached_pages WHERE folderId = :folderId")
    suspend fun deleteByFolder(folderId: Long)

    @Query("DELETE FROM cached_pages")
    suspend fun clear()
}

@Dao
interface MarkedAdDao {
    @Query("SELECT * FROM marked_ads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MarkedAdEntity>>

    @Query("SELECT * FROM marked_ads WHERE host = :host ORDER BY createdAt DESC")
    suspend fun byHost(host: String): List<MarkedAdEntity>

    @Insert
    suspend fun insert(entity: MarkedAdEntity): Long

    @Update
    suspend fun update(entity: MarkedAdEntity)

    @Delete
    suspend fun delete(entity: MarkedAdEntity)

    @Query("DELETE FROM marked_ads")
    suspend fun clear()
}
