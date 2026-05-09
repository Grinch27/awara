package me.rerere.awara.data.repo

// TODO(user): Decide whether importing saved feed views should overwrite matching IDs silently or ask before replacement.
// TODO(agent): If typed filters land, replace the stringly transitional mapping here with dedicated filter serializers and migration tests.

import androidx.room.withTransaction
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
    val version: Int = 3,
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
        val dao = appDatabase.savedFeedViewDao()
        val existingView = dao.getView(view.id)
        val normalizedView = view.normalizePinState(
            existingPinOrder = existingView?.pinOrder,
            nextPinOrder = if (view.pinned && existingView?.pinned != true && view.pinOrder <= 0) {
                dao.getMaxPinOrder(view.scope.name) + 1
            } else {
                null
            },
        )
        dao.replaceView(
            view = normalizedView.toEntity(),
            filters = normalizedView.toFilterEntities(),
        )
        if (existingView?.pinned == true && !normalizedView.pinned) {
            compactPinOrders(FeedScope.valueOf(existingView.scope))
        }
    }

    suspend fun replaceAll(views: List<SavedFeedView>) {
        val normalizedViews = views.normalizePinOrders()
        appDatabase.savedFeedViewDao().replaceAllViews(
            views = normalizedViews.map(SavedFeedView::toEntity),
            filters = normalizedViews.flatMap(SavedFeedView::toFilterEntities),
        )
    }

    suspend fun update(view: SavedFeedView) {
        save(view.copy(updatedAt = Instant.now()))
    }

    suspend fun delete(id: String) {
        val dao = appDatabase.savedFeedViewDao()
        val view = dao.getView(id)
        dao.deleteView(id)
        if (view?.pinned == true) {
            compactPinOrders(FeedScope.valueOf(view.scope))
        }
    }

    suspend fun setPinned(view: SavedFeedView, pinned: Boolean) {
        val dao = appDatabase.savedFeedViewDao()
        val updatedAt = Instant.now().toEpochMilli()
        if (pinned) {
            val nextPinOrder = if (view.pinned && view.pinOrder > 0) {
                view.pinOrder
            } else {
                dao.getMaxPinOrder(view.scope.name) + 1
            }
            dao.updatePinnedState(
                id = view.id,
                pinned = true,
                pinOrder = nextPinOrder,
                updatedAt = updatedAt,
            )
            return
        }
        dao.updatePinnedState(
            id = view.id,
            pinned = false,
            pinOrder = 0,
            updatedAt = updatedAt,
        )
        compactPinOrders(view.scope)
    }

    suspend fun movePinnedView(scope: FeedScope, viewId: String, moveUp: Boolean) {
        val pinnedViews = getAll()
            .filter { it.scope == scope && it.pinned }
            .sortedWith(compareBy<SavedFeedView> { it.pinOrder }.thenByDescending { it.updatedAt })
        val currentIndex = pinnedViews.indexOfFirst { it.id == viewId }
        if (currentIndex == -1) {
            return
        }
        val targetIndex = if (moveUp) currentIndex - 1 else currentIndex + 1
        if (targetIndex !in pinnedViews.indices) {
            return
        }
        val reorderedViews = pinnedViews.toMutableList().apply {
            add(targetIndex, removeAt(currentIndex))
        }
        writePinOrder(scope = scope, orderedViews = reorderedViews)
    }

    suspend fun create(
        name: String,
        scope: FeedScope,
        sort: String?,
        filters: List<FeedFilter>,
        description: String = "",
        tags: List<String> = emptyList(),
        pinned: Boolean = false,
        smartSubscription: Boolean = false,
    ): SavedFeedView {
        val view = SavedFeedView(
            id = UUID.randomUUID().toString(),
            name = name,
            scope = scope,
            description = description,
            tags = tags,
            sort = sort,
            filters = filters,
            pinned = pinned,
            smartSubscription = smartSubscription,
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

    private suspend fun compactPinOrders(scope: FeedScope) {
        val pinnedViews = getAll().filter { it.scope == scope && it.pinned }
        writePinOrder(scope = scope, orderedViews = pinnedViews)
    }

    private suspend fun writePinOrder(
        scope: FeedScope,
        orderedViews: List<SavedFeedView>,
    ) {
        val dao = appDatabase.savedFeedViewDao()
        val updatedAt = Instant.now().toEpochMilli()
        appDatabase.withTransaction {
            orderedViews
                .filter { it.scope == scope }
                .forEachIndexed { index, savedView ->
                    dao.updatePinOrder(
                        id = savedView.id,
                        pinOrder = index + 1,
                        updatedAt = updatedAt,
                    )
                }
        }
    }
}

private fun SavedFeedView.toEntity(): SavedFeedViewEntity {
    return SavedFeedViewEntity(
        id = id,
        name = name,
        scope = scope.name,
        description = description,
        tags = tags.joinToString(separator = ","),
        sort = sort,
        pinned = pinned,
        pinOrder = pinOrder,
        smartSubscription = smartSubscription,
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
        tags = view.tags.toSavedViewTags(),
        sort = view.sort,
        filters = filters.map(SavedFeedFilterEntity::toDomainFilter),
        pinned = view.pinned,
        pinOrder = view.pinOrder,
        smartSubscription = view.smartSubscription,
        createdAt = view.createdAt,
        updatedAt = view.updatedAt,
    )
}

private fun SavedFeedView.normalizePinState(
    existingPinOrder: Int?,
    nextPinOrder: Int?,
): SavedFeedView {
    if (!pinned) {
        return copy(pinOrder = 0)
    }
    return copy(pinOrder = when {
        existingPinOrder != null -> existingPinOrder
        pinOrder > 0 -> pinOrder
        nextPinOrder != null -> nextPinOrder
        else -> 0
    })
}

private fun List<SavedFeedView>.normalizePinOrders(): List<SavedFeedView> {
    val nextPinOrderByScope = mutableMapOf<FeedScope, Int>()
    return map { savedView ->
        if (!savedView.pinned) {
            savedView.copy(pinOrder = 0)
        } else {
            val nextPinOrder = nextPinOrderByScope.getOrElse(savedView.scope) { 1 }
            nextPinOrderByScope[savedView.scope] = nextPinOrder + 1
            savedView.copy(pinOrder = nextPinOrder)
        }
    }
}

private fun SavedFeedFilterEntity.toDomainFilter(): FeedFilter {
    return when (filterType) {
        "key_value" -> FeedFilter.KeyValue(key = fieldKey, value = value)
        else -> FeedFilter.KeyValue(key = fieldKey, value = value)
    }
}

private fun String.toSavedViewTags(): List<String> {
    return split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()
}