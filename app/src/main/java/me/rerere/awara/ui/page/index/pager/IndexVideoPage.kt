package me.rerere.awara.ui.page.index.pager

// TODO(user): Decide whether video saved views should support pinning directly from the home feed.
// TODO(agent): If home feed saved views turn into a primary navigation model, lift this action out of the pager footer and into a shared saved-view entry point.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import kotlinx.coroutines.launch
import me.rerere.awara.R
import me.rerere.awara.ui.LocalDialogProvider
import me.rerere.awara.ui.LocalMessageProvider
import me.rerere.awara.ui.LocalRouterProvider
import me.rerere.awara.ui.component.common.UiStateBox
import me.rerere.awara.ui.component.ext.DynamicStaggeredGridCells
import me.rerere.awara.ui.component.iwara.MediaCard
import me.rerere.awara.ui.component.iwara.PaginationBar
import me.rerere.awara.ui.component.iwara.param.FilterAndSort
import me.rerere.awara.ui.page.savedview.savedFeedViewsRoute
import me.rerere.awara.domain.feed.FeedScope
import me.rerere.awara.ui.component.iwara.param.sort.MediaSortOptions
import me.rerere.awara.ui.page.index.SavedFeedViewDraft
import me.rerere.awara.ui.page.index.SavedFeedViewEditor
import me.rerere.awara.ui.page.index.IndexVM
import me.rerere.awara.ui.page.index.normalizedTags
import me.rerere.awara.util.AppLogger

@Composable
fun IndexVideoPage(vm: IndexVM) {
    val state = vm.state
    val dialog = LocalDialogProvider.current
    val message = LocalMessageProvider.current
    val router = LocalRouterProvider.current
    val coroutineScope = rememberCoroutineScope()
    var saveDraft by remember { mutableStateOf(SavedFeedViewDraft()) }

    Column {
        UiStateBox(
            state = state.videoState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            onErrorRetry = {
                vm.loadVideoList()
            }
        ) {
            LazyVerticalStaggeredGrid(
                columns = DynamicStaggeredGridCells(150.dp, 2, 4),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
                modifier = Modifier.matchParentSize()
            ) {
                items(state.videoList) {
                    MediaCard(media = it)
                }
            }
        }

        PaginationBar(
            page = state.videoPage,
            limit = 24,
            total = state.videoCount,
            onPageChange = {
                vm.updateVideoPage(it)
            },
            leading = {
                FilterAndSort(
                    sort = vm.videoSort,
                    onSortChange = {
                        vm.updateVideoSort(it)
                    },
                    sortOptions = MediaSortOptions,
                    filterValues = vm.videoFilters,
                    onFilterAdd = vm::addVideoFilter,
                    onFilterRemove = vm::removeVideoFilter,
                    onFilterChooseDone = {
                        vm.loadVideoList()
                    },
                    onFilterClear = {
                        vm.clearVideoFilter()
                    },
                    savedViews = state.savedVideoViews,
                    selectedSavedViewId = state.selectedVideoSavedViewId,
                    onSavedViewSelected = vm::applyVideoSavedView,
                    onManageSavedViews = {
                        router.navigate(savedFeedViewsRoute(FeedScope.HOME_VIDEO))
                    },
                    onSaveCurrentView = {
                        saveDraft = SavedFeedViewDraft()
                        dialog.show(
                            title = {
                                Text(stringResource(R.string.save_current_video_view_title))
                            },
                            content = {
                                SavedFeedViewEditor(
                                    draft = saveDraft,
                                    onDraftChange = { saveDraft = it },
                                )
                            },
                            positiveText = {
                                Text(stringResource(R.string.confirm))
                            },
                            positiveAction = {
                                coroutineScope.launch {
                                    runCatching {
                                        vm.saveCurrentVideoView(
                                            name = saveDraft.name,
                                            description = saveDraft.description,
                                            tags = saveDraft.normalizedTags(),
                                            pinned = saveDraft.pinned,
                                            smartSubscription = saveDraft.smartSubscription,
                                        )
                                    }.onSuccess { savedView ->
                                        message.success {
                                            Text(stringResource(R.string.save_current_view_success, savedView.name))
                                        }
                                    }.onFailure {
                                        AppLogger.e("IndexVideoPage", "Failed to save video feed view", it)
                                        message.error {
                                            Text(stringResource(R.string.save_current_view_failure))
                                        }
                                    }
                                }
                            },
                            negativeText = {
                                Text(stringResource(R.string.cancel))
                            }
                        )
                    },
                )
            }
        )
    }
}