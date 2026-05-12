package me.rerere.awara.ui.page.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalFireDepartment
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import me.rerere.awara.ui.component.iwara.param.sort.DEFAULT_MEDIA_SORT
import me.rerere.awara.ui.component.iwara.param.sort.MediaSortKeys
import me.rerere.compose_setting.preference.rememberStringPreference
import org.koin.androidx.compose.get
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar

private const val SETTING_MEDIA_LIST_MODE = "setting.media_list_mode"
private const val MEDIA_LIST_MODE_DETAIL = "detail"
private const val MEDIA_LIST_MODE_THUMBNAIL = "thumbnail"

@Composable
fun SearchPage(
    vm: SearchVM = koinViewModel(),
    onBack: () -> Unit = {},
    onOpenMedia: (SearchMediaItem) -> Unit = {},
    onOpenUser: (SearchUserItem) -> Unit = {},
) {
    var listMode by rememberStringPreference(
        key = SETTING_MEDIA_LIST_MODE,
        default = MEDIA_LIST_MODE_DETAIL,
    )
    var recentQueriesRaw by rememberStringPreference(key = "search.recent_queries", default = "")
    var searchBarActive by rememberSaveable { mutableStateOf(false) }
    val gridState = rememberLazyStaggeredGridState()
    val recentQueries = remember(recentQueriesRaw) {
        recentQueriesRaw.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .take(8)
            .toList()
    }
    val activeFilters = when (vm.state.searchType) {
        "image" -> vm.imageFilters.toList()
        "video" -> vm.videoFilters.toList()
        else -> emptyList()
    }
    val currentDateFilterValue = activeFilters.firstOrNull { it.key == "date" }?.value
    val currentItemCount = when (vm.state.searchType) {
        "image" -> vm.state.imageList.size
        "user" -> vm.state.userList.size
        else -> vm.state.videoList.size
    }

    LaunchedEffect(
        gridState,
        vm.state.searchType,
        currentItemCount,
        vm.state.hasMore,
        vm.state.loadingMore,
    ) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }.collectLatest { lastVisibleIndex ->
            if (vm.state.hasMore && !vm.state.loadingMore && currentItemCount > 0 && lastVisibleIndex >= currentItemCount - 6) {
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
        vm.submitSearch()
        searchBarActive = false
    }

    fun updateDateFilter(selectedDate: String?) {
        val activeValueList = if (vm.state.searchType == "image") {
            vm.imageFilters.toList()
        } else {
            vm.videoFilters.toList()
        }
        activeValueList.filter { it.key == "date" }.forEach {
            if (vm.state.searchType == "image") {
                vm.removeImageFilter(it)
            } else {
                vm.removeVideoFilter(it)
            }
        }
        if (!selectedDate.isNullOrBlank()) {
            val dateFilter = FilterValue("date", selectedDate)
            if (vm.state.searchType == "image") {
                vm.addImageFilter(dateFilter)
            } else {
                vm.addVideoFilter(dateFilter)
            }
        }
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SearchTypeChip(
                            modifier = Modifier.weight(1f),
                            selected = vm.state.searchType == "video",
                            label = stringResource(R.string.video),
                            onClick = { vm.updateSearchType("video") },
                        )
                        SearchTypeChip(
                            modifier = Modifier.weight(1f),
                            selected = vm.state.searchType == "image",
                            label = stringResource(R.string.image),
                            onClick = { vm.updateSearchType("image") },
                        )
                        SearchTypeChip(
                            modifier = Modifier.weight(1f),
                            selected = vm.state.searchType == "user",
                            label = stringResource(R.string.user),
                            onClick = { vm.updateSearchType("user") },
                        )
                    }

                    if (vm.state.searchType != "user") {
                        DateDropdown(
                            modifier = Modifier.fillMaxWidth(),
                            selectedDateValue = currentDateFilterValue,
                            onValueChange = ::updateDateFilter,
                        )
                    } else {
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
                        columns = if (vm.state.searchType == "user") {
                            DynamicStaggeredGridCells(180.dp, 1, 2)
                        } else {
                            mediaListGridCells(listMode)
                        },
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        when (vm.state.searchType) {
                            "video" -> {
                                items(vm.state.videoList, key = { it.id }) { media ->
                                    SearchMediaCard(
                                        media = media,
                                        listMode = listMode,
                                        onClick = { onOpenMedia(media) },
                                    )
                                }
                            }

                            "image" -> {
                                items(vm.state.imageList, key = { it.id }) { media ->
                                    SearchMediaCard(
                                        media = media,
                                        listMode = listMode,
                                        onClick = { onOpenMedia(media) },
                                    )
                                }
                            }

                            "user" -> {
                                items(vm.state.userList, key = { it.id }) { user ->
                                    SearchUserCard(
                                        user = user,
                                        onClick = { onOpenUser(user) },
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
private fun searchOptions(): List<SelectOption<String>> {
    return listOf(
        SelectOption(
            value = "video",
            label = { Text(stringResource(R.string.video)) },
        ),
        SelectOption(
            value = "image",
            label = { Text(stringResource(R.string.image)) },
        ),
        SelectOption(
            value = "user",
            label = { Text(stringResource(R.string.user)) },
        ),
    )
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
            for (offset in 0..23) {
                val totalMonth = year * 12 + (month - 1) - offset
                val optionYear = totalMonth / 12
                val optionMonth = (totalMonth % 12) + 1
                add("$optionYear-$optionMonth")
            }
        }
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
private fun SearchSummarySection(
    query: String,
    searchType: String,
    activeSort: String,
    activeFilters: List<FilterValue>,
    onEditQuery: () -> Unit,
    onRemoveFilter: (FilterValue) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.search_summary_filters_title),
                    style = MaterialTheme.typography.labelMedium,
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (query.isNotBlank()) {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = onEditQuery,
                                label = {
                                    Text(
                                        text = stringResource(R.string.search_summary_query, query),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                        }
                    }

                    if (searchType != "user") {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = {
                                    Text(
                                        text = stringResource(
                                            R.string.search_summary_sort,
                                            mediaSortLabel(activeSort),
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(items = activeFilters, key = { "${it.key}:${it.value}" }) { filterValue ->
                FilterChip(
                    selected = true,
                    onClick = { onRemoveFilter(filterValue) },
                    label = {
                        Text(
                            text = formatSearchFilterValue(filterValue),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Close, contentDescription = null)
                    },
                )
            }
        }
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

private fun formatSearchFilterValue(filterValue: FilterValue): String {
    return if (filterValue.key.contains("tag", ignoreCase = true)) {
        "#${filterValue.value}"
    } else {
        "${filterValue.key}:${filterValue.value}"
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

@Stable
private class SelectOption<T>(
    val value: T,
    val label: @Composable () -> Unit,
)

@Composable
private fun <T> SelectButton(
    value: T,
    options: List<SelectOption<T>>,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDropdown by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        TextButton(onClick = { showDropdown = !showDropdown }) {
            options.firstOrNull { it.value == value }?.label?.invoke() ?: Text("-")
        }

        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
        ) {
            options.fastForEach { option ->
                DropdownMenuItem(
                    text = { option.label() },
                    onClick = {
                        onValueChange(option.value)
                        showDropdown = false
                    },
                )
            }
        }
    }
}

private data class SortOption(
    val name: String,
    val label: @Composable () -> Unit,
    val icon: @Composable () -> Unit,
)

@Composable
private fun mediaSortOptions(): List<SortOption> {
    return MediaSortKeys.map { key ->
        when (key) {
            "trending" -> SortOption(
                name = key,
                label = { Text(stringResource(R.string.sort_trending)) },
                icon = { Icon(Icons.Outlined.LocalFireDepartment, null) },
            )

            "popularity" -> SortOption(
                name = key,
                label = { Text(stringResource(R.string.sort_popularity)) },
                icon = { Icon(Icons.Outlined.Star, null) },
            )

            "views" -> SortOption(
                name = key,
                label = { Text(stringResource(R.string.sort_views)) },
                icon = { Icon(Icons.Outlined.RemoveRedEye, null) },
            )

            "likes" -> SortOption(
                name = key,
                label = { Text(stringResource(R.string.sort_likes)) },
                icon = { Icon(Icons.Outlined.Favorite, null) },
            )

            else -> SortOption(
                name = key,
                label = { Text(stringResource(R.string.sort_date)) },
                icon = { Icon(Icons.Outlined.CalendarMonth, null) },
            )
        }
    }
}

@Composable
private fun FilterAndSort(
    sort: String,
    onSortChange: (String) -> Unit,
    filterValues: List<FilterValue>,
    onFilterAdd: (FilterValue) -> Unit,
    onFilterRemove: (FilterValue) -> Unit,
    onFilterChooseDone: () -> Unit,
    onFilterClear: () -> Unit,
) {
    val sortOptions = mediaSortOptions()
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val currentSort = sortOptions.firstOrNull { it.name == sort } ?: sortOptions.first()

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box {
            FilledTonalButton(onClick = { showSortMenu = !showSortMenu }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    currentSort.icon()
                    currentSort.label()
                }
            }

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false },
            ) {
                sortOptions.fastForEach { option ->
                    DropdownMenuItem(
                        text = { option.label() },
                        leadingIcon = { option.icon() },
                        onClick = {
                            onSortChange(option.name)
                            showSortMenu = false
                        },
                    )
                }
            }
        }

        Box {
            FilledTonalButton(onClick = { showFilterSheet = true }) {
                Icon(Icons.Outlined.FilterList, null)
            }
            if (filterValues.isNotEmpty()) {
                Badge(modifier = Modifier.align(Alignment.TopEnd)) {
                    Text(filterValues.size.toString())
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            filterValues = filterValues,
            onFilterAdd = onFilterAdd,
            onFilterRemove = onFilterRemove,
            onFilterChooseDone = {
                showFilterSheet = false
                onFilterChooseDone()
            },
            onFilterClear = onFilterClear,
            onDismiss = { showFilterSheet = false },
        )
    }
}

@Composable
private fun FilterBottomSheet(
    filterValues: List<FilterValue>,
    onFilterAdd: (FilterValue) -> Unit,
    onFilterRemove: (FilterValue) -> Unit,
    onFilterChooseDone: () -> Unit,
    onFilterClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(0) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                divider = {},
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.tag)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.date)) },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.rating)) },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
            ) {
                when (selectedTab) {
                    0 -> SearchTagFilter(
                        values = filterValues,
                        onValueAdd = onFilterAdd,
                        onValueRemove = onFilterRemove,
                    )

                    1 -> SearchDateFilter(
                        values = filterValues,
                        onValueAdd = onFilterAdd,
                        onValueRemove = onFilterRemove,
                    )

                    else -> SearchRatingFilter(
                        values = filterValues,
                        onValueAdd = onFilterAdd,
                        onValueRemove = onFilterRemove,
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = onFilterClear) {
                    Icon(Icons.Outlined.ClearAll, null)
                }
                FilledTonalButton(onClick = onFilterChooseDone) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchDateFilter(
    values: List<FilterValue>,
    onValueAdd: (FilterValue) -> Unit,
    onValueRemove: (FilterValue) -> Unit,
) {
    val currentValue = values.firstOrNull { it.key == "date" }
    val pickedYear = currentValue?.value?.split("-")?.getOrNull(0)?.toIntOrNull()
    val pickedMonth = currentValue?.value?.split("-")?.getOrNull(1)?.toIntOrNull()
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val monthOfPickedYear = if (pickedYear != null) {
        if (pickedYear == currentYear) Calendar.getInstance().get(Calendar.MONTH) + 1 else 12
    } else {
        0
    }

    FlowRow(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (year in 2014..currentYear) {
            if (year == pickedYear) {
                SearchFilterChip(
                    selected = true,
                    label = year.toString(),
                    onClick = { currentValue?.let(onValueRemove) },
                )
                for (month in 1..monthOfPickedYear) {
                    SearchFilterChip(
                        selected = month == pickedMonth,
                        label = "$year-$month",
                        onClick = {
                            if (month != pickedMonth) {
                                currentValue?.let(onValueRemove)
                                onValueAdd(FilterValue("date", "$year-$month"))
                            } else {
                                currentValue?.let(onValueRemove)
                            }
                        },
                    )
                }
            } else {
                SearchFilterChip(
                    selected = false,
                    label = year.toString(),
                    onClick = {
                        currentValue?.let(onValueRemove)
                        onValueAdd(FilterValue("date", "$year"))
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchRatingFilter(
    values: List<FilterValue>,
    onValueAdd: (FilterValue) -> Unit,
    onValueRemove: (FilterValue) -> Unit,
) {
    val currentValue = values.firstOrNull { it.key == "rating" }

    FlowRow(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SearchFilterChip(
            selected = currentValue?.value == "all",
            label = stringResource(R.string.rating_all),
            onClick = {
                if (currentValue?.value == "all") {
                    currentValue?.let(onValueRemove)
                } else {
                    currentValue?.let(onValueRemove)
                    onValueAdd(FilterValue("rating", "all"))
                }
            },
        )
        SearchFilterChip(
            selected = currentValue?.value == "general",
            label = stringResource(R.string.rating_general),
            onClick = {
                if (currentValue?.value == "general") {
                    currentValue?.let(onValueRemove)
                } else {
                    currentValue?.let(onValueRemove)
                    onValueAdd(FilterValue("rating", "general"))
                }
            },
        )
        SearchFilterChip(
            selected = currentValue?.value == "ecchi",
            label = stringResource(R.string.rating_ecchi),
            onClick = {
                if (currentValue?.value == "ecchi") {
                    currentValue?.let(onValueRemove)
                } else {
                    currentValue?.let(onValueRemove)
                    onValueAdd(FilterValue("rating", "ecchi"))
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchTagFilter(
    values: List<FilterValue>,
    onValueAdd: (FilterValue) -> Unit,
    onValueRemove: (FilterValue) -> Unit,
) {
    val searchRepository = get<SearchRepository>()
    val scope = rememberCoroutineScope()
    val defaultError = stringResource(R.string.search_error_default)
    val currentTags = values.filter { it.key == "tags" }
    var query by remember { mutableStateOf("") }
    var queryLoading by remember { mutableStateOf(false) }
    var queryActive by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val queryResult = remember { mutableStateListOf<String>() }

    fun search() {
        if (query.isBlank()) {
            queryResult.clear()
            return
        }
        scope.launch {
            queryLoading = true
            errorText = null
            runCatching {
                searchRepository.suggestTags(query)
            }.onSuccess { result ->
                queryResult.clear()
                queryResult.addAll(result)
            }.onFailure { throwable ->
                errorText = throwable.localizedMessage ?: defaultError
            }
            queryLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            currentTags.fastForEach { currentTag ->
                SearchFilterChip(
                    selected = true,
                    label = currentTag.value,
                    onClick = { onValueRemove(currentTag) },
                )
            }
        }

        DockedSearchBar(
            query = query,
            onQueryChange = { query = it },
            onSearch = { search() },
            active = queryActive,
            onActiveChange = { queryActive = it },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (queryActive) {
                        IconButton(onClick = { queryActive = false }) {
                            Icon(Icons.Outlined.Close, null)
                        }
                    }
                    if (!queryLoading) {
                        IconButton(onClick = { search() }) {
                            Icon(Icons.Outlined.Search, null)
                        }
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            },
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (!errorText.isNullOrBlank()) {
                    item {
                        Text(
                            text = errorText.orEmpty(),
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                items(queryResult, key = { it }) { tag ->
                    Text(
                        text = tag,
                        modifier = Modifier
                            .clickable {
                                onValueAdd(FilterValue("tags", tag))
                                queryActive = false
                                query = ""
                            }
                            .padding(8.dp)
                            .fillMaxWidth(),
                    )
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

@Composable
private fun MediaListModeButton(
    value: String,
    onValueChange: (String) -> Unit,
) {
    SelectButton(
        value = value,
        options = listOf(
            SelectOption(
                value = MEDIA_LIST_MODE_DETAIL,
                label = { Text(stringResource(R.string.media_list_mode_detail)) },
            ),
            SelectOption(
                value = MEDIA_LIST_MODE_THUMBNAIL,
                label = { Text(stringResource(R.string.media_list_mode_thumbnail)) },
            ),
        ),
        onValueChange = onValueChange,
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