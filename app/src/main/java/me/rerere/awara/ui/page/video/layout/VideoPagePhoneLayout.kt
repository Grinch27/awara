package me.rerere.awara.ui.page.video.layout

// TODO(user): Decide whether the phone detail stream should eventually insert chapter-like anchors for comments and related videos once more metadata blocks exist.
// TODO(agent): If tablet later also converges to one stream, extract this phone-only feed assembly into a shared detail body instead of duplicating section order twice.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.rerere.awara.R
import me.rerere.awara.ui.component.common.Spin
import me.rerere.awara.ui.component.ext.excludeBottom
import me.rerere.awara.ui.component.iwara.MediaCard
import me.rerere.awara.ui.component.iwara.rememberMediaListModePreference
import me.rerere.awara.ui.component.iwara.comment.EmbeddedCommentSection
import me.rerere.awara.ui.component.player.PlayerState
import me.rerere.awara.ui.page.video.VideoVM
import me.rerere.awara.ui.page.video.pager.VideoOverviewHeaderSection

private const val DETAIL_NAV_INDEX = 1
private const val DETAIL_COMMENTS_INDEX = 2
private const val DETAIL_RELATED_INDEX = 3

@Composable
fun VideoPagePhoneLayout(vm: VideoVM, state: PlayerState, player: @Composable () -> Unit) {
    var relatedExpanded by rememberSaveable(vm.id) { mutableStateOf(false) }
    val listMode by rememberMediaListModePreference()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    Spin(
        show = vm.state.loading,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .aspectRatio(16 / 9f),
                ) {
                    player()
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VideoOverviewHeaderSection(vm = vm)
                }
            }

            if (!vm.state.private) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                        PhoneDetailSectionNavigator(
                            commentCount = vm.state.video?.numComments ?: vm.state.commentState.stack.lastOrNull()?.total ?: 0,
                            relatedCount = vm.state.relatedVideos.size,
                            onJumpToComments = {
                                scope.launch {
                                    listState.animateScrollToItem(DETAIL_COMMENTS_INDEX)
                                }
                            },
                            onJumpToRelated = {
                                relatedExpanded = true
                                scope.launch {
                                    listState.animateScrollToItem(DETAIL_RELATED_INDEX)
                                }
                            },
                        )
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .navigationBarsPadding(),
                    ) {
                        EmbeddedCommentSection(
                            state = vm.state.commentState,
                            contentPadding = PaddingValues(0.dp),
                            onPageChange = vm::jumpCommentPage,
                            onBack = vm::popComment,
                            onPush = { vm.pushComment(it) },
                            onPostReply = { vm.postComment(it) },
                        )
                    }
                }

                if (vm.state.relatedVideos.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                            RelatedVideoSectionHeader(
                                relatedCount = vm.state.relatedVideos.size,
                                expanded = relatedExpanded,
                                onToggleExpanded = {
                                    relatedExpanded = !relatedExpanded
                                },
                            )
                        }
                    }

                    if (relatedExpanded) {
                        items(
                            items = vm.state.relatedVideos,
                            key = { it.id },
                        ) { relatedVideo ->
                            Box(modifier = Modifier.padding(horizontal = 8.dp)) {
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
}

@Composable
private fun PhoneDetailSectionNavigator(
    commentCount: Int,
    relatedCount: Int,
    onJumpToComments: () -> Unit,
    onJumpToRelated: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = onJumpToComments,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.video_jump_comments, commentCount))
            }

            FilledTonalButton(
                onClick = onJumpToRelated,
                enabled = relatedCount > 0,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.video_jump_related, relatedCount))
            }
        }
    }
}

@Composable
private fun RelatedVideoSectionHeader(
    relatedCount: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.video_related_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.video_related_count, relatedCount),
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            TextButton(onClick = onToggleExpanded) {
                Text(
                    text = stringResource(
                        if (expanded) {
                            R.string.video_related_collapse
                        } else {
                            R.string.video_related_expand
                        }
                    )
                )
            }
        }
    }
}
