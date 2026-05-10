package me.rerere.awara.ui.page.video.layout

// TODO(user): Decide whether the phone detail stream should eventually insert chapter-like anchors for comments and related videos once more metadata blocks exist.
// TODO(agent): If tablet later also converges to one stream, extract this phone-only feed assembly into a shared detail body instead of duplicating section order twice.

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.rerere.awara.R
import me.rerere.awara.ui.component.common.BackButton
import me.rerere.awara.ui.component.common.Spin
import me.rerere.awara.ui.component.ext.excludeBottom
import me.rerere.awara.ui.component.iwara.MediaCard
import me.rerere.awara.ui.component.iwara.rememberMediaListModePreference
import me.rerere.awara.ui.component.iwara.comment.EmbeddedCommentSection
import me.rerere.awara.ui.component.player.PlayerState
import me.rerere.awara.ui.page.video.VideoVM
import me.rerere.awara.ui.page.video.pager.VideoOverviewHeaderSection

@Composable
fun VideoPagePhoneLayout(vm: VideoVM, state: PlayerState, player: @Composable () -> Unit) {
    var offset by remember { mutableStateOf(0f) }
    val listMode by rememberMediaListModePreference()
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (consumed.y == 0f && available.y > 0f) {
                    offset = 0f
                } else {
                    offset += consumed.y
                }
                return super.onPostScroll(consumed, available, source)
            }
        }
    }
    Scaffold(
        topBar = {
            Box(modifier = Modifier.animateContentSize()) {
                if (offset != 0f && !state.playing) {
                    TopAppBar(
                        title = {
                            Text(text = "视频详情")
                        },
                        navigationIcon = {
                            BackButton()
                        },
                        modifier = Modifier
                            .background(Color.Black)
                            .statusBarsPadding()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .background(Color.Black)
                            .statusBarsPadding()
                            .fillMaxWidth()
                            .aspectRatio(16 / 9f)
                    ) {
                        player()
                    }
                }
            }
        }
    ) { innerPadding ->
        Spin(
            show = vm.state.loading,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding.excludeBottom())
                    .nestedScroll(nestedScrollConnection)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    top = 8.dp,
                    end = 8.dp,
                    bottom = navigationBarPadding.calculateBottomPadding() + 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    VideoOverviewHeaderSection(vm = vm)
                }

                if (!vm.state.private) {
                    item {
                        EmbeddedCommentSection(
                            state = vm.state.commentState,
                            contentPadding = navigationBarPadding,
                            onPageChange = vm::jumpCommentPage,
                            onBack = vm::popComment,
                            onPush = { vm.pushComment(it) },
                            onPostReply = { vm.postComment(it) },
                        )
                    }

                    if (vm.state.relatedVideos.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.video_related_title),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            )
                        }

                        items(
                            items = vm.state.relatedVideos,
                            key = { it.id },
                        ) { relatedVideo ->
                            MediaCard(
                                media = relatedVideo,
                                listMode = listMode,
                            )
                        }
                    }
                }
            }
        }
    }
}