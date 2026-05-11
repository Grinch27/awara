package me.rerere.awara.ui.page.index.pager

// TODO(user): Decide whether home feed filters should later gain presets or remain ad hoc.
// TODO(agent): Keep the pager footer focused on sort and list mode only.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.awara.ui.component.common.UiStateBox
import me.rerere.awara.ui.component.iwara.MediaCard
import me.rerere.awara.ui.component.iwara.MediaListModeButton
import me.rerere.awara.ui.component.iwara.mediaListGridCells
import me.rerere.awara.ui.component.iwara.rememberMediaListModePreference
import me.rerere.awara.ui.component.iwara.PaginationBar
import me.rerere.awara.ui.component.iwara.param.FilterAndSort
import me.rerere.awara.ui.component.iwara.param.sort.MediaSortOptions
import me.rerere.awara.ui.page.index.IndexVM

@Composable
fun IndexVideoPage(vm: IndexVM) {
    val state = vm.state
    var listMode by rememberMediaListModePreference()

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
                columns = mediaListGridCells(listMode),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.videoList) {
                    MediaCard(media = it, listMode = listMode)
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