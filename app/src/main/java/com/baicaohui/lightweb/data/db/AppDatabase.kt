package com.baicaohui.lightweb.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FolderEntity::class,
        BookmarkEntity::class,
        HistoryEntity::class,
        ShortcutEntity::class,
        SiteSettingEntity::class,
        DownloadEntity::class,
        ReaderCacheEntity::class,
        CachedPageFolderEntity::class,
        CachedPageEntity::class,
        MarkedAdEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun shortcutDao(): ShortcutDao
    abstract fun siteSettingDao(): SiteSettingDao
    abstract fun downloadDao(): DownloadDao
    abstract fun readerCacheDao(): ReaderCacheDao
    abstract fun cachedPageFolderDao(): CachedPageFolderDao
    abstract fun cachedPageDao(): CachedPageDao
    abstract fun markedAdDao(): MarkedAdDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `downloads` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `url` TEXT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `mimeType` TEXT,
                        `totalBytes` INTEGER NOT NULL,
                        `downloadedBytes` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `destination` TEXT,
                        `userAgent` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE site_settings ADD COLUMN safeBrowsing INTEGER")
                db.execSQL("ALTER TABLE site_settings ADD COLUMN thirdPartyCookies INTEGER")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reader_cache` (
                        `url` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `byline` TEXT NOT NULL,
                        `contentHtml` TEXT NOT NULL,
                        `savedAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE site_settings ADD COLUMN location INTEGER")
                db.execSQL("ALTER TABLE site_settings ADD COLUMN camera INTEGER")
                db.execSQL("ALTER TABLE site_settings ADD COLUMN microphone INTEGER")
                db.execSQL("ALTER TABLE site_settings ADD COLUMN notifications INTEGER")
                db.execSQL("ALTER TABLE site_settings ADD COLUMN popups INTEGER")
                db.execSQL("ALTER TABLE site_settings ADD COLUMN autoplay INTEGER")
                db.execSQL("ALTER TABLE site_settings ADD COLUMN httpsUpgrade INTEGER")
                db.execSQL("ALTER TABLE site_settings ADD COLUMN clearOnExit INTEGER")
                db.execSQL("ALTER TABLE site_settings ADD COLUMN antiTracking INTEGER")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_page_folders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_pages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `folderId` INTEGER,
                        `title` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `iconUrl` TEXT,
                        `html` TEXT NOT NULL,
                        `savedAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `marked_ads` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `host` TEXT NOT NULL,
                        `selector` TEXT NOT NULL,
                        `adHosts` TEXT NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_marked_ads_host` ON `marked_ads` (`host`)",
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE marked_ads ADD COLUMN html TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "bch.db",
            )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                )
                .build()
                .also { instance = it }
        }
    }
}
