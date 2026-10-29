// Requirements: shared media density, readable native search results.
// Verify: large fonts, blocked thumbnails and stable item keys.
package me.rerere.awara.ui.page.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.rerere.awara.feature.search.R
import me.rerere.awara.ui.component.common.UiState
import me.rerere.awara.ui.component.iwara.param.FilterValue
import me.rerere.awara.ui.component.iwara.param.rating.DEFAULT_MEDIA_RATING
import me.rerere.awara.ui.component.iwara.param.rating.MediaRatingKeys
import me.rerere.awara.ui.component.iwara.param.rating.SETTING_MEDIA_SEARCH_RATING
import me.rerere.awara.ui.component.iwara.param.sort.DEFAULT_MEDIA_SORT
import me.rerere.awara.ui.component.iwara.param.sort.MediaSortKeys
import me.rerere.compose_setting.preference.rememberStringPreference
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.CancellationException
import me.rerere.awara.ui.component.iwara.*
import me.rerere.awara.domain.media.MediaCardModel

@Composable
internal fun UiStateBox(
    modifier: Modifier = Modifier,
    state: UiState,
    onErrorRetry: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        when (state) {
            UiState.Initial -> Unit
            UiState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(R.string.search_recent_empty))
                }
            }

            UiState.Loading -> {
                content()
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            UiState.Success -> content()
            is UiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    state.message?.invoke()
                    if (state.message == null) {
                        Text(
                            text = state.messageText ?: stringResource(R.string.search_error_default),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    TextButton(onClick = onErrorRetry) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

@Composable
internal fun SearchFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
internal fun SearchResultCard(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (onClick == null) {
        Card { content() }
    } else {
        Card(onClick = onClick) { content() }
    }
}

@Composable
internal fun SearchPostCard(post: SearchPostItem) {
    SearchResultCard {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            SearchMetaText(authorName = post.authorName, dateLabel = post.createdAtLabel)
            post.body.takeIf { it.isNotBlank() }?.let { body ->
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(R.string.num_views, post.numViews),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun SearchPlaylistCard(
    playlist: SearchPlaylistItem,
    onClick: () -> Unit,
) {
    SearchResultCard(onClick = onClick) {
        Row(modifier = Modifier.fillMaxWidth()) {
            playlist.thumbnailUrl?.let { thumbnailUrl ->
                MediaThumbnail(id = "playlist:" + playlist.id,
                    url = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .width(132.dp)
                        .aspectRatio(16f / 9f),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                SearchMetaText(authorName = playlist.authorName, dateLabel = "")
                Text(
                    text = stringResource(R.string.playlist_num_videos, playlist.numVideos),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun SearchForumPostCard(
    post: SearchForumPostItem,
    onOpenForumThread: (String) -> Unit,
) {
    SearchResultCard(
        onClick = post.threadId.takeIf(String::isNotBlank)?.let { threadId ->
            { onOpenForumThread(threadId) }
        },
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = post.threadTitle.ifBlank { post.threadId.ifBlank { post.id } },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            SearchMetaText(authorName = post.authorName, dateLabel = post.createdAtLabel)
            post.body.takeIf { it.isNotBlank() }?.let { body ->
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(R.string.search_forum_reply, post.replyNum),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun SearchForumThreadCard(
    thread: SearchForumThreadItem,
    onClick: () -> Unit,
) {
    SearchResultCard(onClick = onClick) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (thread.sticky) {
                    RoleBadge(text = stringResource(R.string.forum_sticky))
                }
                if (thread.locked) {
                    RoleBadge(text = stringResource(R.string.forum_locked))
                }
                if (thread.section.isNotBlank()) {
                    RoleBadge(text = thread.section)
                }
            }
            Text(
                text = thread.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            SearchMetaText(authorName = thread.authorName, dateLabel = thread.updatedAtLabel)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.forum_posts_count, thread.numPosts),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.forum_views_count, thread.numViews),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun SearchMetaText(
    authorName: String,
    dateLabel: String,
) {
    val text = listOf(authorName, dateLabel)
        .filter(String::isNotBlank)
        .joinToString(separator = " / ")
    if (text.isNotBlank()) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun SearchMediaCard(media: SearchMediaItem, listMode: String, onClick: () -> Unit) {
    CompactMediaCard(
        media = MediaCardModel(media.id, media.title, media.authorName, media.createdAtLabel,
            media.thumbnailUrl, media.type == SearchMediaType.IMAGE, media.isPrivate),
        listMode = listMode,
        viewsLabel = stringResource(R.string.num_views, media.numViews),
        likesLabel = stringResource(R.string.num_likes, media.numLikes),
        privateLabel = stringResource(R.string.private_badge), onClick = onClick)
}

@Composable
internal fun SearchUserCard(
    user: SearchUserItem,
    onClick: () -> Unit,
) {
    Card(
        onClick = {
            if (user.hasNavigableProfile) {
                onClick()
            }
        },
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    text = user.displayHandle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                RoleBadge(text = user.role)
                if (user.premium) {
                    RoleBadge(text = stringResource(R.string.premium))
                }
            }
        }
    }
}

@Composable
internal fun RoleBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}