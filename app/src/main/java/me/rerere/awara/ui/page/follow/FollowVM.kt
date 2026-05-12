package me.rerere.awara.ui.page.follow

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.rerere.awara.data.entity.Follower
import me.rerere.awara.data.entity.Following
import me.rerere.awara.data.repo.MediaRepo
import me.rerere.awara.data.repo.UserRepo
import me.rerere.awara.data.source.onError
import me.rerere.awara.data.source.onException
import me.rerere.awara.data.source.onSuccess
import me.rerere.awara.data.source.runAPICatching
import me.rerere.awara.data.source.stringResource
import me.rerere.awara.ui.component.common.UiState

class FollowVM(
    savedStateHandle: SavedStateHandle,
    private val mediaRepo: MediaRepo,
    private val userRepo: UserRepo
) : ViewModel() {
    private val userId = checkNotNull(savedStateHandle.get<String>("userId"))
    var state by mutableStateOf(FollowState())
        private set

    init {
        loadFollowing()
        loadFollower()
    }

    fun loadNextFollowingPage() {
        if (state.followingLoadingMore || !state.followingHasMore) {
            return
        }
        loadFollowing(replaceResults = false)
    }

    fun loadFollowing(replaceResults: Boolean = true) {
        val targetPage = if (replaceResults) 1 else state.followingPage + 1
        state = state.copy(
            followingState = if (replaceResults) UiState.Loading else state.followingState,
            followingLoadingMore = !replaceResults,
        )
        viewModelScope.launch {
            runAPICatching {
                userRepo.getFollowing(userId = userId, page = targetPage - 1)
            }.onSuccess {
                val mergedList = if (replaceResults) it.results else state.followingList + it.results
                state = if(mergedList.isEmpty()) {
                    state.copy(
                        followingState = UiState.Empty,
                        followingPage = targetPage,
                        followingList = mergedList,
                        followingCount = it.count,
                        followingLoadingMore = false,
                        followingHasMore = false,
                    )
                } else {
                    state.copy(
                        followingState = UiState.Success,
                        followingPage = targetPage,
                        followingList = mergedList,
                        followingCount = it.count,
                        followingLoadingMore = false,
                        followingHasMore = mergedList.size < it.count,
                    )
                }
            }.onError {
                state = state.copy(
                    followingState = if (replaceResults) {
                        UiState.Error(
                            message = {
                                Text(text = stringResource(error = it))
                            }
                        )
                    } else {
                        state.followingState
                    },
                    followingLoadingMore = false,
                )
            }.onException {
                state = state.copy(
                    followingState = if (replaceResults) {
                        UiState.Error(
                            message = {
                                Text(text = it.exception.localizedMessage ?: "Unknown Error")
                            }
                        )
                    } else {
                        state.followingState
                    },
                    followingLoadingMore = false,
                )
            }
        }
    }

    fun loadNextFollowerPage() {
        if (state.followerLoadingMore || !state.followerHasMore) {
            return
        }
        loadFollower(replaceResults = false)
    }

    fun loadFollower(replaceResults: Boolean = true) {
        val targetPage = if (replaceResults) 1 else state.followerPage + 1
        state = state.copy(
            followerState = if (replaceResults) UiState.Loading else state.followerState,
            followerLoadingMore = !replaceResults,
        )
        viewModelScope.launch {
            runAPICatching {
                userRepo.getFollowers(userId = userId, page = targetPage - 1)
            }.onSuccess {
                val mergedList = if (replaceResults) it.results else state.followerList + it.results
                state = if(mergedList.isEmpty()) {
                    state.copy(
                        followerState = UiState.Empty,
                        followerPage = targetPage,
                        followerList = mergedList,
                        followerCount = it.count,
                        followerLoadingMore = false,
                        followerHasMore = false,
                    )
                } else {
                    state.copy(
                        followerState = UiState.Success,
                        followerPage = targetPage,
                        followerList = mergedList,
                        followerCount = it.count,
                        followerLoadingMore = false,
                        followerHasMore = mergedList.size < it.count,
                    )
                }
            }.onError {
                state = state.copy(
                    followerState = if (replaceResults) {
                        UiState.Error(
                            message = {
                                Text(text = stringResource(error = it))
                            }
                        )
                    } else {
                        state.followerState
                    },
                    followerLoadingMore = false,
                )
            }.onException {
                state = state.copy(
                    followerState = if (replaceResults) {
                        UiState.Error(
                            message = {
                                Text(text = it.exception.localizedMessage ?: "Unknown Error")
                            }
                        )
                    } else {
                        state.followerState
                    },
                    followerLoadingMore = false,
                )
            }
        }
    }

    data class FollowState(
        val followingState: UiState = UiState.Initial,
        val followingPage: Int = 1,
        val followingCount: Int = 0,
        val followingLoadingMore: Boolean = false,
        val followingHasMore: Boolean = true,
        val followingList: List<Following> = emptyList(),
        val followerState: UiState = UiState.Initial,
        val followerPage: Int = 1,
        val followerCount: Int = 0,
        val followerLoadingMore: Boolean = false,
        val followerHasMore: Boolean = true,
        val followerList: List<Follower> = emptyList()
    )
}