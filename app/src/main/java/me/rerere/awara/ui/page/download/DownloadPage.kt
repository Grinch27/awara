package me.rerere.awara.ui.page.download

import android.content.pm.ActivityInfo
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import coil.compose.AsyncImage
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import me.rerere.awara.R
import me.rerere.awara.data.entity.DownloadItem
import me.rerere.awara.ui.component.common.BackButton
import me.rerere.awara.ui.component.ext.plus
import me.rerere.awara.ui.component.player.Player
import me.rerere.awara.ui.component.player.PlayerScaffold
import me.rerere.awara.ui.component.player.PlayerState
import me.rerere.awara.ui.component.player.rememberPlayerState
import me.rerere.awara.ui.hooks.rememberRequestedScreenOrientation
import me.rerere.awara.util.toLocalDateTimeString
import org.koin.androidx.compose.koinViewModel

@Composable
fun DownloadPage(vm: DownloadVM = koinViewModel()) {
    val items = vm.downloadedItems.collectAsLazyPagingItems()
    val appBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val state = rememberPlayerState {
        ExoPlayer.Builder(it).build()
    }
    var requestOrientation by rememberRequestedScreenOrientation()
    var fullscreen by remember { mutableStateOf(false) }
    fun enterFullScreen() {
        Log.i("DownloadPage", "enterFullScreen: ${state.videoSize.width}")
        if (state.videoSize.width > state.videoSize.height) {
            fullscreen = true
            requestOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            fullscreen = true
            requestOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    LaunchedEffect(state.videoSize) {
        if (state.videoSize != PlayerState.VideoSize.UNKNOWN) {
            enterFullScreen()
            state.play()
        }
    }

    fun exitFullScreen() {
        requestOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        fullscreen = false
        state.pause()
    }

    BackHandler(fullscreen) {
        exitFullScreen()
    }

    PlayerScaffold(
        fullscreen = fullscreen,
        player = {
            Player(
                state = state,
                modifier = Modifier.fillMaxSize(),
                navigationIcon = {
                    BackButton()
                },
                actions = {
                    if (fullscreen) {
                        IconButton(
                            onClick = {
                                state.resizeMode =
                                    if (state.resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT)
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    else AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        ) {
                            Icon(Icons.Outlined.FitScreen, null)
                        }
                    }
                },
                controllerTrail = {
                    IconButton(
                        onClick = {
                            if (!fullscreen) enterFullScreen() else exitFullScreen()
                        }
                    ) {
                        Icon(
                            if (!fullscreen) Icons.Outlined.Fullscreen else Icons.Outlined.FullscreenExit,
                            "Fullscreen"
                        )
                    }
                }
            )
        }
    ) { player ->
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.download)) },
                    navigationIcon = { BackButton() },
                    scrollBehavior = appBarScrollBehavior
                )
            }
        ) {
            LazyColumn(
                contentPadding = it + PaddingValues(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(appBarScrollBehavior.nestedScrollConnection),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items) { item ->
                    if (item != null) {
                        DownloadItem(
                            item = item,
                            onDelete = {
                                vm.delete(item)
                            }
                        ) {
                            state.player.setMediaItem(
                                MediaItem.Builder()
                                    .setUri(Uri.fromFile(java.io.File(item.path)))
                                    .build()
                            )
                            state.player.prepare()
                            fullscreen = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadItem(item: DownloadItem, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .height(64.dp)
                        .aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1
                    )

                    Text(
                        text = item.time.toLocalDateTimeString(),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)
            ) {
                IconButton(
                    onClick = onDelete
                ) {
                    Icon(Icons.Outlined.Delete, null)
                }
            }
        }
    }
}