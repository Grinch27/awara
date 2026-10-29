package me.rerere.awara.di

// TODO(user): Decide whether future saved-view schema changes should keep staying in Room migrations, or move into a dedicated data module once the feature splits out.

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import me.rerere.awara.data.dao.TransferDao
import me.rerere.awara.data.entity.DownloadTask
import me.rerere.awara.data.entity.MediaProgress
import me.rerere.awara.data.entity.ImportCommit
import me.rerere.awara.data.dao.AppLogDao
import me.rerere.awara.data.dao.DownloadDao
import me.rerere.awara.data.dao.HistoryDao
import me.rerere.awara.data.entity.AppLogEntry
import me.rerere.awara.data.entity.DownloadItem
import me.rerere.awara.data.entity.HistoryItem
import androidx.sqlite.db.SupportSQLiteDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "awara.db"
        ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
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

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP INDEX IF EXISTS `index_saved_feed_filter_viewId_orderIndex`")
        database.execSQL("DROP INDEX IF EXISTS `index_saved_feed_filter_viewId`")
        database.execSQL("DROP TABLE IF EXISTS `saved_feed_filter`")
        database.execSQL("DROP TABLE IF EXISTS `saved_feed_view`")
    }
}

// No destructive fallback: all existing history and completed downloads survive.
private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS download_tasks (id TEXT NOT NULL PRIMARY KEY, resourceId TEXT NOT NULL, title TEXT NOT NULL, thumbnail TEXT NOT NULL, quality TEXT NOT NULL, owner TEXT NOT NULL, state TEXT NOT NULL, bytes INTEGER NOT NULL, totalBytes INTEGER NOT NULL, validator TEXT NOT NULL, targetPath TEXT NOT NULL, error TEXT NOT NULL, generation INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE IF NOT EXISTS media_progress (type TEXT NOT NULL, resourceId TEXT NOT NULL, fileId TEXT NOT NULL, position INTEGER NOT NULL, duration INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(type, resourceId))")
        database.execSQL("CREATE TABLE IF NOT EXISTS import_commits (id TEXT NOT NULL PRIMARY KEY)")
    }
}

@Database(
    entities = [
        HistoryItem::class,
        DownloadItem::class,
        AppLogEntry::class,
        DownloadTask::class,
        MediaProgress::class,
        ImportCommit::class,
    ],
    version = 7,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transferDao(): TransferDao

    abstract fun historyDao(): HistoryDao

    abstract fun downloadDao(): DownloadDao

    abstract fun appLogDao(): AppLogDao
}
