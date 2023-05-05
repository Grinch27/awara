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

    fun loadFriends() {
        viewModelScope.launch {
            state = state.copy(friendsUiState = UiState.Loading)
            runAPICatching {
                userRepo.getUserFriends(userId, state.friendsPage - 1)
            }.onSuccess {
                state = state.copy(
                    friendsUiState = if (it.results.isNotEmpty()) UiState.Success else UiState.Empty,
                    friendsData = it.results,
                    friendsCount = it.count,
                )
            }.onError {
                state = state.copy(friendsUiState = UiState.Error(
                    message = {
                        Text(stringResource(error = it))
                    }
                ))
            }.onException {
                state = state.copy(friendsUiState = UiState.Error(message = {
                    Text(it.exception.localizedMessage ?: "Unknown Error")
                }))
            }
        }
    }

    fun loadRequests() {
        if (!self) return
        viewModelScope.launch {
            state = state.copy(requestsUiState = UiState.Loading)
            runAPICatching {
                userRepo.getFriendRequests(userId, state.requestsPage - 1)
            }.onSuccess {
                state = state.copy(
                    requestsUiState = if (it.results.isNotEmpty()) UiState.Success else UiState.Empty,
                    requestsData = it.results,
                    requestsCount = it.count,
                )
            }.onError {
                state = state.copy(requestsUiState = UiState.Error(
                    message = {
                        Text(stringResource(error = it))
                    }
                ))
            }.onException {
                state = state.copy(requestsUiState = UiState.Error(message = {
                    Text(it.exception.localizedMessage ?: "Unknown Error")
                }))
            }
        }
    }

    fun addFriends(userId: String) {
        viewModelScope.launch {
            state = state.copy(loadingFriendChange = true)
            kotlin.runCatching {
                userRepo.addFriend(userId)
            }.onSuccess {
                loadFriends()
                loadRequests()
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
                loadRequests()
            }
            state = state.copy(loadingFriendChange = false)
        }
    }

    fun jumpFriendsPage(page: Int) {
        state = state.copy(friendsPage = page)
        loadFriends()
    }

    fun jumpRequestsPage(it: Int) {
        state = state.copy(requestsPage = it)
        loadRequests()
    }

    data class FriendsState(
        val friendsUiState: UiState = UiState.Initial,
        val friendsPage: Int = 1,
        val friendsCount: Int = 0,
        val friendsData: List<User> = emptyList(),
        val requestsUiState: UiState = UiState.Initial,
        val requestsPage: Int = 1,
        val requestsCount: Int = 0,
        val requestsData: List<FriendRequestDto> = emptyList(),
        val loadingFriendChange: Boolean = false
    )
}