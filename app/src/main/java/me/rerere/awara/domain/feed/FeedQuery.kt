package me.rerere.awara.domain.feed

// TODO(user): Decide whether saved feed views should support only local persistence first or sync between devices later.
// TODO(agent): If query semantics diverge between video, image, and user search, split FeedQuery into media and profile variants instead of overloading one model.
// TODO(agent): If filter complexity grows, replace the generic key/value bridge with typed filter subclasses and dedicated serializers.

data class FeedQuery(
    val scope: FeedScope,
    val keyword: String? = null,
    val sort: String? = null,
    val filters: List<FeedFilter> = emptyList(),
    val page: Int = 0,
    val pageSize: Int = 24,
)

enum class FeedScope {
    HOME_VIDEO,
    HOME_IMAGE,
    SUBSCRIPTION_VIDEO,
    SUBSCRIPTION_IMAGE,
    SEARCH_VIDEO,
    SEARCH_IMAGE,
    SEARCH_USER,
}

sealed interface FeedFilter {
    data class KeyValue(
        val key: String,
        val value: String,
    ) : FeedFilter
}

data class SavedFeedView(
    val id: String,
    val name: String,
    val scope: FeedScope,
    val description: String = "",
    val sort: String? = null,
    val filters: List<FeedFilter> = emptyList(),
    val pinned: Boolean = false,
)