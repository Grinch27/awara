package me.rerere.awara.ui.page.search

import me.rerere.awara.data.entity.ForumPost
import me.rerere.awara.data.entity.ForumThread
import me.rerere.awara.data.entity.Image
import me.rerere.awara.data.entity.Playlist
import me.rerere.awara.data.entity.Post
import me.rerere.awara.data.entity.User
import me.rerere.awara.data.entity.Video
import me.rerere.awara.data.entity.thumbnailUrl
import me.rerere.awara.data.entity.toAvatarUrl
import me.rerere.awara.data.feed.toApiParams
import me.rerere.awara.data.repo.MediaRepo
import me.rerere.awara.domain.feed.FeedQuery
import me.rerere.awara.util.toLocalDateTimeString

class AppSearchRepository(
    private val mediaRepo: MediaRepo,
) : SearchRepository {
    override suspend fun searchVideos(query: FeedQuery): SearchPageResult<SearchMediaItem> {
        val pager = mediaRepo.getVideoList(query.toApiParams())
        return SearchPageResult(
            count = pager.count,
            results = pager.results.map(Video::toSearchMediaItem),
        )
    }

    override suspend fun searchImages(query: FeedQuery): SearchPageResult<SearchMediaItem> {
        val pager = mediaRepo.getImageList(query.toApiParams())
        return SearchPageResult(
            count = pager.count,
            results = pager.results.map(Image::toSearchMediaItem),
        )
    }

    override suspend fun searchUsers(query: String, page: Int): SearchPageResult<SearchUserItem> {
        val pager = mediaRepo.searchUser(query, page)
        return SearchPageResult(
            count = pager.count,
            results = pager.results.map(User::toSearchUserItem),
        )
    }

    override suspend fun searchPosts(query: String, page: Int): SearchPageResult<SearchPostItem> {
        val pager = mediaRepo.searchPosts(query, page)
        return SearchPageResult(
            count = pager.count,
            results = pager.results.map(Post::toSearchPostItem),
        )
    }

    override suspend fun searchPlaylists(query: String, page: Int): SearchPageResult<SearchPlaylistItem> {
        val pager = mediaRepo.searchPlaylists(query, page)
        return SearchPageResult(
            count = pager.count,
            results = pager.results.map(Playlist::toSearchPlaylistItem),
        )
    }

    override suspend fun searchForumPosts(query: String, page: Int): SearchPageResult<SearchForumPostItem> {
        val pager = mediaRepo.searchForumPosts(query, page)
        return SearchPageResult(
            count = pager.count,
            results = pager.results.map(ForumPost::toSearchForumPostItem),
        )
    }

    override suspend fun searchForumThreads(query: String, page: Int): SearchPageResult<SearchForumThreadItem> {
        val pager = mediaRepo.searchForumThreads(query, page)
        return SearchPageResult(
            count = pager.count,
            results = pager.results.map(ForumThread::toSearchForumThreadItem),
        )
    }

    override suspend fun suggestTags(query: String): List<String> {
        return mediaRepo.getTagsSuggestions(query).results.map { it.id }
    }

    override suspend fun browseTags(filter: String, page: Int): SearchPageResult<String> {
        val pager = mediaRepo.getTags(filter, page)
        return SearchPageResult(
            count = pager.count,
            results = pager.results.map { it.id },
        )
    }
}

private fun Video.toSearchMediaItem(): SearchMediaItem {
    return SearchMediaItem(
        id = id,
        type = SearchMediaType.VIDEO,
        title = title,
        authorName = user.displayName,
        numLikes = numLikes,
        numViews = numViews,
        createdAtLabel = createdAt.toLocalDateTimeString(),
        description = body?.trim(),
        thumbnailUrl = thumbnailUrl(),
        isPrivate = private,
    )
}

private fun Image.toSearchMediaItem(): SearchMediaItem {
    return SearchMediaItem(
        id = id,
        type = SearchMediaType.IMAGE,
        title = title,
        authorName = user.displayName,
        numLikes = numLikes,
        numViews = numViews,
        createdAtLabel = createdAt.toLocalDateTimeString(),
        description = body?.trim(),
        thumbnailUrl = thumbnailUrl(),
        isPrivate = false,
    )
}

private fun User.toSearchUserItem(): SearchUserItem {
    return SearchUserItem(
        id = id,
        username = username,
        displayName = displayName,
        displayHandle = displayHandle,
        role = role,
        premium = premium,
        avatarUrl = avatar.toAvatarUrl(),
        hasNavigableProfile = hasNavigableProfile,
    )
}

private fun Post.toSearchPostItem(): SearchPostItem {
    return SearchPostItem(
        id = id,
        title = title.ifBlank { id },
        authorName = user?.displayName.orEmpty(),
        createdAtLabel = createdAt.toSearchDateLabel(),
        body = body.trim(),
        numViews = numViews,
    )
}

private fun Playlist.toSearchPlaylistItem(): SearchPlaylistItem {
    return SearchPlaylistItem(
        id = id,
        title = title.ifBlank { id },
        authorName = user?.displayName.orEmpty(),
        numVideos = numVideos,
        thumbnailUrl = thumbnail?.toThumbnailUrl(),
    )
}

private fun ForumPost.toSearchForumPostItem(): SearchForumPostItem {
    val targetThreadId = threadId.ifBlank { thread?.id.orEmpty() }
    return SearchForumPostItem(
        id = id,
        threadId = targetThreadId,
        threadTitle = thread?.title?.ifBlank { targetThreadId }.orEmpty(),
        authorName = user?.displayName.orEmpty(),
        createdAtLabel = createdAt.toSearchDateLabel(),
        body = body.trim(),
        replyNum = replyNum,
    )
}

private fun ForumThread.toSearchForumThreadItem(): SearchForumThreadItem {
    return SearchForumThreadItem(
        id = id,
        title = title.ifBlank { id },
        section = section,
        authorName = user?.displayName.orEmpty(),
        updatedAtLabel = (updatedAt ?: createdAt).toSearchDateLabel(),
        numPosts = numPosts,
        numViews = numViews,
        locked = locked,
        sticky = sticky,
    )
}

private fun me.rerere.awara.data.entity.PlaylistThumbnail.toThumbnailUrl(): String? {
    val fileId = file?.id ?: return null
    val thumbnailIndex = thumbnail.toString().padStart(2, '0')
    return "https://i.iwara.tv/image/thumbnail/$fileId/thumbnail-$thumbnailIndex.jpg"
}

private fun java.time.Instant?.toSearchDateLabel(): String {
    return this?.toLocalDateTimeString().orEmpty()
}
