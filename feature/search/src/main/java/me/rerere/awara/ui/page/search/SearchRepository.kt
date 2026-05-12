package me.rerere.awara.ui.page.search

import me.rerere.awara.domain.feed.FeedQuery

interface SearchRepository {
    suspend fun searchVideos(query: FeedQuery): SearchPageResult<SearchMediaItem>

    suspend fun searchImages(query: FeedQuery): SearchPageResult<SearchMediaItem>

    suspend fun searchUsers(query: String, page: Int): SearchPageResult<SearchUserItem>

    suspend fun suggestTags(query: String): List<String>

    suspend fun browseTags(filter: String, page: Int): SearchPageResult<String>
}

data class SearchPageResult<T>(
    val count: Int,
    val results: List<T>,
)

enum class SearchMediaType {
    VIDEO,
    IMAGE,
}

data class SearchMediaItem(
    val id: String,
    val type: SearchMediaType,
    val title: String,
    val authorName: String,
    val numLikes: Int,
    val numViews: Int,
    val createdAtLabel: String,
    val description: String?,
    val thumbnailUrl: String,
    val isPrivate: Boolean,
)

data class SearchUserItem(
    val id: String,
    val username: String,
    val displayName: String,
    val displayHandle: String,
    val role: String,
    val premium: Boolean,
    val avatarUrl: String,
    val hasNavigableProfile: Boolean,
)