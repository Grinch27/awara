package me.rerere.awara.ui.page.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.rerere.awara.R
import me.rerere.awara.ui.component.common.BackButton
import me.rerere.awara.ui.component.common.BetterTabBar
import me.rerere.awara.ui.component.common.Spin
import me.rerere.awara.ui.component.ext.excludeBottom
import me.rerere.awara.ui.component.iwara.LoadMoreEffect
import me.rerere.awara.ui.component.iwara.MediaCard
import me.rerere.awara.ui.component.iwara.mediaListGridCells
import me.rerere.awara.ui.component.iwara.rememberMediaListModePreference
import me.rerere.awara.ui.component.iwara.loadMoreFooter
import org.koin.androidx.compose.koinViewModel

@Composable
fun FavoritesPage(vm: FavoritesVM = koinViewModel()) {
    val appbarBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val listMode by rememberMediaListModePreference()
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(stringResource(R.string.favorites))
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = appbarBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding.excludeBottom())
        ) {
            BetterTabBar(
                selectedTabIndex = pagerState.currentPage
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = {
                        Text(stringResource(R.string.video))
                    }
                )

                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = {
                        Text(stringResource(R.string.image))
                    }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> {
                        val gridState = rememberLazyStaggeredGridState()
                        LoadMoreEffect(
                            gridState = gridState,
                            itemCount = vm.state.videoList.size,
                            hasMore = vm.state.videoHasMore,
                            loadingMore = vm.state.videoLoadingMore,
                            onLoadMore = vm::loadNextVideoPage,
                        )
                        Column {
                            Spin(
                                show = vm.state.videoLoading,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
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
                                    items(vm.state.videoList) { video ->
                                        MediaCard(media = video.video, listMode = listMode)
                                    }

                                    loadMoreFooter(vm.state.videoLoadingMore)
                                }
                            }
                        }
                    }

                    1 -> {
                        val gridState = rememberLazyStaggeredGridState()
                        LoadMoreEffect(
                            gridState = gridState,
                            itemCount = vm.state.imageList.size,
                            hasMore = vm.state.imageHasMore,
                            loadingMore = vm.state.imageLoadingMore,
                            onLoadMore = vm::loadNextImagePage,
                        )
                        Column {
                            Spin(
                                show = vm.state.imageLoading,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
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
                                    items(vm.state.imageList) { image ->
                                        MediaCard(media = image.image, listMode = listMode)
                                    }

                                    loadMoreFooter(vm.state.imageLoadingMore)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}