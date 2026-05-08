package me.rerere.awara.data.repo

// TODO(user): Decide whether importing saved feed views should overwrite matching IDs silently or ask before replacement.
// TODO(agent): If typed filters land, replace the stringly transitional mapping here with dedicated filter serializers and migration tests.

import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import me.rerere.awara.data.dao.SavedFeedViewRecord
import me.rerere.awara.data.entity.SavedFeedFilterEntity
import me.rerere.awara.data.entity.SavedFeedViewEntity
import me.rerere.awara.di.AppDatabase
import me.rerere.awara.domain.feed.FeedFilter
import me.rerere.awara.domain.feed.FeedScope
import me.rerere.awara.domain.feed.SavedFeedView
import me.rerere.awara.util.InstantSerializer
import me.rerere.awara.util.JsonInstance
import java.time.Instant

@Serializable
data class SavedFeedViewExportBundle(
    val version: Int = 1,
    @Serializable(with = InstantSerializer::class)
    val exportedAt: Instant = Instant.now(),
    val views: List<SavedFeedView>,
)

class SavedFeedViewRepo(
    private val appDatabase: AppDatabase,
) {
    suspend fun count(): Int = appDatabase.savedFeedViewDao().countViews()

    suspend fun getAll(): List<SavedFeedView> = appDatabase.savedFeedViewDao()
        .getViewRecords()
        .map(SavedFeedViewRecord::toDomain)

    suspend fun save(view: SavedFeedView) {
        appDatabase.savedFeedViewDao().replaceView(
            view = view.toEntity(),
            filters = view.toFilterEntities(),
        )
    }

    suspend fun replaceAll(views: List<SavedFeedView>) {
        appDatabase.savedFeedViewDao().replaceAllViews(
            views = views.map(SavedFeedView::toEntity),
            filters = views.flatMap(SavedFeedView::toFilterEntities),
        )
    }

    suspend fun create(
        name: String,
        scope: FeedScope,
        sort: String?,
        filters: List<FeedFilter>,
        description: String = "",
        pinned: Boolean = false,
    ): SavedFeedView {
        val view = SavedFeedView(
            id = UUID.randomUUID().toString(),
            name = name,
            scope = scope,
            description = description,
            sort = sort,
            filters = filters,
            pinned = pinned,
        )
        save(view)
        return view
    }

    suspend fun exportJson(): String {
        return JsonInstance.encodeToString(SavedFeedViewExportBundle(views = getAll()))
    }

    suspend fun importJson(content: String): Int {
        val views = runCatching {
            JsonInstance.decodeFromString<SavedFeedViewExportBundle>(content).views
        }.getOrElse {
            JsonInstance.decodeFromString<List<SavedFeedView>>(content)
        }
        views.forEach { view ->
            save(view.copy(updatedAt = Instant.now()))
        }
        return views.size
    }
}

private fun SavedFeedView.toEntity(): SavedFeedViewEntity {
    return SavedFeedViewEntity(
        id = id,
        name = name,
        scope = scope.name,
        description = description,
        sort = sort,
        pinned = pinned,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

private fun SavedFeedView.toFilterEntities(): List<SavedFeedFilterEntity> {
    return filters.mapIndexed { index, filter ->
        when (filter) {
            is FeedFilter.KeyValue -> {
                SavedFeedFilterEntity(
                    viewId = id,
                    filterType = "key_value",
                    fieldKey = filter.key,
                    value = filter.value,
                    orderIndex = index,
                )
            }
        }
    }
}

private fun SavedFeedViewRecord.toDomain(): SavedFeedView {
    return SavedFeedView(
        id = view.id,
        name = view.name,
        scope = FeedScope.valueOf(view.scope),
        description = view.description,
        sort = view.sort,
        filters = filters.map(SavedFeedFilterEntity::toDomainFilter),
        pinned = view.pinned,
        createdAt = view.createdAt,
        updatedAt = view.updatedAt,
    )
}

private fun SavedFeedFilterEntity.toDomainFilter(): FeedFilter {
    return when (filterType) {
        "key_value" -> FeedFilter.KeyValue(key = fieldKey, value = value)
        else -> FeedFilter.KeyValue(key = fieldKey, value = value)
    }
}