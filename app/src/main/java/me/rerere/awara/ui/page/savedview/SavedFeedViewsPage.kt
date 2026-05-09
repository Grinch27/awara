package me.rerere.awara.ui.page.savedview

// TODO(user): Decide whether saved view management should eventually support batch actions like multi-delete or bulk retagging.
// TODO(agent): If this page keeps absorbing more feed administration tools, split list, filters, and edit flows into dedicated composables.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.VideoLabel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import kotlinx.coroutines.launch
import me.rerere.awara.R
import me.rerere.awara.domain.feed.FeedScope
import me.rerere.awara.domain.feed.SavedFeedView
import me.rerere.awara.ui.LocalDialogProvider
import me.rerere.awara.ui.LocalMessageProvider
import me.rerere.awara.ui.component.common.BackButton
import me.rerere.awara.ui.component.common.BetterTabBar
import me.rerere.awara.ui.page.index.SavedFeedViewDraft
import me.rerere.awara.ui.page.index.SavedFeedViewEditor
import me.rerere.awara.ui.page.index.normalizedTags
import me.rerere.awara.util.AppLogger
import org.koin.androidx.compose.koinViewModel

const val SAVED_FEED_VIEWS_ROUTE = "saved-views/{scope}"

fun savedFeedViewsRoute(scope: FeedScope): String = "saved-views/${scope.name}"

