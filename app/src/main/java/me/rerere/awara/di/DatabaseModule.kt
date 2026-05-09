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
        ).addMigrations(MIGRATION_3_4)
            .build()
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

@Database(
    entities = [
        HistoryItem::class,
        DownloadItem::class,
        AppLogEntry::class,
        SavedFeedViewEntity::class,
        SavedFeedFilterEntity::class,
    ],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    abstract fun downloadDao(): DownloadDao

    abstract fun appLogDao(): AppLogDao

    abstract fun savedFeedViewDao(): SavedFeedViewDao
}