package me.rerere.awara.ui.page.index.pager

// TODO(user): Decide whether home feed filters should later gain presets or remain ad hoc.
// TODO(agent): Keep the pager footer focused on sort and list mode only.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import me.rerere.awara.ui.component.common.Spin
import me.rerere.awara.ui.component.common.UiStateBox
import me.rerere.awara.ui.component.iwara.MediaCard
import me.rerere.awara.ui.component.iwara.MediaListModeButton
import me.rerere.awara.ui.component.iwara.mediaListGridCells
import me.rerere.awara.ui.component.iwara.rememberMediaListModePreference
import me.rerere.awara.ui.component.iwara.param.FilterAndSort
import me.rerere.awara.ui.component.iwara.param.sort.MediaSortOptions
import me.rerere.awara.ui.page.index.IndexVM

@Composable
fun IndexVideoPage(vm: IndexVM) {
    val state = vm.state
    var listMode by rememberMediaListModePreference()
    val gridState = rememberLazyStaggeredGridState()

    LaunchedEffect(gridState, state.videoList.size, state.videoHasMore, state.videoLoadingMore) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }.collectLatest { lastVisibleIndex ->
            if (
                state.videoHasMore &&
                !state.videoLoadingMore &&
                state.videoList.isNotEmpty() &&
                lastVisibleIndex >= state.videoList.size - 6
            ) {
                vm.loadNextVideoPage()
            }
        }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                    vm.loadVideoList()
                },
            )

            MediaListModeButton(
                value = listMode,
                onValueChange = { listMode = it },
            )
        }

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
                state = gridState,
                columns = mediaListGridCells(listMode),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.videoList) {
                    MediaCard(media = it, listMode = listMode)
                }

                if (state.videoLoadingMore) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Spin()
                        }
                    }
                }
            }
        }
    }
}