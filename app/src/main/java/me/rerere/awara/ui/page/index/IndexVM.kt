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
import me.rerere.awara.data.repo.SavedFeedViewRepo
import me.rerere.awara.data.repo.UserRepo
import me.rerere.awara.data.source.onError
import me.rerere.awara.data.source.onException
import me.rerere.awara.data.source.onSuccess
import me.rerere.awara.data.source.runAPICatching
import me.rerere.awara.data.source.stringResource
import me.rerere.awara.domain.feed.FeedQuery
import me.rerere.awara.domain.feed.FeedScope
import me.rerere.awara.domain.feed.SavedFeedView
import me.rerere.awara.domain.feed.toFeedFilters
import me.rerere.awara.ui.component.common.UiState
import me.rerere.awara.ui.component.iwara.param.FilterValue
import me.rerere.awara.ui.component.iwara.param.sort.MediaSortOptions
import java.time.Instant
import java.util.UUID

private const val TAG = "IndexVM"

class IndexVM(
    private val userRepo: UserRepo,
    private val mediaRepo: MediaRepo,
    private val savedFeedViewRepo: SavedFeedViewRepo,
) : ViewModel() {
    var state by mutableStateOf(IndexState())
        private set
    var videoSort: String by mutableStateOf(MediaSortOptions.first().name)
    val videoFilters: MutableList<FilterValue> = mutableStateListOf()
    var imageSort: String = MediaSortOptions.first().name
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

    suspend fun saveCurrentVideoView(name: String): SavedFeedView {
        return saveCurrentView(
            name = name,
            scope = FeedScope.HOME_VIDEO,
            sort = videoSort,
            filters = videoFilters.toFeedFilters(),
        )
    }

    suspend fun saveCurrentImageView(name: String): SavedFeedView {
        return saveCurrentView(
            name = name,
            scope = FeedScope.HOME_IMAGE,
            sort = imageSort,
            filters = imageFilters.toFeedFilters(),
        )
    }

    private suspend fun saveCurrentView(
        name: String,
        scope: FeedScope,
        sort: String?,
        filters: List<me.rerere.awara.domain.feed.FeedFilter>,
    ): SavedFeedView {
        val trimmedName = name.trim().ifBlank {
            when (scope) {
                FeedScope.HOME_VIDEO -> "Video View ${Instant.now()}"
                FeedScope.HOME_IMAGE -> "Image View ${Instant.now()}"
                else -> "Saved View ${Instant.now()}"
            }
        }
        val view = SavedFeedView(
            id = UUID.randomUUID().toString(),
            name = trimmedName,
            scope = scope,
            sort = sort,
            filters = filters,
        )
        savedFeedViewRepo.save(view)
        return view
    }

    fun loadSubscriptions() {
        viewModelScope.launch {
            state = state.copy(subscriptionState = UiState.Loading)
            runAPICatching {
                val param = buildSubscriptionQuery().toApiParams()
                when (state.subscriptionType) {
                    SubscriptionType.VIDEO -> mediaRepo.getVideoList(param)
                    SubscriptionType.IMAGE -> mediaRepo.getImageList(param)
                }
            }.onSuccess {
                state = state.copy(
                    subscriptions = it.results,
                    subscriptionTotal = it.count,
                    subscriptionState = if (it.results.isNotEmpty()) UiState.Success else UiState.Empty,
                )
            }.onError {
                state = state.copy(subscriptionState = UiState.Error(
                    message = {
                        Text(stringResource(error = it))
                    }
                ))
            }.onException {
                state = state.copy(subscriptionState = UiState.Error(message = {
                    Text(it.exception.localizedMessage ?: "Unknown Error")
                }))
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

    fun jumpToSubscriptionPage(page: Int) {
        if (page == state.subscriptionPage || page < 1) return
        state = state.copy(subscriptionPage = page)
        loadSubscriptions()
    }

    fun changeSubscriptionType(it: IndexVM.SubscriptionType) {
        state = state.copy(subscriptionType = it)
        loadSubscriptions()
    }

    fun loadVideoList() {
        viewModelScope.launch {
            state = state.copy(videoState = UiState.Loading)
            runAPICatching {
                mediaRepo.getVideoList(buildVideoQuery().toApiParams())
            }.onSuccess {
                state = state.copy(
                    videoList = it.results,
                    videoCount = it.count,
                    videoState = if (it.results.isNotEmpty()) UiState.Success else UiState.Empty,
                )
            }.onError {
                state = state.copy(subscriptionState = UiState.Error(
                    message = {
                        Text(stringResource(error = it))
                    }
                ))
            }.onException {
                state = state.copy(subscriptionState = UiState.Error(message = {
                    Text(it.exception.localizedMessage ?: "Unknown Error")
                }))
            }
        }
    }

    fun loadImageList() {
        viewModelScope.launch {
            state = state.copy(imageState = UiState.Loading)
            runAPICatching {
                mediaRepo.getImageList(buildImageQuery().toApiParams())
            }.onSuccess {
                state = state.copy(
                    imageList = it.results,
                    imageCount = it.count,
                    imageState = if (it.results.isNotEmpty()) UiState.Success else UiState.Empty,
                )
            }.onError {
                state = state.copy(subscriptionState = UiState.Error(
                    message = {
                        Text(stringResource(error = it))
                    }
                ))
            }.onException {
                state = state.copy(subscriptionState = UiState.Error(message = {
                    Text(it.exception.localizedMessage ?: "Unknown Error")
                }))
            }
        }
    }

    fun updateVideoSort(sort: String) {
        videoSort = sort
        loadVideoList()
    }

    fun updateVideoPage(it: Int) {
        if (it == state.videoPage || it < 1) return
        state = state.copy(videoPage = it)
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
        loadImageList()
    }

    fun updateImagePage(it: Int) {
        if (it == state.imagePage || it < 1) return
        state = state.copy(imagePage = it)
        loadImageList()
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

    fun clearVideoFilter() {
        videoFilters.clear()
    }

    data class IndexState(
        val subscriptionState: UiState = UiState.Initial,
        val subscriptionPage: Int = 1,
        val subscriptionTotal: Int = 0,
        val subscriptionType: SubscriptionType = SubscriptionType.VIDEO,
        val subscriptions: List<Media> = emptyList(),
        val videoState: UiState = UiState.Initial,
        val videoPage: Int = 1,
        val videoCount: Int = 0,
        val videoList: List<Media> = emptyList(),
        val imageState: UiState = UiState.Initial,
        val imagePage: Int = 1,
        val imageCount: Int = 0,
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

