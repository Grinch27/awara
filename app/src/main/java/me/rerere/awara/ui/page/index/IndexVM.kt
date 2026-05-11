package me.rerere.awara.ui.page.index

// Privacy note:
// 1. Home feed initialization intentionally avoids any third-party update check.
// 2. If update prompts are needed later, decide whether they should come from GitHub Releases or an Iwara-owned endpoint.

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import me.rerere.awara.data.feed.toApiParams
import me.rerere.awara.R
import me.rerere.awara.data.dto.Notification
import me.rerere.awara.data.entity.Media
import me.rerere.awara.data.repo.MediaRepo
import me.rerere.awara.data.repo.UserRepo
import me.rerere.awara.data.source.onError
import me.rerere.awara.data.source.onException
import me.rerere.awara.data.source.onSuccess
import me.rerere.awara.data.source.runAPICatching
import me.rerere.awara.data.source.stringResource
import me.rerere.awara.domain.feed.FeedQuery
import me.rerere.awara.domain.feed.FeedScope
import me.rerere.awara.domain.feed.toFeedFilters
import me.rerere.awara.ui.component.common.UiState
import me.rerere.awara.ui.component.iwara.param.FilterValue
import me.rerere.awara.ui.component.iwara.param.sort.MediaSortOptions

private const val TAG = "IndexVM"
private val DEFAULT_MEDIA_SORT = MediaSortOptions.first().name

