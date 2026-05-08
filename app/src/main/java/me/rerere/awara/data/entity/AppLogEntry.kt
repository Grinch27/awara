package me.rerere.awara.data.entity

// TODO(user): Decide whether release builds should keep exporting debug-level diagnostics by default.
// TODO(agent): If log write amplification becomes noticeable, batch inserts behind a small in-memory buffer instead of writing every event immediately.

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import me.rerere.awara.util.InstantSerializer
import java.time.Instant

@Entity(tableName = "app_logs")
@TypeConverters(AppLogLevelConverter::class, AppLogInstantConverter::class)
@Serializable
data class AppLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @Serializable(with = InstantSerializer::class)
    @ColumnInfo val time: Instant = Instant.now(),
    @ColumnInfo val level: AppLogLevel,
    @ColumnInfo val tag: String,
    @ColumnInfo val message: String,
    @ColumnInfo val throwable: String? = null,
)

@Serializable
enum class AppLogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
}

class AppLogLevelConverter {
    @TypeConverter
    fun toLevel(value: String): AppLogLevel = enumValueOf(value)

    @TypeConverter
    fun fromLevel(value: AppLogLevel): String = value.name
}

class AppLogInstantConverter {
    @TypeConverter
    fun toInstant(value: Long): Instant = Instant.ofEpochMilli(value)

    @TypeConverter
    fun fromInstant(value: Instant): Long = value.toEpochMilli()
}

@Serializable
data class AppLogExportBundle(
    val version: Int = 1,
    @Serializable(with = InstantSerializer::class)
    val exportedAt: Instant = Instant.now(),
    val logs: List<AppLogEntry>,
)