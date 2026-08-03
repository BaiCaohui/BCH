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
