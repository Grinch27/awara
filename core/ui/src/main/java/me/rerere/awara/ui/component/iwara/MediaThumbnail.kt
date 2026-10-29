// Requirements: one cover policy across feeds/search/history/downloads.
// Verify: blocked means no ImageRequest/AsyncImage; blur remains a separate post-load option.
package me.rerere.awara.ui.component.iwara

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import me.rerere.compose_setting.preference.rememberBooleanPreference

@Composable
fun MediaThumbnail(id: String, url: String?, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val blocked by rememberBlockMediaThumbnailsPreference()
    val blurred by rememberBooleanPreference("setting.work_mode", false)
    val context = LocalContext.current
    val scope = LocalMediaCacheScope.current
    val request = remember(id, url, scope, blocked) {
        if (blocked || url.isNullOrBlank()) null else {
            val key = scope + ":" + id + ":" + url
            ImageRequest.Builder(context).data(url).memoryCacheKey(key).diskCacheKey(key).build()
        }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        if (request != null) AsyncImage(model = request, contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().then(if (blurred) Modifier.blur(8.dp) else Modifier))
    }
}
