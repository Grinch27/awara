package me.rerere.awara.ui.component.iwara.comment

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.rerere.awara.data.entity.Comment
import me.rerere.awara.ui.LocalRouterProvider
import me.rerere.awara.ui.component.iwara.Avatar
import me.rerere.awara.ui.component.iwara.RichText
import me.rerere.awara.ui.component.iwara.UserStatus
import me.rerere.awara.ui.stores.LocalUserStore
import me.rerere.awara.ui.stores.collectAsState
import me.rerere.awara.util.openUrl
import me.rerere.awara.util.toLocalDateTimeString

@Composable
fun CommentCard(
    modifier: Modifier = Modifier,
    comment: Comment,
    onLoadReplies: (Comment) -> Unit,
    onReply: (Comment) -> Unit
) {
    val context = LocalContext.current
    val user = LocalUserStore.current.collectAsState()
    val router = LocalRouterProvider.current
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        tonalElevation = 1.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(horizontal = 10.dp, vertical = 9.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
                        text = comment.user?.displayName ?: "",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.clickable {
                            comment.user
                                ?.takeIf { it.hasNavigableProfile }
                                ?.let { router.navigate("user/${it.username}") }
                        }
                    )
                    Text(
                        text = comment.user?.displayHandle ?: "",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                comment.user?.let {
                    UserStatus(user = it)
                }

                // "Me" Tag
                if(comment.user?.id == user.user?.id) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = "我",
                            style = MaterialTheme.typography.labelMedium,
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
                style = MaterialTheme.typography.bodyMedium,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                Text(
                    text = comment.createdAt.toLocalDateTimeString(),
                    style = MaterialTheme.typography.labelMedium
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
                    ) {
                        Text(
                            text = "共${comment.numReplies}条回复",
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
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "回复",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}