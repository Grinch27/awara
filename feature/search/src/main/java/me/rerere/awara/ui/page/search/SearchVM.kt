package me.rerere.awara.ui.page.search

// TODO(user): Decide whether saved feed views should be usable as search presets in the first rollout or wait for a dedicated saved-view selector.
// TODO(agent): If user profile search gains typed filters later, move the remaining raw string branch onto the same FeedQuery pipeline instead of keeping two query styles.

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.rerere.awara.domain.feed.FeedQuery
import me.rerere.awara.domain.feed.FeedFilter
import me.rerere.awara.domain.feed.FeedScope
import me.rerere.awara.domain.feed.toFeedFilters
import me.rerere.awara.ui.component.common.UiState
import me.rerere.awara.ui.component.iwara.param.FilterValue
import me.rerere.awara.ui.component.iwara.param.rating.DEFAULT_MEDIA_RATING
import me.rerere.awara.ui.component.iwara.param.sort.DEFAULT_MEDIA_SORT

class SearchVM(
    private val searchRepository: SearchRepository,
) : ViewModel() {
    var state by mutableStateOf(SearchState())
        private set
    var query by mutableStateOf("")
    var videoSort by mutableStateOf(DEFAULT_MEDIA_SORT)
    var videoRating by mutableStateOf(DEFAULT_MEDIA_RATING)
    val videoFilters: MutableList<FilterValue> = mutableStateListOf()
    var imageSort by mutableStateOf(DEFAULT_MEDIA_SORT)
    var imageRating by mutableStateOf(DEFAULT_MEDIA_RATING)
    val imageFilters: MutableList<FilterValue> = mutableStateListOf()
    private var defaultRatingApplied = false

    private fun hasActiveSearchCriteria(type: String = state.searchType): Boolean {
        return when (type) {
            SearchTypes.VIDEO -> query.isNotBlank() || videoFilters.isNotEmpty() || videoRating != DEFAULT_MEDIA_RATING || videoSort != DEFAULT_MEDIA_SORT
            SearchTypes.IMAGE -> query.isNotBlank() || imageFilters.isNotEmpty() || imageRating != DEFAULT_MEDIA_RATING || imageSort != DEFAULT_MEDIA_SORT
            else -> query.isNotBlank()
        }
    }

    private fun mediaFilters(scope: FeedScope): List<FeedFilter> {
        val baseFilters = when (scope) {
            FeedScope.SEARCH_IMAGE -> imageFilters.toFeedFilters()
            else -> videoFilters.toFeedFilters()
        }.filterNot { filter ->
            filter is FeedFilter.KeyValue && filter.key == "rating"
        }
        val rating = when (scope) {
            FeedScope.SEARCH_IMAGE -> imageRating
            else -> videoRating
        }
        return baseFilters + FeedFilter.KeyValue("rating", rating)
    }

    private fun buildMediaSearchQuery(scope: FeedScope): FeedQuery {
        return FeedQuery(
            scope = scope,
            keyword = query,
            sort = when (scope) {
                FeedScope.SEARCH_IMAGE -> imageSort
                else -> videoSort
            },
            filters = mediaFilters(scope),
            page = state.page - 1,
            pageSize = 24,
        )
    }

    fun applyDefaultRating(rating: String) {
        if (defaultRatingApplied) {
            return
        }
        videoRating = rating
        imageRating = rating
        defaultRatingApplied = true
    }

    fun searchByTag(type: String, tag: String) {
        val targetType = if (SearchTypes.normalize(type) == SearchTypes.IMAGE) SearchTypes.IMAGE else SearchTypes.VIDEO
        val tagFilter = FilterValue("tags", tag)
        query = ""
        when (targetType) {
            SearchTypes.IMAGE -> {
                imageFilters.clear()
                imageFilters.add(tagFilter)
            }

            else -> {
                videoFilters.clear()
                videoFilters.add(tagFilter)
            }
        }
        state = state.copy(searchType = targetType, page = 1, hasMore = true)
        search()
    }

    fun submitSearch() {
        query = query.trim()
        state = state.copy(page = 1, hasMore = true)
        search()
    }

    fun search(replaceResults: Boolean = true) {
        val requestedPage = state.page
        val previousUiState = state.uiState
        viewModelScope.launch {
            state = state.copy(
                uiState = if (replaceResults) UiState.Loading else state.uiState,
                loadingMore = !replaceResults,
            )
            runCatching {
                when (state.searchType) {
                    SearchTypes.VIDEO -> {
                        val pager = searchRepository.searchVideos(
                            buildMediaSearchQuery(FeedScope.SEARCH_VIDEO),
                        )
                        val mergedList = if (replaceResults) {
                            pager.results
                        } else {
                            state.videoList + pager.results
                        }
                        state = state.copy(
                            uiState = if (mergedList.isNotEmpty()) UiState.Success else UiState.Empty,
                            count = pager.count,
                            videoList = mergedList,
                            loadingMore = false,
                            hasMore = mergedList.size < pager.count,
                        )
                    }

                    SearchTypes.IMAGE -> {
                        val pager = searchRepository.searchImages(
                            buildMediaSearchQuery(FeedScope.SEARCH_IMAGE),
                        )
                        val mergedList = if (replaceResults) {
                            pager.results
                        } else {
                            state.imageList + pager.results
                        }
                        state = state.copy(
                            uiState = if (mergedList.isNotEmpty()) UiState.Success else UiState.Empty,
                            count = pager.count,
                            imageList = mergedList,
                            loadingMore = false,
                            hasMore = mergedList.size < pager.count,
                        )
                    }

                    SearchTypes.USER -> {
                        val pager = searchRepository.searchUsers(query, state.page - 1)
                        val mergedList = if (replaceResults) {
                            pager.results
                        } else {
                            state.userList + pager.results
                        }
                        state = state.copy(
                            uiState = if (mergedList.isNotEmpty()) UiState.Success else UiState.Empty,
                            count = pager.count,
                            userList = mergedList,
                            loadingMore = false,
                            hasMore = mergedList.size < pager.count,
                        )
                    }

                    SearchTypes.POST -> {
                        val pager = searchRepository.searchPosts(query, state.page - 1)
                        val mergedList = if (replaceResults) {
                            pager.results
                        } else {
                            state.postList + pager.results
                        }
                        state = state.copy(
                            uiState = if (mergedList.isNotEmpty()) UiState.Success else UiState.Empty,
                            count = pager.count,
                            postList = mergedList,
                            loadingMore = false,
                            hasMore = mergedList.size < pager.count,
                        )
                    }

                    SearchTypes.PLAYLIST -> {
                        val pager = searchRepository.searchPlaylists(query, state.page - 1)
                        val mergedList = if (replaceResults) {
                            pager.results
                        } else {
                            state.playlistList + pager.results
                        }
                        state = state.copy(
                            uiState = if (mergedList.isNotEmpty()) UiState.Success else UiState.Empty,
                            count = pager.count,
                            playlistList = mergedList,
                            loadingMore = false,
                            hasMore = mergedList.size < pager.count,
                        )
                    }

                    SearchTypes.FORUM_POST -> {
                        val pager = searchRepository.searchForumPosts(query, state.page - 1)
                        val mergedList = if (replaceResults) {
                            pager.results
                        } else {
                            state.forumPostList + pager.results
                        }
                        state = state.copy(
                            uiState = if (mergedList.isNotEmpty()) UiState.Success else UiState.Empty,
                            count = pager.count,
                            forumPostList = mergedList,
                            loadingMore = false,
                            hasMore = mergedList.size < pager.count,
                        )
                    }

                    SearchTypes.FORUM_THREAD -> {
                        val pager = searchRepository.searchForumThreads(query, state.page - 1)
                        val mergedList = if (replaceResults) {
                            pager.results
                        } else {
                            state.forumThreadList + pager.results
                        }
                        state = state.copy(
                            uiState = if (mergedList.isNotEmpty()) UiState.Success else UiState.Empty,
                            count = pager.count,
                            forumThreadList = mergedList,
                            loadingMore = false,
                            hasMore = mergedList.size < pager.count,
                        )
                    }
                }
            }.onFailure { throwable ->
                state = state.copy(
                    page = if (replaceResults) 1 else (requestedPage - 1).coerceAtLeast(1),
                    uiState = if (replaceResults) {
                        UiState.Error(
                            throwable = throwable,
                            messageText = throwable.localizedMessage ?: "Unknown Error",
                        )
                    } else {
                        previousUiState
                    },
                    loadingMore = false,
                )
            }
        }
    }

    fun loadNextPage() {
        if (state.loadingMore || !state.hasMore) {
            return
        }
        state = state.copy(page = state.page + 1, loadingMore = true)
        search(replaceResults = false)
    }

    fun updateSearchType(type: String) {
        val normalizedType = SearchTypes.normalize(type)
        state = state.copy(
            searchType = normalizedType,
            page = 1,
            count = 0,
            loadingMore = false,
            hasMore = true,
            uiState = UiState.Initial,
        )
        if (hasActiveSearchCriteria(normalizedType)) {
            search()
        }
    }

    fun updateVideoSort(sort: String) {
        videoSort = sort
        state = state.copy(page = 1, hasMore = true)
        if (state.searchType == SearchTypes.VIDEO) {
            search()
        }
    }

    fun updateVideoRating(rating: String) {
        videoRating = rating
        state = state.copy(page = 1, hasMore = true)
        if (state.searchType == SearchTypes.VIDEO) {
            search()
        }
    }

    fun addVideoFilter(filterValue: FilterValue) {
        if (filterValue !in videoFilters) {
            videoFilters.add(filterValue)
        }
    }

    fun removeVideoFilter(filterValue: FilterValue) {
        videoFilters.remove(filterValue)
    }

    fun clearVideoFilter() {
        videoFilters.clear()
    }

    fun updateImageSort(sort: String) {
        imageSort = sort
        state = state.copy(page = 1, hasMore = true)
        if (state.searchType == SearchTypes.IMAGE) {
            search()
        }
    }

    fun updateImageRating(rating: String) {
        imageRating = rating
        state = state.copy(page = 1, hasMore = true)
        if (state.searchType == SearchTypes.IMAGE) {
            search()
        }
    }

    fun addImageFilter(filterValue: FilterValue) {
        if (filterValue !in imageFilters) {
            imageFilters.add(filterValue)
        }
    }

    fun removeImageFilter(filterValue: FilterValue) {
        imageFilters.remove(filterValue)
    }

    fun clearImageFilter() {
        imageFilters.clear()
    }

    data class SearchState(
        val uiState: UiState = UiState.Initial,
        val searchType: String = "video",
        val page: Int = 1,
        val count: Int = 0,
        val loadingMore: Boolean = false,
        val hasMore: Boolean = true,
        val videoList: List<SearchMediaItem> = emptyList(),
        val imageList: List<SearchMediaItem> = emptyList(),
        val userList: List<SearchUserItem> = emptyList(),
        val postList: List<SearchPostItem> = emptyList(),
        val playlistList: List<SearchPlaylistItem> = emptyList(),
        val forumPostList: List<SearchForumPostItem> = emptyList(),
        val forumThreadList: List<SearchForumThreadItem> = emptyList(),
    )
}