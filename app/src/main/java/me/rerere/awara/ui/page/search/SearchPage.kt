package me.rerere.awara.ui.page.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import me.rerere.awara.R
import me.rerere.awara.ui.component.common.BackButton
import me.rerere.awara.ui.component.common.SelectButton
import me.rerere.awara.ui.component.common.SelectOption
import me.rerere.awara.ui.component.common.UiStateBox
import me.rerere.awara.ui.component.ext.DynamicStaggeredGridCells
import me.rerere.awara.ui.component.iwara.MediaCard
import me.rerere.awara.ui.component.iwara.MediaListModeButton
import me.rerere.awara.ui.component.iwara.mediaListGridCells
import me.rerere.awara.ui.component.iwara.PaginationBar
import me.rerere.awara.ui.component.iwara.param.FilterAndSort
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
            Surface(modifier = Modifier.fillMaxWidth()) {
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
                            IconButton(onClick = { submitSearch() }) {
                                Icon(Icons.Outlined.Search, null)
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

                    Divider()

                    Text(
                        text = stringResource(R.string.search_results_count, vm.state.count),
                    )
                }
            }

            UiStateBox(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
                    modifier = Modifier.matchParentSize()
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