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
import me.rerere.awara.domain.feed.FeedScope
import me.rerere.awara.domain.feed.toFeedFilters
import me.rerere.awara.ui.component.common.UiState
import me.rerere.awara.ui.component.iwara.param.FilterValue
import me.rerere.awara.ui.component.iwara.param.sort.DEFAULT_MEDIA_SORT

class SearchVM(
    private val searchRepository: SearchRepository,
) : ViewModel() {
    var state by mutableStateOf(SearchState())
        private set
    var query by mutableStateOf("")
    var videoSort by mutableStateOf(DEFAULT_MEDIA_SORT)
    val videoFilters: MutableList<FilterValue> = mutableStateListOf()
    var imageSort by mutableStateOf(DEFAULT_MEDIA_SORT)
    val imageFilters: MutableList<FilterValue> = mutableStateListOf()

    private fun buildMediaSearchQuery(scope: FeedScope): FeedQuery {
        return FeedQuery(
            scope = scope,
            keyword = query,
            sort = when (scope) {
                FeedScope.SEARCH_IMAGE -> imageSort
                else -> videoSort
            },
            filters = when (scope) {
                FeedScope.SEARCH_IMAGE -> imageFilters.toFeedFilters()
                else -> videoFilters.toFeedFilters()
            },
            page = state.page - 1,
            pageSize = 24,
        )
    }

    fun submitSearch() {
        query = query.trim()
        state = state.copy(page = 1)
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
                    "video" -> {
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

                    "image" -> {
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

                    "user" -> {
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
        if (state.loadingMore || !state.hasMore || query.isBlank()) {
            return
        }
        state = state.copy(page = state.page + 1)
        search(replaceResults = false)
    }

    fun updateSearchType(type: String) {
        state = state.copy(searchType = type, page = 1)
        if (query.isNotBlank()) {
            search()
        }
    }

    fun updateVideoSort(sort: String) {
        videoSort = sort
        state = state.copy(page = 1)
        if (state.searchType == "video" && query.isNotBlank()) {
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
        state = state.copy(page = 1)
        if (state.searchType == "image" && query.isNotBlank()) {
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
    )
}