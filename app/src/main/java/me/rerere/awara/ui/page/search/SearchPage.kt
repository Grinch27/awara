package me.rerere.awara.ui.page.search

// TODO(user): Decide whether saved views should surface here as first-class search presets once the search summary row settles.
// TODO(agent): If search gains server-side suggestion APIs later, replace the local-only quick history chips with ranked mixed suggestions instead of stacking another row.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.rerere.awara.R
import me.rerere.awara.ui.component.common.BackButton
import me.rerere.awara.ui.component.common.SelectButton
import me.rerere.awara.ui.component.common.SelectOption
import me.rerere.awara.ui.component.common.UiState
import me.rerere.awara.ui.component.common.UiStateBox
import me.rerere.awara.ui.component.ext.DynamicStaggeredGridCells
import me.rerere.awara.ui.component.iwara.MediaCard
import me.rerere.awara.ui.component.iwara.MediaListModeButton
import me.rerere.awara.ui.component.iwara.mediaListGridCells
import me.rerere.awara.ui.component.iwara.PaginationBar
import me.rerere.awara.ui.component.iwara.param.FilterAndSort
import me.rerere.awara.ui.component.iwara.param.FilterChipCloseIcon
import me.rerere.awara.ui.component.iwara.param.FilterValue
import me.rerere.awara.ui.component.iwara.param.sort.MediaSortOptions
import me.rerere.awara.ui.component.iwara.rememberMediaListModePreference
import me.rerere.awara.ui.component.iwara.UserCard
import me.rerere.compose_setting.preference.rememberStringPreference
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchPage(vm: SearchVM = koinViewModel()) {
    var listMode by rememberMediaListModePreference()
    var recentQueriesRaw by rememberStringPreference(key = "search.recent_queries", default = "")
    var searchBarActive by rememberSaveable { mutableStateOf(false) }
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
    val activeSort = when (vm.state.searchType) {
        "image" -> vm.imageSort
        "video" -> vm.videoSort
        else -> ""
    }
    val showSearchSummary = vm.query.isNotBlank() || activeFilters.isNotEmpty() || (
        vm.state.searchType != "user" && vm.state.uiState != UiState.Initial
    )

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.search))
                },
                navigationIcon = {
                    BackButton()
                }
            )
        },
        bottomBar = {
            PaginationBar(
                page = vm.state.page,
                limit = 32,
                total = vm.state.count,
                onPageChange = {
                     vm.jumpToPage(it)
                },
                leading = {
                    if (vm.state.searchType != "user") {
                        FilterAndSort(
                            sort = if (vm.state.searchType == "image") vm.imageSort else vm.videoSort,
                            onSortChange = {
                                if (vm.state.searchType == "image") {
                                    vm.updateImageSort(it)
                                } else {
                                    vm.updateVideoSort(it)
                                }
                            },
                            sortOptions = MediaSortOptions,
                            filterValues = if (vm.state.searchType == "image") vm.imageFilters else vm.videoFilters,
                            onFilterAdd = {
                                if (vm.state.searchType == "image") {
                                    vm.addImageFilter(it)
                                } else {
                                    vm.addVideoFilter(it)
                                }
                            },
                            onFilterRemove = {
                                if (vm.state.searchType == "image") {
                                    vm.removeImageFilter(it)
                                } else {
                                    vm.removeVideoFilter(it)
                                }
                            },
                            onFilterChooseDone = {
                                vm.submitSearch()
                            },
                            onFilterClear = {
                                if (vm.state.searchType == "image") {
                                    vm.clearImageFilter()
                                } else {
                                    vm.clearVideoFilter()
                                }
                            },
                        )
                    }
                },
                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                trailing = {
                    if (vm.state.searchType != "user") {
                        MediaListModeButton(
                            value = listMode,
                            onValueChange = { listMode = it },
                        )
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DockedSearchBar(
                        modifier = Modifier.fillMaxWidth(),
                        query = vm.query,
                        onQueryChange = { vm.query = it },
                        onSearch = { submitSearch() },
                        active = searchBarActive,
                        onActiveChange = { searchBarActive = it },
                        placeholder = {
                            Text(stringResource(R.string.search_hint_media))
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
                                    Text(
                                        text = stringResource(R.string.search_recent_title),
                                    )
                                    TextButton(
                                        onClick = {
                                            recentQueriesRaw = ""
                                        },
                                    ) {
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
                        leadingIcon = {
                            SelectButton(
                                value = vm.state.searchType,
                                options = SearchOptions,
                                onValueChange = {
                                    vm.updateSearchType(it)
                                }
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (searchBarActive && vm.query.isNotBlank()) {
                                        vm.query = ""
                                    } else {
                                        submitSearch()
                                    }
                                }
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
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SearchTypeChip(
                            selected = vm.state.searchType == "video",
                            label = stringResource(R.string.video),
                            onClick = { vm.updateSearchType("video") },
                        )
                        SearchTypeChip(
                            selected = vm.state.searchType == "image",
                            label = stringResource(R.string.image),
                            onClick = { vm.updateSearchType("image") },
                        )
                        SearchTypeChip(
                            selected = vm.state.searchType == "user",
                            label = stringResource(R.string.user),
                            onClick = { vm.updateSearchType("user") },
                        )
                    }

                    if (recentQueries.isNotEmpty() && !searchBarActive) {
                        Text(
                            text = stringResource(R.string.search_recent_quick_title),
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            items(
                                items = recentQueries,
                                key = { it },
                            ) { recentQuery ->
                                FilterChip(
                                    selected = recentQuery == vm.query,
                                    onClick = {
                                        vm.query = recentQuery
                                        submitSearch()
                                    },
                                    label = {
                                        Text(
                                            text = recentQuery,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.History, null)
                                    },
                                )
                            }
                        }
                    }

                    if (showSearchSummary) {
                        Divider()

                        SearchSummarySection(
                            query = vm.query,
                            searchType = vm.state.searchType,
                            activeSort = activeSort,
                            activeFilters = activeFilters,
                            onEditQuery = {
                                searchBarActive = true
                            },
                            onRemoveFilter = { filterValue ->
                                if (vm.state.searchType == "image") {
                                    vm.removeImageFilter(filterValue)
                                } else {
                                    vm.removeVideoFilter(filterValue)
                                }
                                vm.submitSearch()
                            },
                        )
                    }

                    Text(
                        text = stringResource(R.string.search_results_count, vm.state.count),
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge,
            ) {
                UiStateBox(
                    modifier = Modifier.fillMaxSize(),
                    state = vm.state.uiState,
                    onErrorRetry = {
                        vm.search()
                    }
                ) {
                    LazyVerticalStaggeredGrid(
                        columns = if (vm.state.searchType == "user") {
                            DynamicStaggeredGridCells(180.dp, 1, 2)
                        } else {
                            mediaListGridCells(listMode)
                        },
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (vm.state.searchType) {
                            "video" -> {
                                items(vm.state.videoList) {
                                    MediaCard(media = it, listMode = listMode)
                                }
                            }

                            "image" -> {
                                items(vm.state.imageList) {
                                    MediaCard(media = it, listMode = listMode)
                                }
                            }

                            "user" -> {
                                items(vm.state.userList) {
                                    UserCard(user = it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val SearchOptions = listOf(
    SelectOption(
        value = "video",
        label = {
            Text(stringResource(R.string.video))
        }
    ),
    SelectOption(
        value = "image",
        label = {
            Text(stringResource(R.string.image))
        }
    ),
    SelectOption(
        value = "user",
        label = {
            Text(stringResource(R.string.user))
        }
    )
)

@Composable
private fun SearchTypeChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(label)
        },
    )
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
            color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = androidx.compose.material3.MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.search_summary_filters_title),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
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
                                            mediaSortLabel(activeSort)
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
            items(
                items = activeFilters,
                key = { "${it.key}:${it.value}" },
            ) { filterValue ->
                FilterChip(
                    selected = true,
                    onClick = {
                        onRemoveFilter(filterValue)
                    },
                    label = {
                        Text(
                            text = formatSearchFilterValue(filterValue),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = FilterChipCloseIcon,
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