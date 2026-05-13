package me.rerere.awara.ui.page.forum

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import me.rerere.awara.R
import me.rerere.awara.data.entity.ForumPost
import me.rerere.awara.data.entity.ForumSection
import me.rerere.awara.data.entity.ForumThread
import me.rerere.awara.data.entity.ForumUser
import me.rerere.awara.data.entity.toAvatarUrl
import me.rerere.awara.ui.component.iwara.RichText
import me.rerere.awara.util.openUrl
import me.rerere.awara.util.toLocalDateTimeString
import java.time.Instant

@Composable
fun ForumSectionCard(
    section: ForumSection,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ForumCardSurface(
        modifier = modifier,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ForumBadge(text = section.group.ifBlank { stringResource(R.string.index_nav_forum) })
                    if (section.locked) {
                        ForumBadge(text = stringResource(R.string.forum_locked))
                    }
                }
                Text(
                    text = section.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ForumStatText(stringResource(R.string.forum_threads_count, section.numThreads))
                    ForumStatText(stringResource(R.string.forum_posts_count, section.numPosts))
                }
                section.lastThread?.let { thread ->
                    Text(
                        text = stringResource(R.string.forum_last_thread, thread.title.ifBlank { thread.id }),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    thread.lastPost?.updatedAt?.let { updatedAt ->
                        ForumStatText(stringResource(R.string.forum_last_post_at, updatedAt.toLocalDateTimeString()))
                    }
                }
            }
        }
    }
}

@Composable
fun ForumThreadCard(
    thread: ForumThread,
    modifier: Modifier = Modifier,
    onOpenUser: (ForumUser) -> Unit,
    onClick: () -> Unit,
) {
    ForumCardSurface(
        modifier = modifier,
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (thread.sticky) {
                    ForumBadge(text = stringResource(R.string.forum_sticky))
                }
                if (thread.locked) {
                    ForumBadge(text = stringResource(R.string.forum_locked))
                }
                if (thread.section.isNotBlank()) {
                    ForumBadge(text = thread.section)
                }
            }
            Text(
                text = thread.title.ifBlank { thread.id },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            ForumUserMeta(
                user = thread.user,
                time = thread.updatedAt ?: thread.createdAt,
                onOpenUser = onOpenUser,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ForumStatText(stringResource(R.string.forum_posts_count, thread.numPosts))
                ForumStatText(stringResource(R.string.forum_views_count, thread.numViews))
            }
            thread.lastPost?.updatedAt?.let { updatedAt ->
                ForumStatText(stringResource(R.string.forum_last_post_at, updatedAt.toLocalDateTimeString()))
            }
        }
    }
}

@Composable
fun ForumThreadHeader(
    thread: ForumThread,
    totalPosts: Int,
    pendingCount: Int,
    onOpenUser: (ForumUser) -> Unit,
) {
    ForumCardSurface {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (thread.sticky) {
                    ForumBadge(text = stringResource(R.string.forum_sticky))
                }
                if (thread.locked) {
                    ForumBadge(text = stringResource(R.string.forum_locked))
                }
                if (thread.section.isNotBlank()) {
                    ForumBadge(text = thread.section)
                }
            }
            Text(
                text = thread.title.ifBlank { thread.id },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            ForumUserMeta(
                user = thread.user,
                time = thread.createdAt,
                onOpenUser = onOpenUser,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ForumStatText(stringResource(R.string.forum_posts_count, totalPosts))
                ForumStatText(stringResource(R.string.forum_views_count, thread.numViews))
                if (pendingCount > 0) {
                    ForumStatText(stringResource(R.string.forum_pending_count, pendingCount))
                }
            }
        }
    }
}

@Composable
fun ForumPostCard(
    post: ForumPost,
    index: Int,
    modifier: Modifier = Modifier,
    onOpenUser: (ForumUser) -> Unit,
) {
    val context = LocalContext.current
    ForumCardSurface(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.forum_floor, index + 1),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                post.updatedAt?.let { updatedAt ->
                    Text(
                        text = updatedAt.toLocalDateTimeString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            ForumUserMeta(
                user = post.user,
                time = post.createdAt,
                onOpenUser = onOpenUser,
            )
            if (post.body.isBlank()) {
                Text(
                    text = stringResource(R.string.forum_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                RichText(
                    text = post.body,
                    style = MaterialTheme.typography.bodyMedium,
                    onLinkClick = { context.openUrl(it) },
                )
            }
        }
    }
}

@Composable
fun ForumUserMeta(
    user: ForumUser?,
    time: Instant?,
    modifier: Modifier = Modifier,
    onOpenUser: (ForumUser) -> Unit,
) {
    val canOpenUser = user?.hasNavigableProfile == true
    Row(
        modifier = modifier.then(
            if (canOpenUser && user != null) {
                Modifier.clickable { onOpenUser(user) }
            } else {
                Modifier
            }
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = user?.avatar.toAvatarUrl(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user?.displayName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.user),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user?.displayHandle?.takeIf { it.isNotBlank() } ?: stringResource(R.string.forum_unknown_user),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                time?.let {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = it.toLocalDateTimeString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ForumCardSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
        shape = shape,
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}

@Composable
private fun ForumBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ForumStatText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
