package me.rerere.awara.ui.page.search

import me.rerere.awara.domain.feed.FeedQuery

interface SearchRepository {
    suspend fun searchVideos(query: FeedQuery): SearchPageResult<SearchMediaItem>

    suspend fun searchImages(query: FeedQuery): SearchPageResult<SearchMediaItem>

    suspend fun searchUsers(query: String, page: Int): SearchPageResult<SearchUserItem>

    suspend fun searchPosts(query: String, page: Int): SearchPageResult<SearchPostItem>

    suspend fun searchPlaylists(query: String, page: Int): SearchPageResult<SearchPlaylistItem>

    suspend fun searchForumPosts(query: String, page: Int): SearchPageResult<SearchForumPostItem>

    suspend fun searchForumThreads(query: String, page: Int): SearchPageResult<SearchForumThreadItem>

    suspend fun suggestTags(query: String): List<String>

    suspend fun browseTags(filter: String, page: Int): SearchPageResult<String>
}

object SearchTypes {
    const val VIDEO = "video"
    const val IMAGE = "image"
    const val POST = "post"
    const val USER = "user"
    const val PLAYLIST = "playlist"
    const val FORUM_POST = "forum_post"
    const val FORUM_THREAD = "forum_thread"

    val all = listOf(
        VIDEO,
        IMAGE,
        POST,
        USER,
        PLAYLIST,
        FORUM_POST,
        FORUM_THREAD,
    )

    val media = setOf(VIDEO, IMAGE)

    fun normalize(type: String): String {
        return when (type) {
            "videos", VIDEO -> VIDEO
            "images", IMAGE -> IMAGE
            "posts", POST -> POST
            "users", USER -> USER
            "playlists", PLAYLIST -> PLAYLIST
            "forum_posts", FORUM_POST -> FORUM_POST
            "forum_threads", FORUM_THREAD -> FORUM_THREAD
            else -> VIDEO
        }
    }
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

data class SearchPostItem(
    val id: String,
    val title: String,
    val authorName: String,
    val createdAtLabel: String,
    val body: String,
    val numViews: Int,
)

data class SearchPlaylistItem(
    val id: String,
    val title: String,
    val authorName: String,
    val numVideos: Int,
    val thumbnailUrl: String?,
)

data class SearchForumPostItem(
    val id: String,
    val threadId: String,
    val threadTitle: String,
    val authorName: String,
    val createdAtLabel: String,
    val body: String,
    val replyNum: Int,
)

data class SearchForumThreadItem(
    val id: String,
    val title: String,
    val section: String,
    val authorName: String,
    val updatedAtLabel: String,
    val numPosts: Int,
    val numViews: Int,
    val locked: Boolean,
    val sticky: Boolean,
)
