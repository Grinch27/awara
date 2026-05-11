package me.rerere.awara.domain.feed

// TODO(user): Decide whether feed queries should stay local-only first or gain preset support later.
// TODO(agent): If query semantics diverge between video, image, and user search, split FeedQuery into media and profile variants instead of overloading one model.
// TODO(agent): Keep feed models canonical and do not reintroduce saved-view state here.

import kotlinx.serialization.Serializable

@Serializable
data class FeedQuery(
    val scope: FeedScope,
    val keyword: String? = null,
    val sort: String? = null,
    val filters: List<FeedFilter> = emptyList(),
    val page: Int = 0,
    val pageSize: Int = 24,
)

@Serializable
enum class FeedScope {
    HOME_VIDEO,
    HOME_IMAGE,
    SUBSCRIPTION_VIDEO,
    SUBSCRIPTION_IMAGE,
    SEARCH_VIDEO,
    SEARCH_IMAGE,
    SEARCH_USER,
}

@Serializable
sealed interface FeedFilter {
    @Serializable
    data class KeyValue(
        val key: String,
        val value: String,
    ) : FeedFilter
}