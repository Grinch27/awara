package me.rerere.awara.ui.page.user

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.rerere.awara.data.dto.FriendStatus
import me.rerere.awara.data.dto.ProfileDto
import me.rerere.awara.data.entity.Image
import me.rerere.awara.data.entity.Video
import me.rerere.awara.data.repo.CommentRepo
import me.rerere.awara.data.repo.MediaRepo
import me.rerere.awara.data.repo.UserRepo
import me.rerere.awara.data.source.APIResult
import me.rerere.awara.data.source.onError
import me.rerere.awara.data.source.onException
import me.rerere.awara.data.source.onSuccess
import me.rerere.awara.data.source.runAPICatching
import me.rerere.awara.ui.component.iwara.comment.CommentState
import me.rerere.awara.ui.component.iwara.comment.pop
import me.rerere.awara.ui.component.iwara.comment.push
import me.rerere.awara.ui.component.iwara.comment.updateTopStack

class UserVM(
    savedStateHandle: SavedStateHandle,
    private val userRepo: UserRepo,
    private val mediaRepo: MediaRepo,
    private val commentRepo: CommentRepo,
) : ViewModel() {
    val id = checkNotNull(savedStateHandle.get<String>("id"))
    var state by mutableStateOf(UserVMState())

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            state = state.copy(
                loading = true,
                friendLoading = true,
                guestbookCommentState = state.guestbookCommentState.copy(loading = true),
            )
            runAPICatching {
                state = state.copy(profile = userRepo.getProfile(id))
            }.onSuccess {
                loadVideoList()
                loadImageList()
                loadGuestbookComments()
                loadFriendsStatus()
            }
            state = state.copy(
                loading = false,
                friendLoading = false,
                guestbookCommentState = if (state.profile?.user == null) {
                    state.guestbookCommentState.copy(loading = false)
                } else {
                    state.guestbookCommentState
                },
            )
        }
    }

    fun loadNextGuestbookPage() {
        val currentCommentState = state.guestbookCommentState.stack.last()
        if (currentCommentState.loadingMore || !currentCommentState.hasMore) {
            return
        }
        loadGuestbookComments(replaceResults = false)
    }

    fun pushGuestbookComment(id: String) {
        state = state.copy(
            guestbookCommentState = state.guestbookCommentState.push(id)
        )
        loadGuestbookComments()
    }

    fun popGuestbookComment() {
        if (state.guestbookCommentState.stack.size <= 1) {
            return
        }
        state = state.copy(
            guestbookCommentState = state.guestbookCommentState.pop().copy(loading = false),
            guestbookError = null,
            guestbookExceptionMessage = null,
        )
    }

    fun loadGuestbookComments(replaceResults: Boolean = true) {
        val profileId = state.profile?.user?.id?.takeIf { it.isNotBlank() } ?: run {
            state = state.copy(
                guestbookCommentState = state.guestbookCommentState.copy(loading = false),
            )
            return
        }
        val currentCommentState = state.guestbookCommentState.stack.last()
        val targetPage = if (replaceResults) 1 else currentCommentState.page + 1
        state = state.copy(
            guestbookCommentState = state.guestbookCommentState
                .copy(loading = replaceResults)
                .updateTopStack(currentCommentState.copy(loadingMore = !replaceResults)),
            guestbookError = null,
            guestbookExceptionMessage = null,
        )
        viewModelScope.launch {
            runAPICatching {
                if (currentCommentState.parent != null) {
                    commentRepo.getProfileCommentReplies(
                        profileId,
                        targetPage - 1,
                        currentCommentState.parent,
                    )
                } else {
                    commentRepo.getProfileComments(profileId, targetPage - 1)
                }
            }.onSuccess {
                val activeCommentState = state.guestbookCommentState.stack.last()
                if (activeCommentState.parent == currentCommentState.parent) {
                    val mergedComments = if (replaceResults) {
                        it.results
                    } else {
                        activeCommentState.comments + it.results
                    }
                    state = state.copy(
                        guestbookCommentState = state.guestbookCommentState.updateTopStack(
                            activeCommentState.copy(
                                page = targetPage,
                                comments = mergedComments,
                                limit = it.limit,
                                total = it.count,
                                loadingMore = false,
                                hasMore = mergedComments.size < it.count,
                            )
                        ),
                    )
                }
            }.onError {
                val activeCommentState = state.guestbookCommentState.stack.last()
                if (activeCommentState.parent == currentCommentState.parent) {
                    state = state.copy(
                        guestbookError = it,
                        guestbookCommentState = state.guestbookCommentState.updateTopStack(
                            activeCommentState.copy(loadingMore = false)
                        ),
                    )
                }
            }.onException {
                val activeCommentState = state.guestbookCommentState.stack.last()
                if (activeCommentState.parent == currentCommentState.parent) {
                    state = state.copy(
                        guestbookExceptionMessage = it.exception.localizedMessage,
                        guestbookCommentState = state.guestbookCommentState.updateTopStack(
                            activeCommentState.copy(loadingMore = false)
                        ),
                    )
                }
            }
            if (state.guestbookCommentState.stack.last().parent == currentCommentState.parent) {
                state = state.copy(
                    guestbookCommentState = state.guestbookCommentState.copy(loading = false),
                )
            }
        }
    }

    private suspend fun loadFriendsStatus() {
        runAPICatching {
            userRepo.getFriendsStatus(state.profile?.user?.id ?: "")
        }.onSuccess {
            state = state.copy(friendStatus = FriendStatus.parse(it.status))
        }
    }

    fun followOrUnfollow() {
        if (state.followLoading) {
            return
        }

        viewModelScope.launch {
            state = state.copy(followLoading = true)
            runAPICatching {
                if (state.profile?.user?.following == true) {
                    userRepo.unfollowUser(state.profile?.user?.id ?: "")
                } else {
                    userRepo.followUser(state.profile?.user?.id ?: "")
                }
                state = state.copy(profile = userRepo.getProfile(id))
            }
            state = state.copy(followLoading = false)
        }
    }

    fun loadNextVideoPage() {
        if (state.videoLoadingMore || !state.videoHasMore) {
            return
        }
        loadVideoList(replaceResults = false)
    }

    fun loadVideoList(replaceResults: Boolean = true) {
        val targetPage = if (replaceResults) 1 else state.videoPage + 1
        state = state.copy(
            videoLoading = replaceResults,
            videoLoadingMore = !replaceResults,
        )
        viewModelScope.launch {
            runAPICatching {
                val result = mediaRepo.getVideoList(
                    mapOf(
                        "page" to (targetPage - 1).toString(),
                        "sort" to "date",
                        "user" to (state.profile?.user?.id ?: ""),
                        "limit" to "32"
                    )
                )
                val mergedList = if (replaceResults) result.results else state.videoList + result.results
                state = state.copy(
                    videoPage = targetPage,
                    videoCount = result.count,
                    videoList = mergedList,
                    videoHasMore = mergedList.size < result.count,
                )
            }
            state = state.copy(videoLoading = false, videoLoadingMore = false)
        }
    }

    fun loadNextImagePage() {
        if (state.imageLoadingMore || !state.imageHasMore) {
            return
        }
        loadImageList(replaceResults = false)
    }

    fun loadImageList(replaceResults: Boolean = true) {
        val targetPage = if (replaceResults) 1 else state.imagePage + 1
        state = state.copy(
            imageLoading = replaceResults,
            imageLoadingMore = !replaceResults,
        )
        viewModelScope.launch {
            runAPICatching {
                val result = mediaRepo.getImageList(
                    mapOf(
                        "page" to (targetPage - 1).toString(),
                        "sort" to "date",
                        "user" to (state.profile?.user?.id ?: ""),
                        "limit" to "32",
                    )
                )
                val mergedList = if (replaceResults) result.results else state.imageList + result.results
                state = state.copy(
                    imagePage = targetPage,
                    imageCount = result.count,
                    imageList = mergedList,
                    imageHasMore = mergedList.size < result.count,
                )
            }
            state = state.copy(imageLoading = false, imageLoadingMore = false)
        }
    }

    fun addOrRemoveFriend() {
        viewModelScope.launch {
            state = state.copy(friendLoading = true)
            runAPICatching {
                if (state.friendStatus == FriendStatus.NONE) {
                    userRepo.addFriend(state.profile?.user?.id ?: "")
                } else {
                    userRepo.removeFriend(state.profile?.user?.id ?: "")
                }
                loadFriendsStatus()
            }
            state = state.copy(friendLoading = false)
        }
    }

    data class UserVMState(
        val loading: Boolean = false,
        val followLoading: Boolean = false,
        val profile: ProfileDto? = null,
        val friendStatus: FriendStatus = FriendStatus.NONE,
        val friendLoading: Boolean = false,
        val guestbookCommentState: CommentState = CommentState(),
        val guestbookError: APIResult.Error? = null,
        val guestbookExceptionMessage: String? = null,
        val videoPage: Int = 1,
        val videoCount: Int = 0,
        val videoLoading: Boolean = false,
        val videoLoadingMore: Boolean = false,
        val videoHasMore: Boolean = true,
        val videoList: List<Video> = emptyList(),
        val imagePage: Int = 1,
        val imageCount: Int = 0,
        val imageLoading: Boolean = false,
        val imageLoadingMore: Boolean = false,
        val imageHasMore: Boolean = true,
        val imageList: List<Image> = emptyList(),
    )
}