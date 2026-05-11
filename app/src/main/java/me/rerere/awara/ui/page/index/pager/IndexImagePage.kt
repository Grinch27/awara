package me.rerere.awara.ui.page.index.pager

// TODO(user): Decide whether home feed filters should later gain presets or remain ad hoc.
// TODO(agent): Keep the pager footer focused on sort and list mode only.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.awara.ui.component.common.UiStateBox
import me.rerere.awara.ui.component.iwara.MediaCard
import me.rerere.awara.ui.component.iwara.MediaListModeButton
import me.rerere.awara.ui.component.iwara.mediaListGridCells
import me.rerere.awara.ui.component.iwara.PaginationBar
import me.rerere.awara.ui.component.iwara.rememberMediaListModePreference
import me.rerere.awara.ui.component.iwara.param.FilterAndSort
import me.rerere.awara.ui.component.iwara.param.sort.MediaSortOptions
import me.rerere.awara.ui.page.index.IndexVM

@Composable
fun IndexImagePage(vm: IndexVM) {
    val state = vm.state
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