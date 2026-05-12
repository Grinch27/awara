package me.rerere.awara.ui.page.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.rerere.awara.R
import me.rerere.awara.ui.component.common.BackButton
import me.rerere.awara.ui.component.common.Spin
import me.rerere.awara.ui.component.iwara.LoadMoreEffect
import me.rerere.awara.ui.component.iwara.MediaCard
import me.rerere.awara.ui.component.iwara.mediaListGridCells
import me.rerere.awara.ui.component.iwara.rememberMediaListModePreference
import me.rerere.awara.ui.component.iwara.loadMoreFooter
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlaylistDetailPage(vm: PlaylistDetailVM = koinViewModel()) {
    val appbarBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listMode by rememberMediaListModePreference()
    val gridState = rememberLazyStaggeredGridState()
    LoadMoreEffect(
        gridState = gridState,
        itemCount = vm.state.list.size,
        hasMore = vm.state.hasMore,
        loadingMore = vm.state.loadingMore,
        onLoadMore = vm::loadNextPage,
    )
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(vm.state.playlist?.title ?: stringResource(R.string.playlist))
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = appbarBehavior
            )
        }
    ) {
        Spin(
            show = vm.state.loading,
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            LazyVerticalStaggeredGrid(
                state = gridState,
                columns = mediaListGridCells(listMode),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
                modifier = Modifier
                    .matchParentSize()
                    .nestedScroll(appbarBehavior.nestedScrollConnection)
            ) {
                items(vm.state.list) { video ->
                    MediaCard(media = video, listMode = listMode)
                }

                loadMoreFooter(vm.state.loadingMore)
            }
        }
    }
}