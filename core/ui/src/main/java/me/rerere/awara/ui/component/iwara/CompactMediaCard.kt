// Requirements: Material 3 with compact information density; blocked covers issue no request.
// Open decisions: initial spacing requires device/font checks, not a claimed measured improvement.
// Verify: large fonts, narrow windows, stable cover bounds and accessible click targets.
package me.rerere.awara.ui.component.iwara

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.rerere.awara.domain.media.MediaCardModel

val LocalMediaCacheScope = staticCompositionLocalOf { "public" }

@Composable
fun CompactMediaCard(
    media: MediaCardModel,
    listMode: String,
    viewsLabel: String,
    likesLabel: String,
    privateLabel: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
            .combinedClickable(role = Role.Button, onClickLabel = media.title, onClick = onClick, onLongClick = onLongClick),
    ) {
        BoxWithConstraints {
            val coverWidth = (maxWidth * .36f).coerceIn(96.dp, 160.dp)
            @Composable
            fun Cover(coverModifier: Modifier) {
                Box(coverModifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
                    MediaThumbnail(id = (if (media.image) "image:" else "video:") + media.id,
                        url = media.thumbnail, modifier = Modifier.fillMaxSize())
                    if (media.isPrivate || selected) {
                        Text(if (selected) "✓" else privateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
            @Composable
            fun Metadata(detail: Boolean) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(media.title.trim(), style = MaterialTheme.typography.titleSmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(media.author, style = MaterialTheme.typography.bodySmall, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (media.createdAt.isNotBlank()) {
                        Text(media.createdAt, style = MaterialTheme.typography.labelSmall, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val stats = (if (detail) listOf(viewsLabel, likesLabel) else listOf(likesLabel)).filter { it.isNotBlank() }.joinToString(" · ")
                    if (stats.isNotBlank()) Text(stats,
                        style = MaterialTheme.typography.labelSmall, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (listMode == MEDIA_LIST_MODE_THUMBNAIL) {
                Column {
                    Cover(Modifier.fillMaxWidth().aspectRatio(if (media.image) 4f / 3f else 16f / 9f))
                    Box(Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) { Metadata(false) }
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Cover(Modifier.width(coverWidth).aspectRatio(4f / 3f))
                    Box(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 6.dp)) { Metadata(true) }
                }
            }
        }
    }
}
