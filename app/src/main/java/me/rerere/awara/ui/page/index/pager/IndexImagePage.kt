package me.rerere.awara.ui.page.index.pager

// TODO(user): Decide whether home feed filters should later gain presets or remain ad hoc.
// TODO(agent): Keep page-local controls focused on feed filters; display mode is configured in Settings.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import me.rerere.awara.ui.component.common.Spin
import me.rerere.awara.ui.component.common.UiStateBox
import me.rerere.awara.ui.component.iwara.MediaCard
import me.rerere.awara.ui.component.iwara.mediaListGridCells
import me.rerere.awara.ui.component.iwara.rememberMediaListModePreference
import me.rerere.awara.ui.page.index.IndexVM

@Composable
fun IndexImagePage(vm: IndexVM) {
    val state = vm.state
    val listMode by rememberMediaListModePreference()
    val gridState = rememberLazyStaggeredGridState()

    LaunchedEffect(gridState, state.imageList.size, state.imageHasMore, state.imageLoadingMore) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }.collectLatest { lastVisibleIndex ->
            if (
                state.imageHasMore &&
                !state.imageLoadingMore &&
                state.imageList.isNotEmpty() &&
                lastVisibleIndex >= state.imageList.size - 6
            ) {
                vm.loadNextImagePage()
            }
        }
    }

    UiStateBox(
        state = state.imageState,
        modifier = Modifier.fillMaxSize(),
        onErrorRetry = {
            vm.loadImageList()
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
            items(state.imageList) {
                MediaCard(media = it, listMode = listMode)
            }

            if (state.imageLoadingMore) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Spin(
                        show = true,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}