@Composable
fun SavedFeedViewsPage(
    initialScopeName: String,
    vm: SavedFeedViewsVM = koinViewModel(),
) {
    val dialog = LocalDialogProvider.current
    val message = LocalMessageProvider.current
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val initialScope = remember(initialScopeName) {
        runCatching { FeedScope.valueOf(initialScopeName) }.getOrDefault(FeedScope.HOME_VIDEO)
    }
    var selectedScopeName by rememberSaveable(initialScope.name) {
        mutableStateOf(initialScope.name)
    }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var editDraft by remember { mutableStateOf(SavedFeedViewDraft()) }
    val selectedScope = remember(selectedScopeName) { FeedScope.valueOf(selectedScopeName) }
    val state = vm.state
    val scopedViews = remember(state.views, selectedScope) {
        state.views.filter { it.scope == selectedScope }
    }
    val availableTags = remember(scopedViews) {
        scopedViews
            .flatMap(SavedFeedView::tags)
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map(Map.Entry<String, Int>::key)
    }
    val filteredViews = remember(scopedViews, selectedTag) {
        scopedViews.filter { savedView ->
            selectedTag == null || selectedTag in savedView.tags
        }
    }

    LaunchedEffect(selectedScopeName) {
        selectedTag = null
    }

    LaunchedEffect(availableTags, selectedTag) {
        if (selectedTag != null && selectedTag !in availableTags) {
            selectedTag = null
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(stringResource(R.string.saved_views_manage_title))
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        if (state.isLoading && state.views.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                BetterTabBar(selectedTabIndex = savedViewScopeTabs.indexOfFirst { it.scope == selectedScope }) {
                    savedViewScopeTabs.forEach { tab ->
                        Tab(
                            selected = selectedScope == tab.scope,
                            onClick = {
                                selectedScopeName = tab.scope.name
                            },
                            text = {
                                Text(stringResource(tab.titleRes))
                            },
                            icon = {
                                tab.icon()
                            },
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.saved_views_manage_summary,
                            scopedViews.size,
                            scopedViews.count(SavedFeedView::pinned),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (availableTags.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedTag == null,
                                    onClick = { selectedTag = null },
                                    label = {
                                        Text(stringResource(R.string.saved_views_manage_all_tags))
                                    },
                                )
                            }

                            items(availableTags, key = { it }) { tag ->
                                FilterChip(
                                    selected = selectedTag == tag,
                                    onClick = { selectedTag = tag },
                                    label = {
                                        Text("#$tag")
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (filteredViews.isEmpty()) {
                item {
                    Surface(
                        tonalElevation = 1.dp,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.saved_views_manage_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                        )
                    }
                }
            } else {
                itemsIndexed(items = filteredViews, key = { _, savedView -> savedView.id }) { index, savedView ->
                    SavedFeedViewManageCard(
                        savedView = savedView,
                        canMoveUp = savedView.pinned && filteredViews.getOrNull(index - 1)?.pinned == true,
                        canMoveDown = savedView.pinned && filteredViews.getOrNull(index + 1)?.pinned == true,
                        onEdit = {
                            editDraft = savedView.toDraft()
                            dialog.show(
                                title = {
                                    Text(stringResource(R.string.saved_views_manage_edit_action))
                                },
                                content = {
                                    SavedFeedViewEditor(
                                        draft = editDraft,
                                        onDraftChange = { editDraft = it },
                                    )
                                },
                                positiveText = {
                                    Text(stringResource(R.string.confirm))
                                },
                                positiveAction = {
                                    coroutineScope.launch {
                                        runCatching {
                                            vm.updateView(
                                                savedView.copy(
                                                    name = editDraft.name.trim().ifBlank { savedView.name },
                                                    description = editDraft.description.trim(),
                                                    tags = editDraft.normalizedTags(),
                                                    pinned = editDraft.pinned,
                                                    smartSubscription = editDraft.smartSubscription,
                                                    updatedAt = Instant.now(),
                                                ),
                                            )
                                        }.onSuccess {
                                            message.success {
                                                Text(stringResource(R.string.saved_views_manage_update_success, savedView.name))
                                            }
                                        }.onFailure {
                                            AppLogger.e("SavedFeedViewsPage", "Failed to update saved view", it)
                                            message.error {
                                                Text(stringResource(R.string.setting_data_action_failed))
                                            }
                                        }
                                    }
                                },
                                negativeText = {
                                    Text(stringResource(R.string.cancel))
                                },
                            )
                        },
                        onTogglePinned = {
                            coroutineScope.launch {
                                runCatching {
                                    vm.setPinned(savedView, !savedView.pinned)
                                }.onFailure {
                                    AppLogger.e("SavedFeedViewsPage", "Failed to toggle saved view pin", it)
                                    message.error {
                                        Text(stringResource(R.string.setting_data_action_failed))
                                    }
                                }
                            }
                        },
                        onMoveUp = {
                            coroutineScope.launch {
                                runCatching {
                                    vm.movePinnedView(scope = selectedScope, viewId = savedView.id, moveUp = true)
                                }.onFailure {
                                    AppLogger.e("SavedFeedViewsPage", "Failed to move pinned saved view up", it)
                                    message.error {
                                        Text(stringResource(R.string.setting_data_action_failed))
                                    }
                                }
                            }
                        },
                        onMoveDown = {
                            coroutineScope.launch {
                                runCatching {
                                    vm.movePinnedView(scope = selectedScope, viewId = savedView.id, moveUp = false)
                                }.onFailure {
                                    AppLogger.e("SavedFeedViewsPage", "Failed to move pinned saved view down", it)
                                    message.error {
                                        Text(stringResource(R.string.setting_data_action_failed))
                                    }
                                }
                            }
                        },
                        onDelete = {
                            dialog.show(
                                title = {
                                    Text(stringResource(R.string.saved_views_manage_delete_title))
                                },
                                content = {
                                    Text(stringResource(R.string.saved_views_manage_delete_text, savedView.name))
                                },
                                positiveText = {
                                    Text(stringResource(R.string.confirm))
                                },
                                positiveAction = {
                                    coroutineScope.launch {
                                        runCatching {
                                            vm.deleteView(savedView.id)
                                        }.onSuccess {
                                            message.success {
                                                Text(stringResource(R.string.saved_views_manage_delete_success, savedView.name))
                                            }
                                        }.onFailure {
                                            AppLogger.e("SavedFeedViewsPage", "Failed to delete saved view", it)
                                            message.error {
                                                Text(stringResource(R.string.setting_data_action_failed))
                                            }
                                        }
                                    }
                                },
                                negativeText = {
                                    Text(stringResource(R.string.cancel))
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedFeedViewManageCard(
    savedView: SavedFeedView,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onTogglePinned: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = savedView.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildSavedViewManageMeta(savedView),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (savedView.pinned) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = "#${savedView.pinOrder}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.saved_views_manage_edit_action))
                }
                TextButton(onClick = onTogglePinned) {
                    Text(
                        stringResource(
                            if (savedView.pinned) {
                                R.string.saved_views_manage_unpin_action
                            } else {
                                R.string.saved_views_manage_pin_action
                            },
                        ),
                    )
                }
                if (savedView.pinned) {
                    TextButton(onClick = onMoveUp, enabled = canMoveUp) {
                        Text(stringResource(R.string.saved_views_manage_move_up_action))
                    }
                    TextButton(onClick = onMoveDown, enabled = canMoveDown) {
                        Text(stringResource(R.string.saved_views_manage_move_down_action))
                    }
                }
                OutlinedButton(onClick = onDelete) {
                    Text(stringResource(R.string.saved_views_manage_delete_action))
                }
            }
        }
    }
}

@Composable
private fun buildSavedViewManageMeta(savedView: SavedFeedView): String {
    return buildList {
        if (savedView.smartSubscription) {
            add(stringResource(R.string.saved_views_smart_title))
        }
        if (savedView.pinned) {
            add("${stringResource(R.string.saved_view_meta_pinned)} #${savedView.pinOrder}")
        }
        if (savedView.filters.isNotEmpty()) {
            add(stringResource(R.string.saved_view_meta_filters, savedView.filters.size))
        }
        if (savedView.tags.isNotEmpty()) {
            add(savedView.tags.joinToString(separator = "  ") { tag -> "#$tag" })
        }
        savedView.description.trim().takeIf(String::isNotEmpty)?.let(::add)
    }.joinToString(separator = " · ")
}

private fun SavedFeedView.toDraft(): SavedFeedViewDraft {
    return SavedFeedViewDraft(
        name = name,
        tagsText = tags.joinToString(separator = ", "),
        description = description,
        smartSubscription = smartSubscription,
        pinned = pinned,
    )
}

private data class SavedViewScopeTab(
    val scope: FeedScope,
    val titleRes: Int,
    val icon: @Composable () -> Unit,
)

private val savedViewScopeTabs = listOf(
    SavedViewScopeTab(
        scope = FeedScope.HOME_VIDEO,
        titleRes = R.string.index_nav_video,
        icon = {
            androidx.compose.material3.Icon(Icons.Outlined.VideoLabel, contentDescription = null)
        },
    ),
    SavedViewScopeTab(
        scope = FeedScope.HOME_IMAGE,
        titleRes = R.string.index_nav_image,
        icon = {
            androidx.compose.material3.Icon(Icons.Outlined.Image, contentDescription = null)
        },
    ),
)