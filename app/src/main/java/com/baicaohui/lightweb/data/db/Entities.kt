package com.baicaohui.lightweb.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "bookmarks", indices = [Index(value = ["url"], unique = true)])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long? = null,
    val title: String,
    val url: String,
    val iconUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val orderIndex: Int = 0,
)

@Entity(tableName = "history", indices = [Index(value = ["url", "visitTime"])])
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String = "",
    val visitTime: Long = System.currentTimeMillis(),
    val visitCount: Int = 1,
)

@Entity(tableName = "shortcuts")
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val color: Long? = null,
    val iconUrl: String? = null,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "site_settings")
data class SiteSettingEntity(
    @PrimaryKey val host: String,
    val jsEnabled: Boolean? = null,
    val adLevel: String? = null,
    val desktopMode: Boolean? = null,
    val safeBrowsing: Boolean? = null,
    val thirdPartyCookies: Boolean? = null,
    val location: Boolean? = null,
    val camera: Boolean? = null,
    val microphone: Boolean? = null,
    val notifications: Boolean? = null,
    val popups: Boolean? = null,
    val autoplay: Boolean? = null,
    val httpsUpgrade: Boolean? = null,
    val clearOnExit: Boolean? = null,
    val antiTracking: Boolean? = null,
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val fileName: String,
    val mimeType: String? = null,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: String = "QUEUED",
    val destination: String? = null,
    val userAgent: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "reader_cache")
data class ReaderCacheEntity(
    @PrimaryKey val url: String,
    val title: String,
    val byline: String = "",
    val contentHtml: String,
    val savedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cached_page_folders")
data class CachedPageFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cached_pages")
data class CachedPageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long? = null,
    val title: String,
    val url: String,
    val iconUrl: String? = null,
    val html: String,
    val savedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "marked_ads", indices = [Index(value = ["host"])])
data class MarkedAdEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val host: String,
    val selector: String,
    val html: String = "",
    val adHosts: String = "",
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)
