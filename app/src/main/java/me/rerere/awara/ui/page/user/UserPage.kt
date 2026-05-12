package me.rerere.awara.ui.page.user

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import me.rerere.awara.R
import me.rerere.awara.data.dto.FriendStatus
import me.rerere.awara.data.dto.ProfileDto
import me.rerere.awara.data.source.stringResource as apiErrorString
import me.rerere.awara.data.entity.toHeaderUrl
import me.rerere.awara.ui.component.common.BackButton
import me.rerere.awara.ui.component.common.BetterTabBar
import me.rerere.awara.ui.component.common.Button
import me.rerere.awara.ui.component.common.ButtonType
import me.rerere.awara.ui.component.common.EmptyStatus
import me.rerere.awara.ui.component.common.ImageAppBar
import me.rerere.awara.ui.component.common.pullrefresh.SwipeRefresh
import me.rerere.awara.ui.component.common.pullrefresh.rememberSwipeRefreshState
import me.rerere.awara.ui.component.ext.excludeBottom
import me.rerere.awara.ui.component.iwara.Avatar
import me.rerere.awara.ui.component.iwara.comment.CommentCard
import me.rerere.awara.ui.component.iwara.LoadMoreEffect
import me.rerere.awara.ui.component.iwara.MediaCard
import me.rerere.awara.ui.component.iwara.mediaListGridCells
import me.rerere.awara.ui.component.iwara.rememberMediaListModePreference
import me.rerere.awara.ui.component.iwara.RichText
import me.rerere.awara.ui.component.iwara.UserStatus
import me.rerere.awara.ui.component.iwara.loadMoreFooter
import me.rerere.awara.util.openUrl
import org.koin.androidx.compose.koinViewModel

