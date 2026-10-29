package me.rerere.awara.ui.page.search

// Requirements: seven search types; request snapshots, cancellation and deduplication.
// Open decisions: presets remain optional. Verify rapid switching and process recreation.
// TODO(agent): If user profile search gains typed filters later, move the remaining raw string branch onto the same FeedQuery pipeline instead of keeping two query styles.

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
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
    private val savedState: SavedStateHandle,
) : ViewModel() {
    var state by mutableStateOf(SearchState(searchType = SearchTypes.normalize(savedState["type"] ?: "video")))
        private set
    var query by mutableStateOf(savedState.get<String>("query").orEmpty())
    var videoSort by mutableStateOf(savedState.get<String>("videoSort") ?: DEFAULT_MEDIA_SORT)
    var videoRating by mutableStateOf(savedState.get<String>("videoRating") ?: DEFAULT_MEDIA_RATING)
    val videoFilters: MutableList<FilterValue> = mutableStateListOf()
    var imageSort by mutableStateOf(savedState.get<String>("imageSort") ?: DEFAULT_MEDIA_SORT)
    var imageRating by mutableStateOf(savedState.get<String>("imageRating") ?: DEFAULT_MEDIA_RATING)
    val imageFilters: MutableList<FilterValue> = mutableStateListOf()
    private var defaultRatingApplied = savedState.contains("videoRating")
    private var requestJob: Job? = null
    private var generation = 0L

    init {
        restoreFilters("videoFilters", videoFilters)
        restoreFilters("imageFilters", imageFilters)
        if (savedState.get<Boolean>("submitted") == true) search()
    }

    private fun restoreFilters(key: String, target: MutableList<FilterValue>) {
        savedState.get<ArrayList<String>>(key)?.chunked(2)?.filter { it.size == 2 }
            ?.take(64)?.forEach { target.add(FilterValue(it[0], it[1])) }
    }

    private fun persistCriteria() {
        query = query.trim().take(2048)
        savedState["query"] = query
        savedState["type"] = state.searchType
        savedState["videoSort"] = videoSort
        savedState["videoRating"] = videoRating
        savedState["imageSort"] = imageSort
        savedState["imageRating"] = imageRating
        savedState["videoFilters"] = ArrayList(videoFilters.flatMap { listOf(it.key, it.value) })
        savedState["imageFilters"] = ArrayList(imageFilters.flatMap { listOf(it.key, it.value) })
    }

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

    // Capture each request before launching; stale or cancelled results never publish.
    fun search(replaceResults: Boolean = true) {
        if (!replaceResults && state.loadingMore) return
        requestJob?.cancel()
        val requestId = ++generation
        persistCriteria()
        savedState["submitted"] = true
        val requestedPage = if (replaceResults) 1 else state.page + 1
        val before = state.copy(page = requestedPage)
        val requestType = before.searchType
        val requestQuery = query
        val mediaQuery = buildMediaSearchQuery(if (requestType == SearchTypes.IMAGE) FeedScope.SEARCH_IMAGE else FeedScope.SEARCH_VIDEO)
            .copy(page = requestedPage - 1)
        state = before.copy(uiState = if (replaceResults) UiState.Loading else before.uiState,
            loadingMore = !replaceResults, appendFailed = false,
            resultKey = if (replaceResults) requestType + requestQuery + mediaQuery.copy(page = 0).toString() else before.resultKey)
        val resultKey = state.resultKey
        requestJob = viewModelScope.launch {
            try {
                val result = when (requestType) {
                    SearchTypes.VIDEO -> {
                        val pager = searchRepository.searchVideos(mediaQuery)
                        val previous = if (replaceResults) emptyList() else before.videoList
                        val merged = (previous + pager.results).distinctBy { it.id }
                        before.copy(videoList = merged, count = pager.count.coerceAtLeast(merged.size),
                            uiState = if (merged.isEmpty()) UiState.Empty else UiState.Success,
                            loadingMore = false, appendFailed = false,
                            hasMore = pager.results.isNotEmpty() && merged.size > previous.size && merged.size < pager.count)
                    }
                    SearchTypes.IMAGE -> {
                        val pager = searchRepository.searchImages(mediaQuery)
                        val previous = if (replaceResults) emptyList() else before.imageList
                        val merged = (previous + pager.results).distinctBy { it.id }
                        before.copy(imageList = merged, count = pager.count.coerceAtLeast(merged.size),
                            uiState = if (merged.isEmpty()) UiState.Empty else UiState.Success,
                            loadingMore = false, appendFailed = false,
                            hasMore = pager.results.isNotEmpty() && merged.size > previous.size && merged.size < pager.count)
                    }
                    SearchTypes.USER -> {
                        val pager = searchRepository.searchUsers(requestQuery, requestedPage - 1)
                        val previous = if (replaceResults) emptyList() else before.userList
                        val merged = (previous + pager.results).distinctBy { it.id }
                        before.copy(userList = merged, count = pager.count.coerceAtLeast(merged.size),
                            uiState = if (merged.isEmpty()) UiState.Empty else UiState.Success,
                            loadingMore = false, appendFailed = false,
                            hasMore = pager.results.isNotEmpty() && merged.size > previous.size && merged.size < pager.count)
                    }
                    SearchTypes.POST -> {
                        val pager = searchRepository.searchPosts(requestQuery, requestedPage - 1)
                        val previous = if (replaceResults) emptyList() else before.postList
                        val merged = (previous + pager.results).distinctBy { it.id }
                        before.copy(postList = merged, count = pager.count.coerceAtLeast(merged.size),
                            uiState = if (merged.isEmpty()) UiState.Empty else UiState.Success,
                            loadingMore = false, appendFailed = false,
                            hasMore = pager.results.isNotEmpty() && merged.size > previous.size && merged.size < pager.count)
                    }
                    SearchTypes.PLAYLIST -> {
                        val pager = searchRepository.searchPlaylists(requestQuery, requestedPage - 1)
                        val previous = if (replaceResults) emptyList() else before.playlistList
                        val merged = (previous + pager.results).distinctBy { it.id }
                        before.copy(playlistList = merged, count = pager.count.coerceAtLeast(merged.size),
                            uiState = if (merged.isEmpty()) UiState.Empty else UiState.Success,
                            loadingMore = false, appendFailed = false,
                            hasMore = pager.results.isNotEmpty() && merged.size > previous.size && merged.size < pager.count)
                    }
                    SearchTypes.FORUM_POST -> {
                        val pager = searchRepository.searchForumPosts(requestQuery, requestedPage - 1)
                        val previous = if (replaceResults) emptyList() else before.forumPostList
                        val merged = (previous + pager.results).distinctBy { it.id }
                        before.copy(forumPostList = merged, count = pager.count.coerceAtLeast(merged.size),
                            uiState = if (merged.isEmpty()) UiState.Empty else UiState.Success,
                            loadingMore = false, appendFailed = false,
                            hasMore = pager.results.isNotEmpty() && merged.size > previous.size && merged.size < pager.count)
                    }
                    SearchTypes.FORUM_THREAD -> {
                        val pager = searchRepository.searchForumThreads(requestQuery, requestedPage - 1)
                        val previous = if (replaceResults) emptyList() else before.forumThreadList
                        val merged = (previous + pager.results).distinctBy { it.id }
                        before.copy(forumThreadList = merged, count = pager.count.coerceAtLeast(merged.size),
                            uiState = if (merged.isEmpty()) UiState.Empty else UiState.Success,
                            loadingMore = false, appendFailed = false,
                            hasMore = pager.results.isNotEmpty() && merged.size > previous.size && merged.size < pager.count)
                    }
                    else -> before
                }
                coroutineContext.ensureActive()
                if (generation == requestId) state = result.copy(resultKey = resultKey)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == requestId) state = state.copy(
                    page = if (replaceResults) 1 else (requestedPage - 1).coerceAtLeast(1),
                    uiState = if (replaceResults) UiState.Error(throwable = error, messageText = null) else before.uiState,
                    loadingMore = false, appendFailed = !replaceResults)
            }
        }
    }

    fun loadNextPage() {
        if (state.uiState != UiState.Success || state.loadingMore || !state.hasMore) {
            return
        }
        search(replaceResults = false)
    }

    fun updateSearchType(type: String) {
        requestJob?.cancel()
        ++generation
        val normalizedType = SearchTypes.normalize(type)
        savedState["type"] = normalizedType
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
        val resultKey: String = "initial",
        val appendFailed: Boolean = false,
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