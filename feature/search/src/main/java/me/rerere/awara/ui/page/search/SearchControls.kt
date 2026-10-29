// Requirements: compact search controls; A-Z/0-9 tag browsing remains available.
// Verify: cancellation, empty/repeated pages, date YYYY and YYYY-MM.
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
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.CancellationException
import me.rerere.awara.ui.component.iwara.*
import me.rerere.awara.domain.media.MediaCardModel

private val TAG_BROWSE_FILTERS = ('A'..'Z').map(Char::toString) + ('0'..'9').map(Char::toString)

@Composable
internal fun SearchTypeChip(
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
internal fun SearchHeaderIconButton(
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
internal fun SortDropdown(
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
internal fun MediaSortIcon(sort: String) {
    val icon = when (sort) {
        "views" -> Icons.Outlined.RemoveRedEye
        "likes" -> Icons.Outlined.Favorite
        "popularity" -> Icons.Outlined.Star
        else -> Icons.Outlined.CalendarMonth
    }
    Icon(icon, null)
}

@Composable
internal fun UserSearchHintBanner(modifier: Modifier = Modifier) {
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
internal fun DateDropdown(
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
internal fun RatingDropdown(
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
internal fun TagBrowseButton(
    modifier: Modifier = Modifier,
    selectedTags: List<FilterValue>,
    onTagSelected: (String) -> Unit,
    onTagRemove: (FilterValue) -> Unit,
) {
    val searchRepository = koinInject<SearchRepository>()
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
    var requestGeneration by remember { mutableIntStateOf(0) }
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
        val requestId = ++requestGeneration
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
                if (selectedFilter != requestFilter || requestId != requestGeneration) {
                    return@onSuccess
                }
                val mergedTags = if (replaceResults) {
                    result.results
                } else {
                    tags + result.results
                }
                tags.clear()
                tags.addAll(mergedTags.distinct())
                page = requestPage
                count = result.count
                hasMore = result.results.isNotEmpty() && mergedTags.distinct().size > (if (replaceResults) 0 else mergedTags.size - result.results.size) && tags.size < result.count
                errorText = null
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                if (selectedFilter != requestFilter || requestId != requestGeneration) {
                    return@onFailure
                }
                if (replaceResults) {
                    errorText = throwable.localizedMessage ?: defaultError
                }
            }
            if (selectedFilter == requestFilter && requestId == requestGeneration) {
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

internal fun tagBrowseFilterFor(query: String): String? {
    val firstSearchChar = query.firstOrNull { it.isLetterOrDigit() } ?: return null
    val upperChar = firstSearchChar.uppercaseChar()
    return when {
        upperChar in 'A'..'Z' -> upperChar.toString()
        firstSearchChar in '0'..'9' -> firstSearchChar.toString()
        else -> null
    }
}

@Composable
internal fun searchTypeLabel(type: String): String {
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
internal fun mediaSortLabel(sort: String): String {
    return when (sort) {
        "trending" -> stringResource(R.string.sort_trending)
        "popularity" -> stringResource(R.string.sort_popularity)
        "views" -> stringResource(R.string.sort_views)
        "likes" -> stringResource(R.string.sort_likes)
        else -> stringResource(R.string.sort_date)
    }
}

@Composable
internal fun mediaRatingLabel(rating: String): String {
    return when (rating) {
        "ecchi" -> stringResource(R.string.rating_ecchi)
        "general" -> stringResource(R.string.rating_general)
        else -> stringResource(R.string.rating_all)
    }
}

