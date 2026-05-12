package me.rerere.awara.ui.page.friends

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.rerere.awara.data.dto.FriendRequestDto
import me.rerere.awara.data.entity.User
import me.rerere.awara.data.repo.UserRepo
import me.rerere.awara.data.source.onError
import me.rerere.awara.data.source.onException
import me.rerere.awara.data.source.onSuccess
import me.rerere.awara.data.source.runAPICatching
import me.rerere.awara.data.source.stringResource
import me.rerere.awara.ui.component.common.UiState

class FriendsVM(
    savedStateHandle: SavedStateHandle,
    private val userRepo: UserRepo
) : ViewModel() {
    private val userId = checkNotNull(savedStateHandle.get<String>("userId"))
    val self = checkNotNull(savedStateHandle.get<Boolean>("self"))
    var state by mutableStateOf(FriendsState())
        private set

    init {
        loadFriends()
        loadRequests()
    }

    fun loadFriends(replaceResults: Boolean = true) {
        val targetPage = if (replaceResults) 1 else state.friendsPage + 1
        state = state.copy(
            friendsUiState = if (replaceResults) UiState.Loading else state.friendsUiState,
            friendsLoadingMore = !replaceResults,
        )
        viewModelScope.launch {
            runAPICatching {
                userRepo.getUserFriends(userId, targetPage - 1)
            }.onSuccess {
                val mergedList = if (replaceResults) it.results else state.friendsData + it.results
                state = state.copy(
                    friendsUiState = if (mergedList.isNotEmpty()) UiState.Success else UiState.Empty,
                    friendsPage = targetPage,
                    friendsData = mergedList,
                    friendsCount = it.count,
                    friendsLoadingMore = false,
                    friendsHasMore = mergedList.size < it.count,
                )
            }.onError {
                state = state.copy(
                    friendsUiState = if (replaceResults) {
                        UiState.Error(
                            message = {
                                Text(stringResource(error = it))
                            }
                        )
                    } else {
                        state.friendsUiState
                    },
                    friendsLoadingMore = false,
                )
            }.onException {
                state = state.copy(
                    friendsUiState = if (replaceResults) {
                        UiState.Error(message = {
                            Text(it.exception.localizedMessage ?: "Unknown Error")
                        })
                    } else {
                        state.friendsUiState
                    },
                    friendsLoadingMore = false,
                )
            }
        }
    }

    fun loadNextFriendsPage() {
        if (state.friendsLoadingMore || !state.friendsHasMore) {
            return
        }
        loadFriends(replaceResults = false)
    }

    fun loadRequests(replaceResults: Boolean = true) {
        if (!self) return
        val targetPage = if (replaceResults) 1 else state.requestsPage + 1
        state = state.copy(
            requestsUiState = if (replaceResults) UiState.Loading else state.requestsUiState,
            requestsLoadingMore = !replaceResults,
        )
        viewModelScope.launch {
            runAPICatching {
                userRepo.getFriendRequests(userId, targetPage - 1)
            }.onSuccess {
                val mergedList = if (replaceResults) it.results else state.requestsData + it.results
                state = state.copy(
                    requestsUiState = if (mergedList.isNotEmpty()) UiState.Success else UiState.Empty,
                    requestsPage = targetPage,
                    requestsData = mergedList,
                    requestsCount = it.count,
                    requestsLoadingMore = false,
                    requestsHasMore = mergedList.size < it.count,
                )
            }.onError {
                state = state.copy(
                    requestsUiState = if (replaceResults) {
                        UiState.Error(
                            message = {
                                Text(stringResource(error = it))
                            }
                        )
                    } else {
                        state.requestsUiState
                    },
                    requestsLoadingMore = false,
                )
            }.onException {
                state = state.copy(
                    requestsUiState = if (replaceResults) {
                        UiState.Error(message = {
                            Text(it.exception.localizedMessage ?: "Unknown Error")
                        })
                    } else {
                        state.requestsUiState
                    },
                    requestsLoadingMore = false,
                )
            }
        }
    }

    fun loadNextRequestsPage() {
        if (state.requestsLoadingMore || !state.requestsHasMore) {
            return
        }
        loadRequests(replaceResults = false)
    }

    fun addFriends(userId: String) {
        viewModelScope.launch {
            state = state.copy(loadingFriendChange = true)
            kotlin.runCatching {
                userRepo.addFriend(userId)
            }.onSuccess {
                loadFriends(replaceResults = true)
                loadRequests(replaceResults = true)
            }
            state = state.copy(loadingFriendChange = false)
        }
    }

    fun removeFriends(userId: String) {
        viewModelScope.launch {
            state = state.copy(loadingFriendChange = true)
            kotlin.runCatching {
                userRepo.removeFriend(userId)
            }.onSuccess {
                loadFriends(replaceResults = true)
                loadRequests(replaceResults = true)
            }
            state = state.copy(loadingFriendChange = false)
        }
    }

    data class FriendsState(
        val friendsUiState: UiState = UiState.Initial,
        val friendsPage: Int = 1,
        val friendsCount: Int = 0,
        val friendsLoadingMore: Boolean = false,
        val friendsHasMore: Boolean = true,
        val friendsData: List<User> = emptyList(),
        val requestsUiState: UiState = UiState.Initial,
        val requestsPage: Int = 1,
        val requestsCount: Int = 0,
        val requestsLoadingMore: Boolean = false,
        val requestsHasMore: Boolean = true,
        val requestsData: List<FriendRequestDto> = emptyList(),
        val loadingFriendChange: Boolean = false
    )
}