class IndexVM(
    private val userRepo: UserRepo,
    private val mediaRepo: MediaRepo,
) : ViewModel() {
    var state by mutableStateOf(IndexState())
        private set
    var videoSort: String by mutableStateOf(DEFAULT_MEDIA_SORT)
    val videoFilters: MutableList<FilterValue> = mutableStateListOf()
    var imageSort: String by mutableStateOf(DEFAULT_MEDIA_SORT)
    val imageFilters: MutableList<FilterValue> = mutableStateListOf()

    val events = MutableSharedFlow<IndexEvent>()

    init {
        loadSubscriptions()
        loadVideoList()
        loadImageList()
    }

    private fun buildSubscriptionQuery(): FeedQuery {
        return FeedQuery(
            scope = when (state.subscriptionType) {
                SubscriptionType.VIDEO -> FeedScope.SUBSCRIPTION_VIDEO
                SubscriptionType.IMAGE -> FeedScope.SUBSCRIPTION_IMAGE
            },
            page = state.subscriptionPage - 1,
            pageSize = 24,
        )
    }

    private fun buildVideoQuery(): FeedQuery {
        return FeedQuery(
            scope = FeedScope.HOME_VIDEO,
            sort = videoSort,
            filters = videoFilters.toFeedFilters(),
            page = state.videoPage - 1,
            pageSize = 24,
        )
    }

    private fun buildImageQuery(): FeedQuery {
        return FeedQuery(
            scope = FeedScope.HOME_IMAGE,
            sort = imageSort,
            filters = imageFilters.toFeedFilters(),
            page = state.imagePage - 1,
            pageSize = 24,
        )
    }

    fun loadSubscriptions(replaceResults: Boolean = true) {
        viewModelScope.launch {
            state = state.copy(
                subscriptionState = if (replaceResults) UiState.Loading else state.subscriptionState,
                subscriptionLoadingMore = !replaceResults,
            )
            runAPICatching {
                val param = buildSubscriptionQuery().toApiParams()
                when (state.subscriptionType) {
                    SubscriptionType.VIDEO -> mediaRepo.getVideoList(param)
                    SubscriptionType.IMAGE -> mediaRepo.getImageList(param)
                }
            }.onSuccess {
                val mergedList = if (replaceResults) {
                    it.results
                } else {
                    state.subscriptions + it.results
                }
                state = state.copy(
                    subscriptions = mergedList,
                    subscriptionTotal = it.count,
                    subscriptionState = if (mergedList.isNotEmpty()) UiState.Success else UiState.Empty,
                    subscriptionLoadingMore = false,
                    subscriptionHasMore = mergedList.size < it.count,
                )
            }.onError {
                state = state.copy(subscriptionState = UiState.Error(
                    message = {
                        Text(stringResource(error = it))
                    }
                ), subscriptionLoadingMore = false)
            }.onException {
                state = state.copy(subscriptionState = UiState.Error(message = {
                    Text(it.exception.localizedMessage ?: "Unknown Error")
                }), subscriptionLoadingMore = false)
            }
        }
    }

    fun loadCounts(userId: String) {
        viewModelScope.launch {
            launch {
                runAPICatching {
                    userRepo.getFollowingCount(userId = userId)
                }.onSuccess {
                    state = state.copy(followingCount = it)
                }.onError {
                    Log.i(TAG, "loadCounts: $it")
                }.onSuccess {
                    Log.i(TAG, "loadCounts: following count: $it")
                }.onException {
                    it.exception.printStackTrace()
                    Log.i(TAG, "loadCounts: $it")
                }
            }

            launch {
                runAPICatching {
                    userRepo.getFollowerCount(userId = userId)
                }.onSuccess {
                    state = state.copy(followerCount = it)
                }
            }

            launch {
                runAPICatching {
                    userRepo.getFriendCount(userId = userId)
                }.onSuccess {
                    state = state.copy(friendsCount = it)
                }
            }

            launch {
                runAPICatching {
                    userRepo.getNotificationCounts()
                }.onSuccess {
                    state = state.copy(notificationCounts = it)
                }
            }
        }
    }

    fun changeSubscriptionType(it: IndexVM.SubscriptionType) {
        state = state.copy(subscriptionType = it, subscriptionPage = 1)
        loadSubscriptions()
    }

    fun loadVideoList(replaceResults: Boolean = true) {
        viewModelScope.launch {
            state = state.copy(
                videoState = if (replaceResults) UiState.Loading else state.videoState,
                videoLoadingMore = !replaceResults,
            )
            runAPICatching {
                mediaRepo.getVideoList(buildVideoQuery().toApiParams())
            }.onSuccess {
                val mergedList = if (replaceResults) {
                    it.results
                } else {
                    state.videoList + it.results
                }
                state = state.copy(
                    videoList = mergedList,
                    videoCount = it.count,
                    videoState = if (mergedList.isNotEmpty()) UiState.Success else UiState.Empty,
                    videoLoadingMore = false,
                    videoHasMore = mergedList.size < it.count,
                )
            }.onError {
                state = state.copy(videoState = UiState.Error(
                    message = {
                        Text(stringResource(error = it))
                    }
                ), videoLoadingMore = false)
            }.onException {
                state = state.copy(videoState = UiState.Error(message = {
                    Text(it.exception.localizedMessage ?: "Unknown Error")
                }), videoLoadingMore = false)
            }
        }
    }

    fun loadImageList(replaceResults: Boolean = true) {
        viewModelScope.launch {
            state = state.copy(
                imageState = if (replaceResults) UiState.Loading else state.imageState,
                imageLoadingMore = !replaceResults,
            )
            runAPICatching {
                mediaRepo.getImageList(buildImageQuery().toApiParams())
            }.onSuccess {
                val mergedList = if (replaceResults) {
                    it.results
                } else {
                    state.imageList + it.results
                }
                state = state.copy(
                    imageList = mergedList,
                    imageCount = it.count,
                    imageState = if (mergedList.isNotEmpty()) UiState.Success else UiState.Empty,
                    imageLoadingMore = false,
                    imageHasMore = mergedList.size < it.count,
                )
            }.onError {
                state = state.copy(imageState = UiState.Error(
                    message = {
                        Text(stringResource(error = it))
                    }
                ), imageLoadingMore = false)
            }.onException {
                state = state.copy(imageState = UiState.Error(message = {
                    Text(it.exception.localizedMessage ?: "Unknown Error")
                }), imageLoadingMore = false)
            }
        }
    }

    fun updateVideoSort(sort: String) {
        videoSort = sort
        state = state.copy(videoPage = 1)
        loadVideoList()
    }

    fun addVideoFilter(filterValue: FilterValue) {
        videoFilters.add(filterValue)
    }

    fun removeVideoFilter(filterValue: FilterValue) {
        videoFilters.remove(filterValue)
    }

    fun updateImageSort(sort: String) {
        imageSort = sort
        state = state.copy(imagePage = 1)
        loadImageList()
    }

    fun loadNextSubscriptionPage() {
        if (state.subscriptionLoadingMore || !state.subscriptionHasMore) {
            return
        }
        state = state.copy(subscriptionPage = state.subscriptionPage + 1)
        loadSubscriptions(replaceResults = false)
    }

    fun loadNextVideoPage() {
        if (state.videoLoadingMore || !state.videoHasMore) {
            return
        }
        state = state.copy(videoPage = state.videoPage + 1)
        loadVideoList(replaceResults = false)
    }

    fun loadNextImagePage() {
        if (state.imageLoadingMore || !state.imageHasMore) {
            return
        }
        state = state.copy(imagePage = state.imagePage + 1)
        loadImageList(replaceResults = false)
    }

    fun addImageFilter(filterValue: FilterValue) {
        imageFilters.add(filterValue)
    }

    fun removeImageFilter(filterValue: FilterValue) {
        imageFilters.remove(filterValue)
    }

    fun clearImageFilter() {
        imageFilters.clear()
        state = state.copy(imagePage = 1)
    }

    fun clearVideoFilter() {
        videoFilters.clear()
        state = state.copy(videoPage = 1)
    }

    data class IndexState(
        val subscriptionState: UiState = UiState.Initial,
        val subscriptionPage: Int = 1,
        val subscriptionTotal: Int = 0,
        val subscriptionLoadingMore: Boolean = false,
        val subscriptionHasMore: Boolean = true,
        val subscriptionType: SubscriptionType = SubscriptionType.VIDEO,
        val subscriptions: List<Media> = emptyList(),
        val videoState: UiState = UiState.Initial,
        val videoPage: Int = 1,
        val videoCount: Int = 0,
        val videoLoadingMore: Boolean = false,
        val videoHasMore: Boolean = true,
        val videoList: List<Media> = emptyList(),
        val imageState: UiState = UiState.Initial,
        val imagePage: Int = 1,
        val imageCount: Int = 0,
        val imageLoadingMore: Boolean = false,
        val imageHasMore: Boolean = true,
        val imageList: List<Media> = emptyList(),
        val followingCount: Int = 0,
        val followerCount: Int = 0,
        val friendsCount: Int = 0,
        val notificationCounts: Notification = Notification()
    )

    enum class SubscriptionType(
        val id: Int
    ) {
        VIDEO(R.string.video),
        IMAGE(R.string.image),
    }

    sealed class IndexEvent {
        data class ShowUpdateDialog(val code: Int, val version: String, val changes: String) :
            IndexEvent()
    }
}

