package com.baicaohui.lightweb.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FolderEntity::class,
        BookmarkEntity::class,
        HistoryEntity::class,
        ShortcutEntity::class,
        SiteSettingEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun shortcutDao(): ShortcutDao
    abstract fun siteSettingDao(): SiteSettingDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "bch.db",
            ).build().also { instance = it }
        }
    }
}
