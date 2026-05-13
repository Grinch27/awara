package me.rerere.awara.ui.page.forum

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.rerere.awara.ui.LocalRouterProvider
import me.rerere.awara.ui.component.common.UiStateBox
import me.rerere.awara.ui.component.iwara.LoadMoreEffect
import me.rerere.awara.ui.component.iwara.loadMoreFooter
import org.koin.androidx.compose.koinViewModel

@Composable
fun ForumThreadPage(
    vm: ForumThreadVM = koinViewModel(),
) {
    val router = LocalRouterProvider.current
    val listState = rememberLazyListState()

    LoadMoreEffect(
        listState = listState,
        itemCount = vm.state.posts.size,
        hasMore = vm.state.hasMore,
        loadingMore = vm.state.loadingMore,
        onLoadMore = vm::loadNextPage,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = vm.state.thread.title.ifBlank { vm.threadId },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { router.popBackStack() }) {
                        Icon(Icons.Outlined.ArrowBack, null)
                    }
                },
            )
        },
    ) { innerPadding ->
        UiStateBox(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            state = vm.state.uiState,
            onErrorRetry = { vm.loadPosts() },
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 10.dp,
                    top = innerPadding.calculateTopPadding() + 10.dp,
                    end = 10.dp,
                    bottom = innerPadding.calculateBottomPadding() + 10.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "thread-header") {
                    ForumThreadHeader(
                        thread = vm.state.thread,
                        totalPosts = vm.state.count,
                        pendingCount = vm.state.pendingCount,
                        onOpenUser = { user ->
                            if (user.hasNavigableProfile) {
                                router.navigate("user/${Uri.encode(user.username)}")
                            }
                        },
                    )
                }
                itemsIndexed(
                    vm.state.posts,
                    key = { index, post -> "${post.id}:$index" },
                ) { index, post ->
                    ForumPostCard(
                        post = post,
                        index = index,
                        onOpenUser = { user ->
                            if (user.hasNavigableProfile) {
                                router.navigate("user/${Uri.encode(user.username)}")
                            }
                        },
                    )
                }
                loadMoreFooter(vm.state.loadingMore)
            }
        }
    }
}
