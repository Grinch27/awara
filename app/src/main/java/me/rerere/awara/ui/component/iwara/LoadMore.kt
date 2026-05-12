package me.rerere.awara.ui.component.iwara

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import me.rerere.awara.ui.component.common.Spin

@Composable
fun LoadMoreEffect(
    listState: LazyListState,
    itemCount: Int,
    hasMore: Boolean,
    loadingMore: Boolean,
    threshold: Int = 6,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(listState, itemCount, hasMore, loadingMore, threshold) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }.collectLatest { lastVisibleIndex ->
            if (hasMore && !loadingMore && itemCount > 0 && lastVisibleIndex >= itemCount - threshold) {
                onLoadMore()
            }
        }
    }
}

@Composable
fun LoadMoreEffect(
    gridState: LazyStaggeredGridState,
    itemCount: Int,
    hasMore: Boolean,
    loadingMore: Boolean,
    threshold: Int = 6,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(gridState, itemCount, hasMore, loadingMore, threshold) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }.collectLatest { lastVisibleIndex ->
            if (hasMore && !loadingMore && itemCount > 0 && lastVisibleIndex >= itemCount - threshold) {
                onLoadMore()
            }
        }
    }
}

fun LazyListScope.loadMoreFooter(loadingMore: Boolean) {
    if (loadingMore) {
        item {
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

fun LazyStaggeredGridScope.loadMoreFooter(loadingMore: Boolean) {
    if (loadingMore) {
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
