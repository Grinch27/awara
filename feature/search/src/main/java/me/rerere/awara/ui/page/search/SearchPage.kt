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
import coil.compose.AsyncImage
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
import org.koin.androidx.compose.get
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar

private const val SETTING_MEDIA_LIST_MODE = "setting.media_list_mode"
private const val MEDIA_LIST_MODE_DETAIL = "detail"
private const val MEDIA_LIST_MODE_THUMBNAIL = "thumbnail"
private val TAG_BROWSE_FILTERS = ('A'..'Z').map(Char::toString) + ('0'..'9').map(Char::toString)

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
    var recentQueriesRaw by rememberStringPreference(key = "search.recent_queries", default = "")
    var searchBarActive by rememberSaveable { mutableStateOf(false) }
    var initialRouteApplied by remember(initialSearchType, initialTag) { mutableStateOf(false) }
    var lastLoadMoreItemCount by remember { mutableStateOf(-1) }
    val gridState = rememberLazyStaggeredGridState()
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
                !vm.state.loadingMore &&
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
        if (trimmedQuery.isBlank()) {
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
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
                            onQueryChange = { vm.query = it },
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
                                                FilledTonalButton(
                                                    onClick = {
                                                        vm.query = recentQuery
                                                        submitSearch()
                                                    },
                                                ) {
                                                    Text(stringResource(R.string.search_apply_recent_action))
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

@Composable
private fun SearchTypeChip(
    modifier: Modifier = Modifier,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
            },
        ),
        shape = shape,
        tonalElevation = if (selected) 2.dp else 0.dp,
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SearchHeaderIconButton(
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
        ),
        shape = RoundedCornerShape(18.dp),
    ) {
        IconButton(onClick = onClick) {
            Box(contentAlignment = Alignment.Center, content = content)
        }
    }
}

@Composable
private fun SortDropdown(
    modifier: Modifier = Modifier,
    selectedSort: String,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentSort = selectedSort.takeIf { it in MediaSortKeys } ?: DEFAULT_MEDIA_SORT

    Box(modifier = modifier) {
        FilledTonalButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Icon(Icons.Outlined.FilterList, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = mediaSortLabel(currentSort),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            MediaSortKeys.fastForEach { sort ->
                DropdownMenuItem(
                    text = { Text(mediaSortLabel(sort)) },
                    leadingIcon = { MediaSortIcon(sort) },
                    onClick = {
                        expanded = false
                        onValueChange(sort)
                    },
                )
            }
        }
    }
}

@Composable
private fun MediaSortIcon(sort: String) {
    val icon = when (sort) {
        "views" -> Icons.Outlined.RemoveRedEye
        "likes" -> Icons.Outlined.Favorite
        "popularity" -> Icons.Outlined.Star
        else -> Icons.Outlined.CalendarMonth
    }
    Icon(icon, null)
}

@Composable
private fun UserSearchHintBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text = stringResource(R.string.search_user_mode_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DateDropdown(
    modifier: Modifier = Modifier,
    selectedDateValue: String?,
    onValueChange: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        buildList {
            add("")
            for (yearOffset in 0..5) {
                add((year - yearOffset).toString())
            }
            for (offset in 0..23) {
                val totalMonth = year * 12 + (month - 1) - offset
                val optionYear = totalMonth / 12
                val optionMonth = (totalMonth % 12) + 1
                add("$optionYear-$optionMonth")
            }
        }.distinct()
    }
    val currentLabel = selectedDateValue?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.date_any)

    Box(modifier = modifier) {
        FilledTonalButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Icon(Icons.Outlined.CalendarMonth, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currentLabel,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.fastForEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            if (option.isBlank()) {
                                stringResource(R.string.date_any)
                            } else {
                                option
                            },
                        )
                    },
                    onClick = {
                        expanded = false
                        onValueChange(option.ifBlank { null })
                    },
                )
            }
        }
    }
}

@Composable
private fun RatingDropdown(
    modifier: Modifier = Modifier,
    selectedRating: String,
    onValueChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentRating = selectedRating.takeIf { it in MediaRatingKeys } ?: DEFAULT_MEDIA_RATING

    Box(modifier = modifier) {
        FilledTonalButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Icon(Icons.Outlined.Star, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = mediaRatingLabel(currentRating),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            MediaRatingKeys.fastForEach { rating ->
                DropdownMenuItem(
                    text = { Text(mediaRatingLabel(rating)) },
                    onClick = {
                        expanded = false
                        onValueChange(rating)
                    },
                )
            }
        }
    }
}

