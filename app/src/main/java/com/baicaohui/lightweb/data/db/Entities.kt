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
)