@Composable
fun UserPage(
    vm: UserVM = koinViewModel()
) {
    val profileState = vm.state.profile
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expand by remember { mutableStateOf(false) }
    val listMode by rememberMediaListModePreference()
    Scaffold(
        topBar = {
            if (expand) {
                ImageAppBar(
                    title = {
                        Text(
                            text = profileState?.user?.name ?: "",
                        )
                    },
                    navigationIcon = {
                        BackButton()
                    },
                    image = {
                        AsyncImage(
                            model = profileState?.header.toHeaderUrl(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    },
                    //scrollBehavior = appBarState,
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = profileState?.user?.name ?: "",
                        )
                    },
                    navigationIcon = {
                        BackButton()
                    },
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it.excludeBottom()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            UserCard(
                vm = vm,
                profileState = profileState,
                expand = expand,
                onChangeExpand = { expand = it },
                onFollow = {
                    vm.followOrUnfollow()
                },
                onFriend = {
                    vm.addOrRemoveFriend()
                }
            )

            Column {
                val pagerState = rememberPagerState(pageCount = { 3 })
                BetterTabBar(selectedTabIndex = pagerState.currentPage) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        text = {
                            Text("视频")
                        }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        text = {
                            Text("图片")
                        }
                    )
                    Tab(
                        selected = pagerState.currentPage == 2,
                        onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                        text = {
                            Text("留言")
                        }
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> {
                            VideoPage(
                                vm = vm,
                                listMode = listMode,
                            )
                        }

                        1 -> {
                            ImagePage(
                                vm = vm,
                                listMode = listMode,
                            )
                        }

                        2 -> {
                            UserGuestbookPage(
                                vm = vm,
                                profileState = profileState,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserGuestbookPage(
    vm: UserVM,
    profileState: ProfileDto?,
) {
    val context = LocalContext.current
    val username = profileState?.user?.username?.takeIf { it.isNotBlank() }
    val browserUrl = username?.let { "https://www.iwara.tv/profile/$it" }
    val guestbookState = vm.state.guestbookCommentState.stack.last()
    val guestbookError = vm.state.guestbookError
    val guestbookExceptionMessage = vm.state.guestbookExceptionMessage
    val listState = rememberLazyListState()

    LoadMoreEffect(
        listState = listState,
        itemCount = guestbookState.comments.size,
        hasMore = guestbookState.hasMore,
        loadingMore = guestbookState.loadingMore,
        onLoadMore = vm::loadNextGuestbookPage,
    )

    Column {
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = vm.state.guestbookCommentState.loading),
            onRefresh = vm::loadGuestbookComments,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
            ) {
                when {
                    guestbookError != null || guestbookExceptionMessage != null -> {
                        UserGuestbookErrorState(
                            message = guestbookError?.let { apiErrorString(it) }
                                ?: guestbookExceptionMessage
                                ?: stringResource(R.string.errors_unknown, "guestbook"),
                            browserUrl = browserUrl,
                            onRetry = vm::loadGuestbookComments,
                        )
                    }

                    guestbookState.comments.isEmpty() -> {
                        EmptyStatus()
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            lazyItems(guestbookState.comments, key = { it.id }) { comment ->
                                CommentCard(
                                    comment = comment,
                                    onLoadReplies = null,
                                    onReply = null,
                                )
                            }

                            loadMoreFooter(guestbookState.loadingMore)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserGuestbookErrorState(
    message: String,
    browserUrl: String?,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
            ),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.user_guestbook_message),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = stringResource(R.string.user_guestbook_tip),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onRetry,
                        type = ButtonType.Default,
                    ) {
                        Text(stringResource(R.string.user_guestbook_retry_action))
                    }

                    if (browserUrl != null) {
                        Button(
                            onClick = { context.openUrl(browserUrl) },
                            type = ButtonType.Outlined,
                        ) {
                            Text(stringResource(R.string.user_guestbook_open_action))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    vm: UserVM,
    profileState: ProfileDto?,
    expand: Boolean,
    onChangeExpand: (Boolean) -> Unit,
    onFollow: () -> Unit,
    onFriend: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Avatar(
                user = profileState?.user,
                modifier = Modifier
                    .size(48.dp)
            )

            Column {
                Text(
                    text = profileState?.user?.name ?: "",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "@" + (profileState?.user?.username ?: ""),
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(modifier = Modifier.height(3.dp))
                
                profileState?.user?.let {
                    UserStatus(user = it)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    onFriend()
                },
                type = when (vm.state.friendStatus) {
                    FriendStatus.NONE -> ButtonType.Default
                    FriendStatus.PENDING -> ButtonType.Outlined
                    FriendStatus.FRIENDS -> ButtonType.Outlined
                },
                loading = vm.state.friendLoading
            ) {
                Text(
                    text = when (vm.state.friendStatus) {
                        FriendStatus.NONE -> stringResource(R.string.friends)
                        FriendStatus.PENDING -> stringResource(R.string.friends_pending)
                        FriendStatus.FRIENDS -> stringResource(R.string.friends_friends)
                    },
                )
            }

            Button(
                onClick = {
                    onFollow()
                },
                type = if (profileState?.user?.following == true) ButtonType.Outlined else ButtonType.Default,
                loading = vm.state.followLoading
            ) {
                Text(
                    text = if (profileState?.user?.following == true) "已关注" else "关注",
                )
            }
        }


        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            RichText(
                text = profileState?.body ?: "",
                onLinkClick = { url ->
                    context.openUrl(url)
                },
                maxLines = if (expand) Int.MAX_VALUE else 1,
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize(),
                overflow = TextOverflow.Ellipsis,
            )


            Icon(
                if (expand) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        onChangeExpand(!expand)
                    }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun VideoPage(
    vm: UserVM,
    listMode: String,
) {
    val gridState = rememberLazyStaggeredGridState()
    LoadMoreEffect(
        gridState = gridState,
        itemCount = vm.state.videoList.size,
        hasMore = vm.state.videoHasMore,
        loadingMore = vm.state.videoLoadingMore,
        onLoadMore = vm::loadNextVideoPage,
    )
    Column {
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = vm.state.videoLoading),
            onRefresh = {
                vm.loadVideoList()
            },
            modifier = Modifier.weight(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (vm.state.videoList.isEmpty()) {
                    EmptyStatus()
                } else {
                    LazyVerticalStaggeredGrid(
                        state = gridState,
                        columns = mediaListGridCells(listMode),
                        modifier = Modifier.fillMaxSize(),
                        verticalItemSpacing = 8.dp,
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(vm.state.videoList) {
                            MediaCard(media = it, listMode = listMode)
                        }

                        loadMoreFooter(vm.state.videoLoadingMore)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagePage(
    vm: UserVM,
    listMode: String,
) {
    val gridState = rememberLazyStaggeredGridState()
    LoadMoreEffect(
        gridState = gridState,
        itemCount = vm.state.imageList.size,
        hasMore = vm.state.imageHasMore,
        loadingMore = vm.state.imageLoadingMore,
        onLoadMore = vm::loadNextImagePage,
    )
    Column {
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = vm.state.imageLoading),
            onRefresh = {
                vm.loadImageList()
            },
            modifier = Modifier.weight(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (vm.state.imageList.isEmpty()) {
                    EmptyStatus()
                } else {
                    LazyVerticalStaggeredGrid(
                        state = gridState,
                        columns = mediaListGridCells(listMode),
                        modifier = Modifier.fillMaxSize(),
                        verticalItemSpacing = 8.dp,
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(vm.state.imageList) {
                            MediaCard(media = it, listMode = listMode)
                        }

                        loadMoreFooter(vm.state.imageLoadingMore)
                    }
                }
            }
        }
    }
}
