package me.rerere.awara.ui.page.search

import me.rerere.awara.data.entity.Image
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