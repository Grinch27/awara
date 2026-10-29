package me.rerere.awara.ui.page.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.rerere.awara.feature.search.R
import me.rerere.awara.ui.component.common.UiState
import me.rerere.awara.ui.component.iwara.param.FilterValue
import me.rerere.awara.ui.component.iwara.param.rating.DEFAULT_MEDIA_RATING
import me.rerere.awara.ui.component.iwara.param.rating.MediaRatingKeys
import me.rerere.awara.ui.component.iwara.param.rating.SETTING_MEDIA_SEARCH_RATING
import me.rerere.awara.ui.component.iwara.param.sort.DEFAULT_MEDIA_SORT
import me.rerere.awara.ui.component.iwara.param.sort.MediaSortKeys
import me.rerere.compose_setting.preference.rememberStringPreference
import me.rerere.compose_setting.preference.rememberBooleanPreference
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.CancellationException
import me.rerere.awara.ui.component.iwara.*
import me.rerere.awara.domain.media.MediaCardModel

@Composable
fun SearchPage(
    vm: SearchVM = koinViewModel(),
    initialSearchType: String? = null,
    initialTag: String? = null,
    onBack: () -> Unit = {},
    onOpenMedia: (SearchMediaItem) -> Unit = {},
    onOpenUser: (SearchUserItem) -> Unit = {},
    onOpenPlaylist: (SearchPlaylistItem) -> Unit = {},
    onOpenForumThread: (String) -> Unit = {},
) {
    var listMode by rememberStringPreference(
        key = SETTING_MEDIA_LIST_MODE,
        default = MEDIA_LIST_MODE_DETAIL,
    )
    var globalSearchRating by rememberStringPreference(
        key = SETTING_MEDIA_SEARCH_RATING,
        default = DEFAULT_MEDIA_RATING,
    )
    var saveHistory by rememberBooleanPreference("search.save_history", true)
    var recentQueriesRaw by rememberStringPreference(key = "search.recent_queries", default = "")
    var searchBarActive by rememberSaveable { mutableStateOf(false) }
    var initialRouteApplied by remember(initialSearchType, initialTag) { mutableStateOf(false) }
    var lastLoadMoreItemCount by remember { mutableStateOf(-1) }
    val gridState = key(vm.state.resultKey) { rememberLazyStaggeredGridState() }
    val defaultSearchRating = globalSearchRating.takeIf { it in MediaRatingKeys } ?: DEFAULT_MEDIA_RATING
    val recentQueries = remember(recentQueriesRaw) {
        recentQueriesRaw.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .take(8)
            .toList()
    }
    val isMediaSearch = vm.state.searchType in SearchTypes.media
    val activeFilters = when (vm.state.searchType) {
        SearchTypes.IMAGE -> vm.imageFilters.toList()
        SearchTypes.VIDEO -> vm.videoFilters.toList()
        else -> emptyList()
    }
    val currentDateFilterValue = activeFilters.firstOrNull { it.key == "date" }?.value
    val currentTagFilterValues = activeFilters.filter { it.key == "tags" }
    val currentSortValue = when (vm.state.searchType) {
        SearchTypes.IMAGE -> vm.imageSort
        else -> vm.videoSort
    }
    val currentRatingValue = when (vm.state.searchType) {
        SearchTypes.IMAGE -> vm.imageRating
        else -> vm.videoRating
    }
    val currentItemCount = when (vm.state.searchType) {
        SearchTypes.IMAGE -> vm.state.imageList.size
        SearchTypes.USER -> vm.state.userList.size
        SearchTypes.POST -> vm.state.postList.size
        SearchTypes.PLAYLIST -> vm.state.playlistList.size
        SearchTypes.FORUM_POST -> vm.state.forumPostList.size
        SearchTypes.FORUM_THREAD -> vm.state.forumThreadList.size
        else -> vm.state.videoList.size
    }

    LaunchedEffect(defaultSearchRating, initialSearchType, initialTag) {
        vm.applyDefaultRating(defaultSearchRating)
        if (!initialRouteApplied && !initialTag.isNullOrBlank()) {
            lastLoadMoreItemCount = -1
            vm.searchByTag(initialSearchType.orEmpty(), initialTag)
            searchBarActive = false
            initialRouteApplied = true
        }
    }

    LaunchedEffect(
        gridState,
        vm.state.searchType,
        currentItemCount,
        vm.state.hasMore,
    ) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: 0
        }.collectLatest { lastVisibleIndex ->
            if (
                vm.state.hasMore &&
                !vm.state.loadingMore && !vm.state.appendFailed &&
                currentItemCount > 0 &&
                lastVisibleIndex >= currentItemCount - 6 &&
                lastLoadMoreItemCount != currentItemCount
            ) {
                lastLoadMoreItemCount = currentItemCount
                vm.loadNextPage()
            }
        }
    }

    fun persistRecentQuery() {
        val trimmedQuery = vm.query.trim()
        if (!saveHistory || trimmedQuery.isBlank()) {
            return
        }
        recentQueriesRaw = buildList {
            add(trimmedQuery)
            addAll(recentQueries.filterNot { it == trimmedQuery })
        }.take(8).joinToString(separator = "\n")
    }

    fun submitSearch() {
        persistRecentQuery()
        lastLoadMoreItemCount = -1
        vm.submitSearch()
        searchBarActive = false
    }

    fun updateSearchType(type: String) {
        lastLoadMoreItemCount = -1
        vm.updateSearchType(type)
    }

    fun updateMediaSort(sort: String) {
        lastLoadMoreItemCount = -1
        if (vm.state.searchType == SearchTypes.IMAGE) {
            vm.updateImageSort(sort)
        } else {
            vm.updateVideoSort(sort)
        }
    }

    fun updateRating(rating: String) {
        lastLoadMoreItemCount = -1
        if (vm.state.searchType == SearchTypes.IMAGE) {
            vm.updateImageRating(rating)
        } else {
            vm.updateVideoRating(rating)
        }
    }

    fun updateDateFilter(selectedDate: String?) {
        val activeValueList = if (vm.state.searchType == SearchTypes.IMAGE) {
            vm.imageFilters.toList()
        } else {
            vm.videoFilters.toList()
        }
        activeValueList.filter { it.key == "date" }.forEach {
            if (vm.state.searchType == SearchTypes.IMAGE) {
                vm.removeImageFilter(it)
            } else {
                vm.removeVideoFilter(it)
            }
        }
        if (!selectedDate.isNullOrBlank()) {
            val dateFilter = FilterValue("date", selectedDate)
            if (vm.state.searchType == SearchTypes.IMAGE) {
                vm.addImageFilter(dateFilter)
            } else {
                vm.addVideoFilter(dateFilter)
            }
        }
        lastLoadMoreItemCount = -1
        vm.submitSearch()
    }

    fun addTagFilter(tag: String) {
        val tagFilter = FilterValue("tags", tag)
        if (vm.state.searchType == SearchTypes.IMAGE) {
            vm.addImageFilter(tagFilter)
        } else {
            vm.addVideoFilter(tagFilter)
        }
        lastLoadMoreItemCount = -1
        vm.submitSearch()
    }

    fun removeMediaFilter(filterValue: FilterValue) {
        if (vm.state.searchType == SearchTypes.IMAGE) {
            vm.removeImageFilter(filterValue)
        } else {
            vm.removeVideoFilter(filterValue)
        }
        lastLoadMoreItemCount = -1
        vm.submitSearch()
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                ),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SearchHeaderIconButton(onClick = onBack) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                        }
                        DockedSearchBar(
                            modifier = Modifier.weight(1f),
                            query = vm.query,
                            onQueryChange = { vm.query = it.take(2048) },
                            onSearch = { submitSearch() },
                            active = searchBarActive,
                            onActiveChange = { searchBarActive = it },
                            shape = RoundedCornerShape(18.dp),
                            tonalElevation = 0.dp,
                            placeholder = {
                                Text(stringResource(R.string.search_hint_media))
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, null)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (searchBarActive && vm.query.isNotBlank()) {
                                            vm.query = ""
                                        } else {
                                            submitSearch()
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = if (searchBarActive && vm.query.isNotBlank()) {
                                            Icons.Outlined.Close
                                        } else {
                                            Icons.Outlined.Search
                                        },
                                        contentDescription = null,
                                    )
                                }
                            },
                            content = {
                                TextButton(onClick = { saveHistory = !saveHistory }) {
                                    Text(stringResource(if (saveHistory) R.string.search_pause_history else R.string.search_enable_history))
                                }
                                if (recentQueries.isEmpty()) {
                                    ListItem(
                                        headlineContent = {
                                            Text(stringResource(R.string.search_recent_empty))
                                        },
                                        leadingContent = {
                                            Icon(Icons.Outlined.History, null)
                                        },
                                    )
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(text = stringResource(R.string.search_recent_title))
                                        TextButton(onClick = { recentQueriesRaw = "" }) {
                                            Text(stringResource(R.string.search_recent_clear))
                                        }
                                    }
                                    recentQueries.forEach { recentQuery ->
                                        ListItem(
                                            headlineContent = {
                                                Text(recentQuery)
                                            },
                                            leadingContent = {
                                                Icon(Icons.Outlined.History, null)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            trailingContent = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = { recentQueriesRaw = recentQueries.filterNot { it == recentQuery }.joinToString("\n") }) {
                                                    Icon(Icons.Outlined.Close, stringResource(R.string.search_remove_history))
                                                }
                                                FilledTonalButton(
                                                    onClick = {
                                                        vm.query = recentQuery
                                                        submitSearch()
                                                    },
                                                ) {
                                                    Text(stringResource(R.string.search_apply_recent_action))
                                                }
                                                }
                                            },
                                        )
                                    }
                                }
                            },
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SearchTypes.all.fastForEach { type ->
                            SearchTypeChip(
                                modifier = Modifier.widthIn(min = 78.dp),
                                selected = vm.state.searchType == type,
                                label = searchTypeLabel(type),
                                onClick = { updateSearchType(type) },
                            )
                        }
                    }

                    if (isMediaSearch) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SortDropdown(
                                modifier = Modifier.widthIn(min = 118.dp, max = 168.dp),
                                selectedSort = currentSortValue,
                                onValueChange = ::updateMediaSort,
                            )
                            RatingDropdown(
                                modifier = Modifier.widthIn(min = 118.dp, max = 168.dp),
                                selectedRating = currentRatingValue,
                                onValueChange = ::updateRating,
                            )
                            DateDropdown(
                                modifier = Modifier.widthIn(min = 132.dp, max = 180.dp),
                                selectedDateValue = currentDateFilterValue,
                                onValueChange = ::updateDateFilter,
                            )
                            TagBrowseButton(
                                modifier = Modifier.widthIn(min = 148.dp, max = 220.dp),
                                selectedTags = currentTagFilterValues,
                                onTagSelected = ::addTagFilter,
                                onTagRemove = ::removeMediaFilter,
                            )
                        }
                    } else if (vm.state.searchType == SearchTypes.USER) {
                        UserSearchHintBanner(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            Text(
                text = stringResource(R.string.search_results_count, vm.state.count),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                UiStateBox(
                    modifier = Modifier.fillMaxSize(),
                    state = vm.state.uiState,
                    onErrorRetry = vm::search,
                ) {
                    LazyVerticalStaggeredGrid(
                        state = gridState,
                        columns = when {
                            vm.state.searchType == SearchTypes.USER -> DynamicStaggeredGridCells(180.dp, 1, 2)
                            isMediaSearch -> mediaListGridCells(listMode)
                            else -> StaggeredGridCells.Fixed(1)
                        },
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        when (vm.state.searchType) {
                            SearchTypes.VIDEO -> {
                                items(vm.state.videoList, key = { it.id }) { media ->
                                    SearchMediaCard(
                                        media = media,
                                        listMode = listMode,
                                        onClick = { onOpenMedia(media) },
                                    )
                                }
                            }

                            SearchTypes.IMAGE -> {
                                items(vm.state.imageList, key = { it.id }) { media ->
                                    SearchMediaCard(
                                        media = media,
                                        listMode = listMode,
                                        onClick = { onOpenMedia(media) },
                                    )
                                }
                            }

                            SearchTypes.USER -> {
                                items(vm.state.userList, key = { it.id }) { user ->
                                    SearchUserCard(
                                        user = user,
                                        onClick = { onOpenUser(user) },
                                    )
                                }
                            }

                            SearchTypes.POST -> {
                                items(vm.state.postList, key = { it.id }) { post ->
                                    SearchPostCard(post = post)
                                }
                            }

                            SearchTypes.PLAYLIST -> {
                                items(vm.state.playlistList, key = { it.id }) { playlist ->
                                    SearchPlaylistCard(
                                        playlist = playlist,
                                        onClick = { onOpenPlaylist(playlist) },
                                    )
                                }
                            }

                            SearchTypes.FORUM_POST -> {
                                items(vm.state.forumPostList, key = { it.id }) { post ->
                                    SearchForumPostCard(
                                        post = post,
                                        onOpenForumThread = onOpenForumThread,
                                    )
                                }
                            }

                            SearchTypes.FORUM_THREAD -> {
                                items(vm.state.forumThreadList, key = { it.id }) { thread ->
                                    SearchForumThreadCard(
                                        thread = thread,
                                        onClick = { onOpenForumThread(thread.id) },
                                    )
                                }
                            }
                        }

                        if (vm.state.appendFailed) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                TextButton(onClick = { lastLoadMoreItemCount = -1; vm.loadNextPage() }) {
                                    Text(stringResource(R.string.search_error_default))
                                }
                            }
                        }
                        if (vm.state.loadingMore) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

