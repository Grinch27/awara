package me.rerere.awara.ui.page.index.pager

// TODO(user): Decide whether the forum entry should stay as a browser-backed landing page or be replaced by a native list once the data path is ready.
// TODO(agent): Replace this page with a repo-backed forum feed when API -> repo -> VM is ready instead of growing more placeholder logic here.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.rerere.awara.R
import me.rerere.awara.util.openUrl

private const val IWARA_FORUM_URL = "https://www.iwara.tv/forum"

@Composable
fun IndexForumPage(
    onBrowseVideo: () -> Unit,
    onBrowseImage: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.index_nav_forum),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.forum_landing_message),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                FilledTonalButton(
                    onClick = {
                        context.openUrl(IWARA_FORUM_URL)
                    },
                ) {
                    Text(stringResource(R.string.forum_landing_open_action))
                }
                Text(
                    text = stringResource(R.string.forum_landing_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = onBrowseVideo) {
                    Text(stringResource(R.string.index_nav_video))
                }
                TextButton(onClick = onBrowseImage) {
                    Text(stringResource(R.string.index_nav_image))
                }
            }
        }
    }
}package me.rerere.awara.ui.page.index.pager

