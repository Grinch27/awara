// Requirements: feed uses the same compact Material 3 card and thumbnail policy as search.
// Verify: existing video/image routes and current list-mode preferences remain intact.
package me.rerere.awara.ui.component.iwara

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.rerere.awara.R
import me.rerere.awara.data.entity.Media
import me.rerere.awara.data.entity.Video
import me.rerere.awara.data.entity.thumbnailUrl
import me.rerere.awara.domain.media.MediaCardModel
import me.rerere.awara.ui.LocalRouterProvider
import me.rerere.awara.util.toLocalDateTimeString
import me.rerere.awara.data.entity.Image as GalleryImage

@Composable
fun MediaCard(
    modifier: Modifier = Modifier,
    listMode: String = MEDIA_LIST_MODE_DETAIL,
    media: Media,
) {
    val router = LocalRouterProvider.current
    CompactMediaCard(
        media = MediaCardModel(
            id = media.id, title = media.title, author = media.user.displayName,
            createdAt = media.createdAt.toLocalDateTimeString(), thumbnail = media.thumbnailUrl(),
            image = media is GalleryImage, isPrivate = media is Video && media.private,
        ),
        listMode = listMode,
        viewsLabel = stringResource(R.string.num_views, media.numViews),
        likesLabel = stringResource(R.string.num_likes, media.numLikes),
        privateLabel = stringResource(R.string.private_badge),
        modifier = modifier,
        onClick = {
            when (media) {
                is Video -> router.navigate("video/" + Uri.encode(media.id))
                is GalleryImage -> router.navigate("image/" + Uri.encode(media.id))
            }
        },
    )
}
