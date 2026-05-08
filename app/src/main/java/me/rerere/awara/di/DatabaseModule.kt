package me.rerere.awara.di

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import me.rerere.awara.data.dao.AppLogDao
import me.rerere.awara.data.dao.DownloadDao
import me.rerere.awara.data.dao.HistoryDao
import me.rerere.awara.data.dao.SavedFeedViewDao
import me.rerere.awara.data.entity.AppLogEntry
import me.rerere.awara.data.entity.DownloadItem
import me.rerere.awara.data.entity.HistoryItem
import me.rerere.awara.data.entity.SavedFeedFilterEntity
import me.rerere.awara.data.entity.SavedFeedViewEntity
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "awara.db"
        ).build()
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
    version = 3,
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