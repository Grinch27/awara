package me.rerere.awara.data.entity

// TODO(user): Decide whether saved feed views should eventually sync through an account or remain local-first.
// TODO(agent): If typed filters replace the key/value bridge, migrate persistence with explicit filter DTOs instead of stretching this transitional schema.

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.Instant

@Entity(tableName = "saved_feed_view")
@TypeConverters(SavedFeedInstantConverter::class)
data class SavedFeedViewEntity(
    @PrimaryKey val id: String,
    @ColumnInfo val name: String,
    @ColumnInfo val scope: String,
    @ColumnInfo val description: String = "",
    @ColumnInfo val tags: String = "",
    @ColumnInfo val sort: String? = null,
    @ColumnInfo val pinned: Boolean = false,
    @ColumnInfo val pinOrder: Int = 0,
    @ColumnInfo val smartSubscription: Boolean = false,
    @ColumnInfo val createdAt: Instant = Instant.now(),
    @ColumnInfo val updatedAt: Instant = Instant.now(),
)

@Entity(
    tableName = "saved_feed_filter",
    foreignKeys = [
        ForeignKey(
            entity = SavedFeedViewEntity::class,
            parentColumns = ["id"],
            childColumns = ["viewId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("viewId"),
        Index(value = ["viewId", "orderIndex"], unique = true),
    ],
)
data class SavedFeedFilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo val viewId: String,
    @ColumnInfo val filterType: String,
    @ColumnInfo val fieldKey: String,
    @ColumnInfo val operator: String = "eq",
    @ColumnInfo val value: String,
    @ColumnInfo val extraValue: String? = null,
    @ColumnInfo val orderIndex: Int = 0,
)

class SavedFeedInstantConverter {
    @TypeConverter
    fun toInstant(value: Long): Instant = Instant.ofEpochMilli(value)

    @TypeConverter
    fun fromInstant(value: Instant): Long = value.toEpochMilli()
}