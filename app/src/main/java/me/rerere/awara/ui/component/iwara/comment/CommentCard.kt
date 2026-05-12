package me.rerere.awara.ui.component.iwara.comment

// TODO(user): Decide whether nested reply cards should eventually collapse long branches or keep always-expanded excerpts.
// TODO(agent): Keep reply depth visible with the left connector and parent excerpt, but avoid overcomplicating the card chrome.

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.rerere.awara.R
import me.rerere.awara.data.entity.Comment
import me.rerere.awara.ui.LocalRouterProvider
import me.rerere.awara.ui.component.iwara.Avatar
import me.rerere.awara.ui.component.iwara.RichText
import me.rerere.awara.ui.component.iwara.UserStatus
import me.rerere.awara.ui.stores.LocalUserStore
import me.rerere.awara.ui.stores.collectAsState
import me.rerere.awara.util.openUrl
import me.rerere.awara.util.toLocalDateTimeString

internal val CommentPanelSurface = Color(0xFF151A21)
internal val CommentCardSurface = Color(0xFF1B222C)
internal val CommentNestedCardSurface = Color(0xFF212B36)
internal val CommentReplyContextSurface = Color(0xFF243140)
internal val CommentCardBorder = Color(0xFF314051)
internal val CommentBodyColor = Color(0xFFF2F5F8)
internal val CommentMetaColor = Color(0xFFB7C1CE)

@Composable
fun CommentCard(
    modifier: Modifier = Modifier,
    comment: Comment,
    nestingLevel: Int = 0,
    showParentContext: Boolean = false,
    onLoadReplies: (Comment) -> Unit,
    onReply: (Comment) -> Unit
) {
    val context = LocalContext.current
    val user = LocalUserStore.current.collectAsState()
    val router = LocalRouterProvider.current
    val shouldShowThreadRail = nestingLevel > 0 || showParentContext
    Surface(
        modifier = modifier.padding(start = (nestingLevel * 14).dp),
        color = if (nestingLevel > 0) CommentNestedCardSurface else CommentCardSurface,
        contentColor = CommentBodyColor,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = if (nestingLevel > 0) 1.2.dp else 1.dp,
            color = if (nestingLevel > 0) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            } else {
                CommentCardBorder
            },
        ),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .animateContentSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (shouldShowThreadRail) {
                CommentThreadRail(
                    modifier = Modifier.padding(top = 2.dp),
                    accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.58f),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (showParentContext && comment.parent != null) {
                    val parentLabel = comment.parent.user?.displayName
                        ?: stringResource(R.string.comment_reply_thread_title)
                    Surface(
                        color = CommentReplyContextSurface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.comment_reply_to, parentLabel),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CommentMetaColor,
                                )
                                Text(
                                    text = comment.parent.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    color = CommentBodyColor,
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Avatar(
                        user = comment.user,
                        modifier = Modifier.size(30.dp),
                        onClick = {
                            comment.user
                                ?.takeIf { it.hasNavigableProfile }
                                ?.let { router.navigate("user/${it.username}") }
                        }
                    )
                    Column {
                        Text(
                            text = comment.user?.displayName
                                ?: stringResource(R.string.comment_reply_thread_title),
                            color = CommentBodyColor,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.clickable {
                                comment.user
                                    ?.takeIf { it.hasNavigableProfile }
                                    ?.let { router.navigate("user/${it.username}") }
                            }
                        )
                        if (!comment.user?.displayHandle.isNullOrBlank()) {
                            Text(
                                text = comment.user?.displayHandle.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = CommentMetaColor,
                            )
                        }
                    }

                    comment.user?.let {
                        UserStatus(user = it)
                    }

                    // "Me" Tag
                    if(comment.user?.id == user.user?.id) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
                        ) {
                            Text(
                                text = stringResource(R.string.comment_me_badge),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(vertical = 3.dp, horizontal = 7.dp)
                            )
                        }
                    }
                }

                RichText(
                    text = comment.body,
                    onLinkClick = {
                        context.openUrl(it)
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(color = CommentBodyColor),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    Text(
                        text = comment.createdAt.toLocalDateTimeString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = CommentMetaColor,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    AnimatedVisibility(
                        visible = comment.numReplies > 0
                    ) {
                        TextButton(
                            onClick = {
                                onLoadReplies(comment)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.comment_replies_count, comment.numReplies),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Icon(Icons.Outlined.KeyboardArrowDown, null)
                        }
                    }

                    if(comment.parent == null) {
                        TextButton(
                            onClick = {
                                onReply(comment)
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.comment_reply_action),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentThreadRail(
    modifier: Modifier = Modifier,
    accentColor: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier = modifier
            .width(12.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .width(2.dp)
                .fillMaxHeight()
                .background(accentColor, RoundedCornerShape(999.dp))
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(accentColor, CircleShape)
        )
    }
}