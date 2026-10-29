package me.rerere.awara.ui.page.video

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player as MediaPlayer
import androidx.media3.datasource.HttpDataSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.rerere.awara.data.session.SecureSession
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.rerere.awara.data.entity.VideoFile
import me.rerere.awara.data.entity.fixUrl
import me.rerere.awara.data.entity.playbackUrl
import me.rerere.awara.data.entity.thumbnailUrl
import me.rerere.awara.data.source.stringResource
import me.rerere.awara.ui.LocalMessageProvider
import me.rerere.awara.ui.component.common.BackButton
import me.rerere.awara.ui.component.player.DlnaSelector
import me.rerere.awara.ui.component.player.Player
import me.rerere.awara.ui.component.player.PlayerScaffold
import me.rerere.awara.ui.component.player.PlayerState
import me.rerere.awara.ui.component.player.rememberDlnaCastState
import me.rerere.awara.ui.component.player.rememberPlayerState
import me.rerere.awara.ui.hooks.ForceSystemBarColor
import me.rerere.awara.ui.hooks.rememberRequestedScreenOrientation
import me.rerere.awara.ui.hooks.rememberWindowSizeClass
import me.rerere.awara.ui.page.video.layout.VideoPagePhoneLayout
import me.rerere.awara.ui.page.video.layout.VideoPageTabletLayout
import me.rerere.awara.ui.theme.DynamicColorTheme
import me.rerere.awara.ui.theme.rememberDynamicColorSchemeState
import me.rerere.compose_setting.preference.mmkvPreference
import okhttp3.OkHttpClient
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

