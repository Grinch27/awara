package me.rerere.awara.ui.page.index.pager

// TODO(user): Decide whether image saved views should share the same pinned surface as video saved views.
// TODO(agent): If saved views later need thumbnails or presets, move this lightweight dialog flow into a dedicated saved-view feature instead of expanding the pager footer further.

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import me.rerere.awara.ui.component.iwara.MediaCard
import me.rerere.awara.ui.component.iwara.MediaListModeButton
import me.rerere.awara.ui.component.iwara.mediaListGridCells
import me.rerere.awara.ui.component.iwara.PaginationBar
import me.rerere.awara.ui.component.iwara.rememberMediaListModePreference
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
fun IndexImagePage(vm: IndexVM) {
    val state = vm.state
    val dialog = LocalDialogProvider.current
    val message = LocalMessageProvider.current
    val router = LocalRouterProvider.current
    val coroutineScope = rememberCoroutineScope()
    var saveDraft by remember { mutableStateOf(SavedFeedViewDraft()) }
    var listMode by rememberMediaListModePreference()

    Column {
        UiStateBox(
            state = state.imageState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            onErrorRetry = {
                vm.loadImageList()
            }
        ) {
            LazyVerticalStaggeredGrid(
                columns = mediaListGridCells(listMode),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
                modifier = Modifier.matchParentSize()
            ) {
                items(state.imageList) {
                    MediaCard(media = it, listMode = listMode)
                }
            }
        }

        PaginationBar(
            page = state.imagePage,
            limit = 24,
            total = state.imageCount,
            onPageChange = {
                vm.updateImagePage(it)
            },
            leading = {
                FilterAndSort(
                    sort = vm.imageSort,
                    onSortChange = {
                        vm.updateImageSort(it)
                    },
                    sortOptions = MediaSortOptions,
                    filterValues = vm.imageFilters,
                    onFilterAdd = vm::addImageFilter,
                    onFilterRemove = vm::removeImageFilter,
                    onFilterChooseDone = {
                        vm.loadImageList()
                    },
                    onFilterClear = {
                        vm.clearImageFilter()
                    },
                    savedViews = state.savedImageViews,
                    selectedSavedViewId = state.selectedImageSavedViewId,
                    onSavedViewSelected = vm::applyImageSavedView,
                    onManageSavedViews = {
                        router.navigate(savedFeedViewsRoute(FeedScope.HOME_IMAGE))
                    },
                    onSaveCurrentView = {
                        saveDraft = SavedFeedViewDraft()
                        dialog.show(
                            title = {
                                Text(stringResource(R.string.save_current_image_view_title))
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
                                        vm.saveCurrentImageView(
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
                                        AppLogger.e("IndexImagePage", "Failed to save image feed view", it)
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
            },
            trailing = {
                MediaListModeButton(
                    value = listMode,
                    onValueChange = { listMode = it },
                )
            }
        )
    }
}