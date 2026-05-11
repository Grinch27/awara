package me.rerere.awara.ui.component.iwara.comment

// TODO(user): Decide whether the embedded comment section on phone detail should keep inline pagination long term or switch to auto-load more after API behavior is stable.
// TODO(agent): If comments later gain moderation, sorting, or collapse rules, split the shared footer/header helpers out of this file before adding another branch here.

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import me.rerere.awara.R
import me.rerere.awara.data.entity.Comment
import me.rerere.awara.data.entity.CommentCreationDto
import me.rerere.awara.ui.component.common.Spin
import me.rerere.awara.ui.component.iwara.PaginationBar

@Composable
fun CommentList(
    modifier: Modifier = Modifier,
    state: CommentState,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onPageChange: (Int) -> Unit,
    onBack: () -> Unit,
    onPush: (String) -> Unit,
    onPostReply: (CommentCreationDto) -> Unit,
) {
    val currentComment = state.stack.last()
    var replying by remember { mutableStateOf(false) }
    var replyTo by remember { mutableStateOf<Comment?>(null) }

    BackHandler(state.stack.size > 1) {
        onBack()
        replyTo = null
    }

    Column(modifier = modifier) {
        CommentSectionHeader(
            showAlways = false,
            state = state,
            currentComment = currentComment,
            onBack = {
                onBack()
                replyTo = null
            },
        )

        Spin(
            show = state.loading,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(10.dp),
            ) {
                items(currentComment.comments) { comment ->
                    CommentCard(
                        comment = comment,
                        nestingLevel = if (state.stack.size > 1) 1 else 0,
                        showParentContext = state.stack.size > 1,
                        onLoadReplies = {
                            onPush(it.id)
                            replyTo = it
                        },
                        onReply = {
                            replying = true
                            replyTo = it
                        },
                    )
                }
            }
        }

        CommentReplyFooter(
            replying = replying,
            replyTo = replyTo,
            currentComment = currentComment,
            contentPadding = contentPadding,
            onReplyingChange = { replying = it },
            onPageChange = onPageChange,
            onPostReply = onPostReply,
        )
    }
}

@Composable
fun EmbeddedCommentSection(
    modifier: Modifier = Modifier,
    state: CommentState,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onPageChange: (Int) -> Unit,
    onBack: () -> Unit,
    onPush: (String) -> Unit,
    onPostReply: (CommentCreationDto) -> Unit,
) {
    val currentComment = state.stack.last()
    var replying by remember { mutableStateOf(false) }
    var replyTo by remember { mutableStateOf<Comment?>(null) }

    BackHandler(state.stack.size > 1) {
        onBack()
        replyTo = null
    }

    Column(modifier = modifier) {
        CommentSectionHeader(
            showAlways = true,
            state = state,
            currentComment = currentComment,
            onBack = {
                onBack()
                replyTo = null
            },
        )

        Spin(
            show = state.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (currentComment.comments.isEmpty()) {
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.comment_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                        )
                    }
                } else {
                    currentComment.comments.forEach { comment ->
                        CommentCard(
                            comment = comment,
                            nestingLevel = if (state.stack.size > 1) 1 else 0,
                            showParentContext = state.stack.size > 1,
                            onLoadReplies = {
                                onPush(it.id)
                                replyTo = it
                            },
                            onReply = {
                                replying = true
                                replyTo = it
                            },
                        )
                    }
                }
            }
        }

        CommentReplyFooter(
            replying = replying,
            replyTo = replyTo,
            currentComment = currentComment,
            contentPadding = contentPadding,
            onReplyingChange = { replying = it },
            onPageChange = onPageChange,
            onPostReply = onPostReply,
        )
    }
}

@Composable
private fun CommentSectionHeader(
    showAlways: Boolean,
    state: CommentState,
    currentComment: CommentStateItem,
    onBack: () -> Unit,
) {
    AnimatedVisibility(visible = showAlways || state.stack.size > 1) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                if (state.stack.size > 1) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, null)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (state.stack.size > 1) {
                                R.string.comment_reply_thread_title
                            } else {
                                R.string.comment_section_title
                            }
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (showAlways) {
                        Text(
                            text = stringResource(R.string.comment_meta_count, currentComment.total),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentReplyFooter(
    replying: Boolean,
    replyTo: Comment?,
    currentComment: CommentStateItem,
    contentPadding: PaddingValues,
    onReplyingChange: (Boolean) -> Unit,
    onPageChange: (Int) -> Unit,
    onPostReply: (CommentCreationDto) -> Unit,
) {
    AnimatedContent(
        targetState = replying,
        label = "ReplyBar",
        transitionSpec = {
            if (targetState) {
                slideInHorizontally(initialOffsetX = { -it }) with slideOutHorizontally(targetOffsetX = { -it })
            } else {
                slideInHorizontally(initialOffsetX = { it }) with slideOutHorizontally(targetOffsetX = { it })
            }
        },
    ) { replyingActive ->
        if (replyingActive) {
            Surface(
                modifier = Modifier
                    .imePadding()
                    .fillMaxWidth(),
                tonalElevation = 4.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(contentPadding)
                        .padding(6.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = { onReplyingChange(false) }) {
                        Icon(Icons.Outlined.Close, null)
                    }

                    var body by remember { mutableStateOf("") }
                    TextField(
                        value = body,
                        onValueChange = { body = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = if (replyTo == null) {
                                    stringResource(R.string.comment_reply_action)
                                } else {
                                    stringResource(R.string.comment_reply_placeholder, replyTo.user?.name ?: "")
                                }
                            )
                        },
                        colors = TextFieldDefaults.textFieldColors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        keyboardOptions = KeyboardOptions(
                            autoCorrect = false,
                            imeAction = ImeAction.Send,
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                onPostReply(
                                    CommentCreationDto(
                                        body = body,
                                        parentId = replyTo?.id,
                                    ),
                                )
                                body = ""
                            },
                        ),
                    )

                    FilledTonalButton(
                        onClick = {
                            onPostReply(
                                CommentCreationDto(
                                    body = body,
                                    parentId = replyTo?.id,
                                ),
                            )
                            body = ""
                        },
                    ) {
                        Icon(Icons.Outlined.Send, null)
                    }
                }
            }
        } else {
            PaginationBar(
                page = currentComment.page,
                limit = currentComment.limit,
                total = currentComment.total,
                onPageChange = onPageChange,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = contentPadding,
                trailing = {
                    FilledTonalButton(
                        onClick = { onReplyingChange(true) },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ModeComment,
                            contentDescription = null,
                        )
                    }
                },
            )
        }
    }
}