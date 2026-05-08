package me.rerere.awara.ui.page.search

// TODO(user): Decide whether saved feed views should be usable as search presets in the first rollout or wait for a dedicated saved-view selector.
// TODO(agent): If user profile search gains typed filters later, move the remaining raw string branch onto the same FeedQuery pipeline instead of keeping two query styles.

import androidx.compose.material3.Text
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
import me.rerere.awara.ui.component.common.UiState

class SearchVM(
    private val mediaRepo: MediaRepo
) : ViewModel() {
    var state by mutableStateOf(SearchState())
        private set
    var query by mutableStateOf("")

    private fun buildMediaSearchQuery(scope: FeedScope): FeedQuery {
        return FeedQuery(
            scope = scope,
            keyword = query,
            page = state.page - 1,
            pageSize = 24,
        )
    }

    fun search() {
        viewModelScope.launch {
            state = state.copy(uiState = UiState.Loading)
            runAPICatching {
                when (state.searchType) {
                    "video" -> {
                        val pager = mediaRepo.getVideoList(
                            buildMediaSearchQuery(FeedScope.SEARCH_VIDEO).toApiParams()
                        )
                        state = if (pager.results.isEmpty()) {
                            state.copy(
                                uiState = UiState.Empty,
                                count = pager.count,
                                videoList = emptyList()
                            )
                        } else {
                            state.copy(
                                uiState = UiState.Success,
                                count = pager.count,
                                videoList = pager.results
                            )
                        }
                    }

                    "image" -> {
                        val pager = mediaRepo.getImageList(
                            buildMediaSearchQuery(FeedScope.SEARCH_IMAGE).toApiParams()
                        )
                        state = if (pager.results.isEmpty()) {
                            state.copy(
                                uiState = UiState.Empty,
                                count = pager.count,
                                imageList = emptyList()
                            )
                        } else {
                            state.copy(
                                uiState = UiState.Success,
                                count = pager.count,
                                imageList = pager.results
                            )
                        }
                    }

                    "user" -> {
                        val pager = mediaRepo.searchUser(query, state.page - 1)
                        state = if (pager.results.isEmpty()) {
                            state.copy(
                                uiState = UiState.Empty,
                                count = pager.count,
                                userList = emptyList()
                            )
                        } else {
                            state.copy(
                                uiState = UiState.Success,
                                count = pager.count,
                                userList = pager.results
                            )
                        }
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

    data class SearchState(
        val uiState: UiState = UiState.Initial,
        val searchType: String = "video",
        val page: Int = 1,
        val count: Int = 0,
        val videoList: List<Video> = emptyList(),
        val imageList: List<Image> = emptyList(),
        val userList: List<User> = emptyList(),
    )
}