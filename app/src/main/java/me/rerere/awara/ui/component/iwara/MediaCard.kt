package me.rerere.awara.ui.component.iwara

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import me.rerere.awara.R
import me.rerere.awara.data.entity.Image as GalleryImage
import me.rerere.awara.data.entity.Media
import me.rerere.awara.data.entity.Video
import me.rerere.awara.data.entity.thumbnailUrl
import me.rerere.awara.ui.LocalRouterProvider
import me.rerere.awara.ui.component.common.SkeletonBox
import me.rerere.awara.util.toLocalDateTimeString
import me.rerere.compose_setting.preference.rememberBooleanPreference

@Composable
fun MediaCard(
    modifier: Modifier = Modifier,
    listMode: String = MEDIA_LIST_MODE_DETAIL,
    media: Media,
) {
    val router = LocalRouterProvider.current
    val workMode by rememberBooleanPreference(
        key = "setting.work_mode",
        default = false,
    )
    val blockMediaThumbnails by rememberBlockMediaThumbnailsPreference()
    val painter = rememberAsyncImagePainter(
        model = if (blockMediaThumbnails) null else media.thumbnailUrl(),
    )

    Card(
        modifier = modifier,
        onClick = {
            when (media) {
                is Video -> router.navigate("video/${media.id}")
                is GalleryImage -> router.navigate("image/${media.id}")
            }
        },
    ) {
        when (listMode) {
            MEDIA_LIST_MODE_THUMBNAIL -> ThumbnailMediaCardBody(
                media = media,
                painter = painter,
                workMode = workMode,
                blockMediaThumbnails = blockMediaThumbnails,
            )

            else -> DetailMediaCardBody(
                media = media,
                painter = painter,
                workMode = workMode,
                blockMediaThumbnails = blockMediaThumbnails,
            )
        }
    }
}

@Composable
private fun ThumbnailMediaCardBody(
    media: Media,
    painter: AsyncImagePainter,
    workMode: Boolean,
    blockMediaThumbnails: Boolean,
) {
    Column {
        MediaCover(
            media = media,
            painter = painter,
            workMode = workMode,
            blockMediaThumbnails = blockMediaThumbnails,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(220f / 160f),
        )

        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Text(
                text = media.title.trim(),
                maxLines = 2,
                style = MaterialTheme.typography.labelLarge,
            )

            Row {
                Text(
                    text = media.user.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalContentColor.current.copy(alpha = 0.75f),
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = LocalContentColor.current.copy(alpha = 0.75f),
                    modifier = Modifier.height(16.dp),
                )
                Text(
                    text = media.numLikes.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalContentColor.current.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun DetailMediaCardBody(
    media: Media,
    painter: AsyncImagePainter,
    workMode: Boolean,
    blockMediaThumbnails: Boolean,
) {
    val description = media.body?.trim().orEmpty()

    Row(modifier = Modifier.fillMaxWidth()) {
        MediaCover(
            media = media,
            painter = painter,
            workMode = workMode,
            blockMediaThumbnails = blockMediaThumbnails,
            modifier = Modifier
                .width(164.dp)
                .aspectRatio(220f / 160f),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = media.title.trim(),
                maxLines = 3,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = media.user.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = LocalContentColor.current.copy(alpha = 0.82f),
                maxLines = 1,
            )
            Text(
                text = media.createdAt.toLocalDateTimeString(),
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.68f),
                maxLines = 1,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.num_views, media.numViews),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = 0.78f),
                )
                Text(
                    text = stringResource(R.string.num_likes, media.numLikes),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = 0.78f),
                )
            }
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    maxLines = 3,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.78f),
                )
            }
        }
    }
}

@Composable
private fun MediaCover(
    media: Media,
    painter: AsyncImagePainter,
    workMode: Boolean,
    blockMediaThumbnails: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (blockMediaThumbnails) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        } else {
            SkeletonBox(
                show = painter.state is AsyncImagePainter.State.Loading,
                modifier = Modifier.fillMaxSize(),
            ) {
                Image(
                    painter = painter,
                    contentDescription = "Media Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (workMode) Modifier.blur(8.dp) else Modifier),
                )
            }
        }

        if (media is Video && media.private) {
            Badge(
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopEnd),
            ) {
                Text(
                    text = stringResource(R.string.private_badge),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}