@Composable
fun VideoPage(
    vm: VideoVM = koinViewModel(),
    publicHttpClient: OkHttpClient = koinInject(),
) {
    ForceSystemBarColor(appearanceLight = false)

    var requestOrientation by rememberRequestedScreenOrientation()
    val windowSize = rememberWindowSizeClass()
    val session by SecureSession.state.collectAsState()
    val cacheScope = session.sessionId.ifBlank { "public" }
    val state = rememberPlayerState(publicHttpClient, cacheScope)
    var restored by remember(vm.id) { mutableStateOf(false) }
    DisposableEffect(state, vm) {
        val listener = object : MediaPlayer.Listener {
            override fun onPlayerError(error: PlaybackException) {
                var cause: Throwable? = error
                while (cause != null) {
                    if (cause is HttpDataSource.InvalidResponseCodeException && cause.responseCode in setOf(401, 403)) {
                        vm.refreshSources(); break
                    }
                    cause = cause.cause
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) vm.saveProgress(state.player.currentPosition, state.player.duration)
            }
        }
        state.player.addListener(listener)
        onDispose {
            vm.saveProgress(state.player.currentPosition, state.player.duration)
            state.player.removeListener(listener)
        }
    }
    LaunchedEffect(state, vm.id) {
        while (isActive) {
            delay(5000)
            vm.saveProgress(state.player.currentPosition, state.player.duration)
        }
    }
    val message = LocalMessageProvider.current
    val scope = rememberCoroutineScope()

    // M3
    val colorState = rememberDynamicColorSchemeState()
    LaunchedEffect(vm.state.video) {
        colorState.updateColorScheme(url = vm.state.video?.thumbnailUrl() ?: "")
    }

    // Dlna
    val dlnaCastState = rememberDlnaCastState()
    var dlnaCastSheetShow by remember {
        mutableStateOf(false)
    }
    DlnaSelector(
        show = dlnaCastSheetShow,
        onDismiss = { dlnaCastSheetShow = false },
        state = dlnaCastState,
        onDeviceSelected = { device ->
            state.pause()
            state.currentMediaItem?.let { mediaItem ->
                scope.launch {
                    kotlin.runCatching {
                        dlnaCastState.playUrl(
                            device = device,
                            url = mediaItem.localConfiguration?.uri.toString()
                        )
                        message.success {
                            Text("已发送到 ${device.friendlyName}")
                        }
                    }.onFailure {
                        message.error {
                            Text("发送失败: ${it.message ?: it.javaClass.simpleName}")
                        }
                    }
                }
            }
        }
    )

    // Full Screen
    var fullscreen by remember { mutableStateOf(false) }
    fun enterFullScreen() {
        if(state.videoSize == PlayerState.VideoSize.UNKNOWN) return
        if(state.videoSize.width > state.videoSize.height) {
            fullscreen = true
            requestOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            fullscreen = true
            requestOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
    fun exitFullScreen() {
        requestOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        fullscreen = false
    }
    BackHandler(fullscreen) {
        exitFullScreen()
    }
    suspend fun applyUrls(urls: List<VideoFile>) {
        val assetVersion = vm.state.video?.file?.id.orEmpty() + ":" + vm.state.video?.updatedAt.toString()
        val mediaId = "video:" + vm.id + ":" + assetVersion
        state.updatePlayerItems(
            urls.map {
                PlayerState.PlayerItem(
                    quality = it.name,
                    mediaItem = MediaItem.Builder()
                        .setUri(it.playbackUrl().fixUrl())
                        .setMediaId(mediaId)
                        .setCustomCacheKey(if (it.type.contains("mpegurl", true) || it.type.contains("dash", true) ||
                            it.playbackUrl().substringBefore('?').endsWith(".m3u8", true) ||
                            it.playbackUrl().substringBefore('?').endsWith(".mpd", true)) null else cacheScope + ":" + mediaId + ":" + it.name)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(vm.state.video?.title)
                                .build()
                        )
                        .build()
                )
            }
        )
        if (urls.isNotEmpty() && !restored) {
            val saved = vm.savedProgress()
            if (saved?.fileId == vm.state.video?.file?.id && saved != null && saved.duration > 0 && saved.position < saved.duration - 5000) {
                state.player.seekTo(saved.position.coerceAtLeast(0))
            }
            restored = true
        }
        if(mmkvPreference.getBoolean("setting.auto_play", true)) {
            state.prepare()
        }
    }

    // Handle VM Events
    LaunchedEffect(state) {
        applyUrls(vm.state.urls)
        vm.events.collectLatest {
            when (it) {
                // URL加载完成，更新播放器
                is VideoVM.VideoEvent.UrlLoaded -> {
                    applyUrls(it.urls)
                }

                is VideoVM.VideoEvent.CommentPostException -> {
                    message.error {
                        Text("评论失败: ${it.throwable.message ?: it.throwable.javaClass.simpleName}")
                    }
                }
                is VideoVM.VideoEvent.CommentPostFailed -> {
                    message.error {
                        Text("评论失败: " + stringResource(it.error))
                    }
                }
                VideoVM.VideoEvent.CommentPosted -> {
                    message.success {
                        Text("评论成功")
                    }
                }
            }
        }
    }

    // UI
    DynamicColorTheme(state = colorState) {
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
                        if (!fullscreen) {
                            IconButton(onClick = { dlnaCastSheetShow = !dlnaCastSheetShow }) {
                                Icon(Icons.Outlined.Cast, null)
                            }
                        }
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

                        Box {
                            var expandedVideoQuality by remember {
                                mutableStateOf(false)
                            }
                            TextButton(
                                onClick = {
                                    expandedVideoQuality = true
                                }
                            ) {
                                Text(
                                    text = state.currentQuality,
                                    color = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = expandedVideoQuality,
                                onDismissRequest = { expandedVideoQuality = false }
                            ) {
                                vm.state.urls.forEach { url ->
                                    DropdownMenuItem(
                                        onClick = {
                                            expandedVideoQuality = false
                                            state.updateCurrentQuality(url.name)
                                        },
                                        text = {
                                            Text(url.name)
                                        }
                                    )
                                }
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
            when (windowSize.widthSizeClass) {
                WindowWidthSizeClass.Compact -> VideoPagePhoneLayout(vm, state, player)
                else -> VideoPageTabletLayout(vm, state, player)
            }
        }
    }
}