@Composable
private fun TagBrowseButton(
    modifier: Modifier = Modifier,
    selectedTags: List<FilterValue>,
    onTagSelected: (String) -> Unit,
    onTagRemove: (FilterValue) -> Unit,
) {
    val searchRepository = get<SearchRepository>()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val defaultError = stringResource(R.string.search_error_default)
    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedFilter by rememberSaveable { mutableStateOf(TAG_BROWSE_FILTERS.first()) }
    var page by remember { mutableStateOf(0) }
    var count by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var lastLoadMoreItemCount by remember { mutableStateOf(-1) }
    var tagSearchQuery by rememberSaveable { mutableStateOf("") }
    val tags = remember { mutableStateListOf<String>() }
    val normalizedTagSearchQuery = tagSearchQuery.trim()
    val displayedTags = if (normalizedTagSearchQuery.isBlank()) {
        tags.toList()
    } else {
        tags.filter { tag -> tag.contains(normalizedTagSearchQuery, ignoreCase = true) }
    }
    val currentLabel = when {
        selectedTags.isEmpty() -> stringResource(R.string.tag_browse)
        selectedTags.size == 1 -> "#${selectedTags.first().value}"
        else -> "#${selectedTags.first().value} +${selectedTags.size - 1}"
    }

    fun loadTags(replaceResults: Boolean = true) {
        if (!expanded || (!replaceResults && (loading || loadingMore || !hasMore))) {
            return
        }
        val requestFilter = selectedFilter
        val requestPage = if (replaceResults) 0 else page + 1
        if (replaceResults) {
            page = 0
            count = 0
            hasMore = true
            loading = true
            loadingMore = false
            errorText = null
            lastLoadMoreItemCount = -1
            tags.clear()
        } else {
            loadingMore = true
        }
        scope.launch {
            runCatching {
                searchRepository.browseTags(requestFilter, requestPage)
            }.onSuccess { result ->
                if (selectedFilter != requestFilter) {
                    return@onSuccess
                }
                val mergedTags = if (replaceResults) {
                    result.results
                } else {
                    tags + result.results
                }
                tags.clear()
                tags.addAll(mergedTags)
                page = requestPage
                count = result.count
                hasMore = tags.size < result.count
                errorText = null
            }.onFailure { throwable ->
                if (selectedFilter != requestFilter) {
                    return@onFailure
                }
                if (replaceResults) {
                    errorText = throwable.localizedMessage ?: defaultError
                }
            }
            if (selectedFilter == requestFilter) {
                loading = false
                loadingMore = false
            }
        }
    }

    LaunchedEffect(expanded, selectedFilter) {
        if (expanded) {
            loadTags()
        }
    }

    LaunchedEffect(expanded, tagSearchQuery) {
        if (expanded) {
            tagBrowseFilterFor(tagSearchQuery)?.let { queryFilter ->
                if (queryFilter != selectedFilter) {
                    selectedFilter = queryFilter
                }
            }
        }
    }

    LaunchedEffect(
        expanded,
        listState,
        selectedFilter,
        tags.size,
        hasMore,
    ) {
        if (!expanded) {
            return@LaunchedEffect
        }
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: 0
        }.collectLatest { lastVisibleIndex ->
            if (
                hasMore &&
                !loading &&
                !loadingMore &&
                tags.isNotEmpty() &&
                lastVisibleIndex >= tags.size - 6 &&
                lastLoadMoreItemCount != tags.size
            ) {
                lastLoadMoreItemCount = tags.size
                loadTags(replaceResults = false)
            }
        }
    }

    Box(modifier = modifier) {
        FilledTonalButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Icon(Icons.Outlined.FilterList, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currentLabel,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 240.dp, max = 320.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.tag_browse_title),
                    style = MaterialTheme.typography.titleMedium,
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = tagSearchQuery,
                    onValueChange = { tagSearchQuery = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.tag_quick_filter)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        if (tagSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { tagSearchQuery = "" }) {
                                Icon(Icons.Outlined.Close, null)
                            }
                        }
                    },
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(TAG_BROWSE_FILTERS, key = { it }) { filter ->
                        SearchFilterChip(
                            selected = filter == selectedFilter,
                            label = filter,
                            onClick = {
                                if (selectedFilter != filter) {
                                    tagSearchQuery = ""
                                    selectedFilter = filter
                                }
                            },
                        )
                    }
                }

                if (selectedTags.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(selectedTags, key = { "${it.key}:${it.value}" }) { selectedTag ->
                            SearchFilterChip(
                                selected = true,
                                label = "#${selectedTag.value}",
                                onClick = { onTagRemove(selectedTag) },
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(
                        R.string.search_results_count,
                        if (normalizedTagSearchQuery.isBlank()) count else displayedTags.size,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                    ) {
                        itemsIndexed(displayedTags, key = { index, tag -> "$selectedFilter:$index:$tag" }) { _, tag ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = tag,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                leadingContent = {
                                    Text(
                                        text = "#",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onTagSelected(tag) },
                            )
                        }
                        if (loadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        } else if (hasMore) {
                            item {
                                TextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { loadTags(replaceResults = false) },
                                ) {
                                    Text(stringResource(R.string.search_load_more))
                                }
                            }
                        }
                    }

                    if (loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (displayedTags.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = errorText ?: stringResource(R.string.tag_browse_empty),
                                color = if (errorText == null) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun tagBrowseFilterFor(query: String): String? {
    val firstSearchChar = query.firstOrNull { it.isLetterOrDigit() } ?: return null
    val upperChar = firstSearchChar.uppercaseChar()
    return when {
        upperChar in 'A'..'Z' -> upperChar.toString()
        firstSearchChar in '0'..'9' -> firstSearchChar.toString()
        else -> null
    }
}

@Composable
private fun searchTypeLabel(type: String): String {
    return when (type) {
        SearchTypes.IMAGE -> stringResource(R.string.image)
        SearchTypes.POST -> stringResource(R.string.post)
        SearchTypes.USER -> stringResource(R.string.user)
        SearchTypes.PLAYLIST -> stringResource(R.string.playlist)
        SearchTypes.FORUM_POST -> stringResource(R.string.search_type_forum_posts)
        SearchTypes.FORUM_THREAD -> stringResource(R.string.search_type_forum_threads)
        else -> stringResource(R.string.video)
    }
}

@Composable
private fun mediaSortLabel(sort: String): String {
    return when (sort) {
        "trending" -> stringResource(R.string.sort_trending)
        "popularity" -> stringResource(R.string.sort_popularity)
        "views" -> stringResource(R.string.sort_views)
        "likes" -> stringResource(R.string.sort_likes)
        else -> stringResource(R.string.sort_date)
    }
}

@Composable
private fun mediaRatingLabel(rating: String): String {
    return when (rating) {
        "ecchi" -> stringResource(R.string.rating_ecchi)
        "general" -> stringResource(R.string.rating_general)
        else -> stringResource(R.string.rating_all)
    }
}

@Composable
private fun UiStateBox(
    modifier: Modifier = Modifier,
    state: UiState,
    onErrorRetry: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        when (state) {
            UiState.Initial -> Unit
            UiState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(R.string.search_recent_empty))
                }
            }

            UiState.Loading -> {
                content()
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            UiState.Success -> content()
            is UiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    state.message?.invoke()
                    if (state.message == null) {
                        Text(
                            text = state.messageText ?: stringResource(R.string.search_error_default),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    TextButton(onClick = onErrorRetry) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

private fun mediaListGridCells(listMode: String): StaggeredGridCells = when (listMode) {
    MEDIA_LIST_MODE_THUMBNAIL -> DynamicStaggeredGridCells(150.dp, 2, 4)
    else -> StaggeredGridCells.Fixed(1)
}

private class DynamicStaggeredGridCells(
    private val minSize: Dp = 150.dp,
    private val min: Int = 2,
    private val max: Int = 4,
) : StaggeredGridCells {
    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        val count = maxOf((availableSize + spacing) / (minSize.roundToPx() + spacing), 1)
        val clampedCount = count.coerceIn(min, max)
        return calculateCellsCrossAxisSizeImpl(availableSize, clampedCount, spacing)
    }
}

private fun calculateCellsCrossAxisSizeImpl(
    gridSize: Int,
    slotCount: Int,
    spacing: Int,
): List<Int> {
    val gridSizeWithoutSpacing = gridSize - spacing * (slotCount - 1)
    val slotSize = gridSizeWithoutSpacing / slotCount
    val remainingPixels = gridSizeWithoutSpacing % slotCount
    return List(slotCount) { index ->
        slotSize + if (index < remainingPixels) 1 else 0
    }
}

@Composable
private fun SearchResultCard(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (onClick == null) {
        Card { content() }
    } else {
        Card(onClick = onClick) { content() }
    }
}

@Composable
private fun SearchPostCard(post: SearchPostItem) {
    SearchResultCard {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            SearchMetaText(authorName = post.authorName, dateLabel = post.createdAtLabel)
            post.body.takeIf { it.isNotBlank() }?.let { body ->
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(R.string.num_views, post.numViews),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SearchPlaylistCard(
    playlist: SearchPlaylistItem,
    onClick: () -> Unit,
) {
    SearchResultCard(onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth()) {
            playlist.thumbnailUrl?.let { thumbnailUrl ->
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(132.dp)
                        .aspectRatio(16f / 9f),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                SearchMetaText(authorName = playlist.authorName, dateLabel = "")
                Text(
                    text = stringResource(R.string.playlist_num_videos, playlist.numVideos),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SearchForumPostCard(
    post: SearchForumPostItem,
    onOpenForumThread: (String) -> Unit,
) {
    SearchResultCard(
        onClick = post.threadId.takeIf(String::isNotBlank)?.let { threadId ->
            { onOpenForumThread(threadId) }
        },
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = post.threadTitle.ifBlank { post.threadId.ifBlank { post.id } },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            SearchMetaText(authorName = post.authorName, dateLabel = post.createdAtLabel)
            post.body.takeIf { it.isNotBlank() }?.let { body ->
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(R.string.search_forum_reply, post.replyNum),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SearchForumThreadCard(
    thread: SearchForumThreadItem,
    onClick: () -> Unit,
) {
    SearchResultCard(onClick = onClick) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (thread.sticky) {
                    RoleBadge(text = stringResource(R.string.forum_sticky))
                }
                if (thread.locked) {
                    RoleBadge(text = stringResource(R.string.forum_locked))
                }
                if (thread.section.isNotBlank()) {
                    RoleBadge(text = thread.section)
                }
            }
            Text(
                text = thread.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            SearchMetaText(authorName = thread.authorName, dateLabel = thread.updatedAtLabel)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.forum_posts_count, thread.numPosts),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.forum_views_count, thread.numViews),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SearchMetaText(
    authorName: String,
    dateLabel: String,
) {
    val text = listOf(authorName, dateLabel)
        .filter(String::isNotBlank)
        .joinToString(separator = " / ")
    if (text.isNotBlank()) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SearchMediaCard(
    media: SearchMediaItem,
    listMode: String,
    onClick: () -> Unit,
) {
    Card(onClick = onClick) {
        if (listMode == MEDIA_LIST_MODE_THUMBNAIL) {
            Column {
                SearchMediaCover(
                    media = media,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(220f / 160f),
                )
                Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Text(
                        text = media.title.trim(),
                        maxLines = 2,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = media.authorName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Box(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Outlined.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = media.numLikes.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                SearchMediaCover(
                    media = media,
                    modifier = Modifier
                        .width(164.dp)
                        .aspectRatio(220f / 160f),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = media.title.trim(),
                        maxLines = 3,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = media.authorName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Text(
                        text = media.createdAtLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.num_views, media.numViews),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.num_likes, media.numLikes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    media.description?.takeIf { it.isNotEmpty() }?.let { description ->
                        Text(
                            text = description,
                            maxLines = 3,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchMediaCover(
    media: SearchMediaItem,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        AsyncImage(
            model = media.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (media.isPrivate) {
            Badge(
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopEnd),
            ) {
                Text(text = stringResource(R.string.private_badge))
            }
        }
    }
}

@Composable
private fun SearchUserCard(
    user: SearchUserItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = {
            if (user.hasNavigableProfile) {
                onClick()
            }
        },
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    text = user.displayHandle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                RoleBadge(text = user.role)
                if (user.premium) {
                    RoleBadge(text = stringResource(R.string.premium))
                }
            }
        }
    }
}

@Composable
private fun RoleBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}