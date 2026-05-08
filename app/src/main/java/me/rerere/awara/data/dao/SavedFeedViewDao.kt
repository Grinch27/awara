package me.rerere.awara.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import me.rerere.awara.data.entity.SavedFeedFilterEntity
import me.rerere.awara.data.entity.SavedFeedViewEntity

data class SavedFeedViewRecord(
    val view: SavedFeedViewEntity,
    val filters: List<SavedFeedFilterEntity>,
)

@Dao
interface SavedFeedViewDao {
    @Query("SELECT * FROM saved_feed_view ORDER BY pinned DESC, updatedAt DESC, createdAt DESC")
    suspend fun getViews(): List<SavedFeedViewEntity>

    @Query("SELECT * FROM saved_feed_filter WHERE viewId IN (:viewIds)")
    suspend fun getFiltersByViewIds(viewIds: List<String>): List<SavedFeedFilterEntity>

    @Query("SELECT COUNT(*) FROM saved_feed_view")
    suspend fun countViews(): Int

    @Upsert
    suspend fun upsertView(view: SavedFeedViewEntity)

    @Insert
    suspend fun insertViews(views: List<SavedFeedViewEntity>)

    @Insert
    suspend fun insertFilters(filters: List<SavedFeedFilterEntity>)

    @Query("DELETE FROM saved_feed_filter WHERE viewId = :viewId")
    suspend fun deleteFiltersForView(viewId: String)

    @Query("DELETE FROM saved_feed_view WHERE id = :id")
    suspend fun deleteView(id: String)

    @Query("DELETE FROM saved_feed_filter")
    suspend fun deleteAllFilters()

    @Query("DELETE FROM saved_feed_view")
    suspend fun deleteAllViews()

    @Transaction
    suspend fun replaceView(view: SavedFeedViewEntity, filters: List<SavedFeedFilterEntity>) {
        upsertView(view)
        deleteFiltersForView(view.id)
        if (filters.isNotEmpty()) {
            insertFilters(filters)
        }
    }

    @Transaction
    suspend fun replaceAllViews(
        views: List<SavedFeedViewEntity>,
        filters: List<SavedFeedFilterEntity>,
    ) {
        deleteAllFilters()
        deleteAllViews()
        if (views.isNotEmpty()) {
            insertViews(views)
        }
        if (filters.isNotEmpty()) {
            insertFilters(filters)
        }
    }

    @Transaction
    suspend fun getViewRecords(): List<SavedFeedViewRecord> {
        val views = getViews()
        if (views.isEmpty()) {
            return emptyList()
        }
        val filters = getFiltersByViewIds(views.map(SavedFeedViewEntity::id))
            .groupBy(SavedFeedFilterEntity::viewId)

        return views.map { view ->
            SavedFeedViewRecord(
                view = view,
                filters = filters[view.id].orEmpty().sortedBy(SavedFeedFilterEntity::orderIndex),
            )
        }
    }
}