package me.rerere.awara.di

// TODO(user): Decide whether future saved-view schema changes should keep staying in Room migrations, or move into a dedicated data module once the feature splits out.

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import me.rerere.awara.data.dao.AppLogDao
import me.rerere.awara.data.dao.DownloadDao
import me.rerere.awara.data.dao.HistoryDao
import me.rerere.awara.data.dao.SavedFeedViewDao
import me.rerere.awara.data.entity.AppLogEntry
import me.rerere.awara.data.entity.DownloadItem
import me.rerere.awara.data.entity.HistoryItem
import me.rerere.awara.data.entity.SavedFeedFilterEntity
import me.rerere.awara.data.entity.SavedFeedViewEntity
import androidx.sqlite.db.SupportSQLiteDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "awara.db"
        ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .build()
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `app_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `time` INTEGER NOT NULL, `level` TEXT NOT NULL, `tag` TEXT NOT NULL, `message` TEXT NOT NULL, `throwable` TEXT)",
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `saved_feed_view` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `scope` TEXT NOT NULL, `description` TEXT NOT NULL, `sort` TEXT, `pinned` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `saved_feed_filter` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `viewId` TEXT NOT NULL, `filterType` TEXT NOT NULL, `fieldKey` TEXT NOT NULL, `operator` TEXT NOT NULL, `value` TEXT NOT NULL, `extraValue` TEXT, `orderIndex` INTEGER NOT NULL, FOREIGN KEY(`viewId`) REFERENCES `saved_feed_view`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_saved_feed_filter_viewId` ON `saved_feed_filter` (`viewId`)",
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_saved_feed_filter_viewId_orderIndex` ON `saved_feed_filter` (`viewId`, `orderIndex`)",
        )
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE saved_feed_view ADD COLUMN tags TEXT NOT NULL DEFAULT ''",
        )
        database.execSQL(
            "ALTER TABLE saved_feed_view ADD COLUMN smartSubscription INTEGER NOT NULL DEFAULT 0",
        )
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE saved_feed_view ADD COLUMN pinOrder INTEGER NOT NULL DEFAULT 0",
        )
    }
}

@Database(
    entities = [
        HistoryItem::class,
        DownloadItem::class,
        AppLogEntry::class,
        SavedFeedViewEntity::class,
        SavedFeedFilterEntity::class,
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    abstract fun downloadDao(): DownloadDao

    abstract fun appLogDao(): AppLogDao

    abstract fun savedFeedViewDao(): SavedFeedViewDao
}