package me.rerere.awara.ui.page.search

// TODO(user): Decide whether saved feed views should be usable as search presets in the first rollout or wait for a dedicated saved-view selector.
// TODO(agent): If user profile search gains typed filters later, move the remaining raw string branch onto the same FeedQuery pipeline instead of keeping two query styles.

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.rerere.awara.data.feed.toApiParams
import me.rerere.awara.data.entity.Image
import me.rerere.awara.data.entity.User
import me.rerere.awara.data.entity.Video
import me.rerere.awara.data.repo.MediaRepo
import me.rerere.awara.data.source.onError
import me.rerere.awara.data.source.onException
import me.rerere.awara.data.source.runAPICatching
import me.rerere.awara.data.source.stringResource
import me.rerere.awara.domain.feed.FeedQuery
import me.rerere.awara.domain.feed.FeedScope
import me.rerere.awara.domain.feed.toFeedFilters
import me.rerere.awara.ui.component.common.UiState
import me.rerere.awara.ui.component.iwara.param.FilterValue
import me.rerere.awara.ui.component.iwara.param.sort.MediaSortOptions

private val DEFAULT_MEDIA_SORT = MediaSortOptions.first().name

class SearchVM(
    private val mediaRepo: MediaRepo
) : ViewModel() {
    var state by mutableStateOf(SearchState())
        private set
    var query by mutableStateOf("")
    var videoSort: String by mutableStateOf(DEFAULT_MEDIA_SORT)
    val videoFilters: MutableList<FilterValue> = mutableStateListOf()
    var imageSort: String by mutableStateOf(DEFAULT_MEDIA_SORT)
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
        viewModelScope.launch {
            state = state.copy(
                uiState = if (replaceResults) UiState.Loading else state.uiState,
                loadingMore = !replaceResults,
            )
            runAPICatching {
                when (state.searchType) {
                    "video" -> {
                        val pager = mediaRepo.getVideoList(
                            buildMediaSearchQuery(FeedScope.SEARCH_VIDEO).toApiParams()
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
                        val pager = mediaRepo.getImageList(
                            buildMediaSearchQuery(FeedScope.SEARCH_IMAGE).toApiParams()
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
                        val pager = mediaRepo.searchUser(query, state.page - 1)
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

                    else -> {}
                }
            }.onError {
                state = state.copy(uiState = UiState.Error(
                    message = {
                        Text(stringResource(error = it))
                    }
                ))
            }.onException {
                state = state.copy(uiState = UiState.Error(
                    message = {
                        Text(it.exception.localizedMessage ?: "Unknown Error")
                    }
                ))
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

    fun jumpToPage(page: Int) {
        state = state.copy(page = page)
        search()
    }

    fun updateSearchType(type: String) {
        state = state.copy(searchType = type)
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
        videoFilters.add(filterValue)
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
        imageFilters.add(filterValue)
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
        val videoList: List<Video> = emptyList(),
        val imageList: List<Image> = emptyList(),
        val userList: List<User> = emptyList(),
    )
}