package me.rerere.awara.ui.component.player

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.common.VideoSize
import me.rerere.compose_setting.preference.mmkvPreference
import okhttp3.OkHttpClient

private const val TAG = "PlayerState"

@Composable
fun rememberPlayerState(
    publicHttpClient: OkHttpClient,
    cacheScope: String = "public",
    builder: (Context, OkHttpClient) -> ExoPlayer = { context, httpClient ->
        ExoPlayer.Builder(context, DefaultMediaSourceFactory(PlayerCache.get(context, httpClient, cacheScope)))
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                setAudioAttributes(androidx.media3.common.AudioAttributes.Builder().setUsage(androidx.media3.common.C.USAGE_MEDIA).setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
                playWhenReady = mmkvPreference.getBoolean("setting.auto_play", true)
                repeatMode = if(mmkvPreference.getBoolean("setting.loop_play", false)) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            }
    }
): PlayerState {
    val context = LocalContext.current
    val state by remember(context, publicHttpClient, cacheScope) {
        mutableStateOf(PlayerState(builder(context, publicHttpClient)))
    }
    DisposableEffect(context, state) {
        val playbackListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                state.state = when (playbackState) {
                    Player.STATE_IDLE -> PlayerState.State.IDLE
                    Player.STATE_BUFFERING -> PlayerState.State.BUFFERING
                    Player.STATE_READY -> PlayerState.State.READY
                    Player.STATE_ENDED -> PlayerState.State.ENDED
                    else -> PlayerState.State.IDLE.also {
                        Log.w(TAG, "onPlaybackStateChanged: unknown state $playbackState")
                    }
                }
                if (playbackState == Player.STATE_READY) {
                    state.duration = state.player.duration
                    state.currentMediaItem = state.player.currentMediaItem
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                state.playing = isPlaying
                state.duration = state.player.duration
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                super.onIsLoadingChanged(isLoading)
                state.loading = isLoading
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                super.onVideoSizeChanged(videoSize)
                state.videoSize = PlayerState.VideoSize.from(videoSize)
                state.duration = state.player.duration
            }
        }
        state.player.addListener(playbackListener)
        state.playing = state.player.isPlaying
        state.state = when (state.player.playbackState) {
            Player.STATE_IDLE -> PlayerState.State.IDLE
            Player.STATE_BUFFERING -> PlayerState.State.BUFFERING
            Player.STATE_READY -> PlayerState.State.READY
            Player.STATE_ENDED -> PlayerState.State.ENDED
            else -> PlayerState.State.IDLE.also {
                Log.w(TAG, "onPlaybackStateChanged: unknown state ${state.player.playbackState}")
            }
        }
        state.videoSize = PlayerState.VideoSize.from(state.player.videoSize)
        state.duration = state.player.duration
        Log.i(TAG, "rememberPlayerState: Init")
        onDispose {
            state.player.run {
                removeListener(playbackListener)
                release()
            }
            Log.i(TAG, "rememberPlayerState: Dispose")
        }
    }
    return state
}

@Stable
@SuppressLint("UnsafeOptInUsageError")
class PlayerState(val player: Player) {
    var playing by mutableStateOf(false)
    var state by mutableStateOf(State.IDLE)
    var loading by mutableStateOf(false)


    var videoSize by mutableStateOf(VideoSize.UNKNOWN)
    var duration by mutableStateOf(0L)
    var resizeMode by mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT)

    var playerItems by mutableStateOf<List<PlayerItem>>(emptyList())
        private set
    var currentQuality by mutableStateOf(
        mmkvPreference.getString(
            "setting.player_quality",
            "Source"
        )!!
    )
    var currentMediaItem by mutableStateOf<MediaItem?>(null)

    private fun preferredQuality(): String = mmkvPreference.getString(
        "setting.player_quality",
        "Source"
    )!!

    private fun selectPlayerItem(item: PlayerItem, persistPreference: Boolean) {
        currentQuality = item.quality
        if (persistPreference) {
            mmkvPreference.putString("setting.player_quality", item.quality)
        }
        val sameContent = player.currentMediaItem?.mediaId == item.mediaItem.mediaId && item.mediaItem.mediaId.isNotBlank()
        val position = if (sameContent) player.currentPosition.coerceAtLeast(0L) else 0L
        val playIntent = player.playWhenReady
        val speed = player.playbackParameters
        player.setMediaItem(item.mediaItem, position)
        player.playbackParameters = speed
        player.playWhenReady = playIntent
        currentMediaItem = item.mediaItem
    }

    fun prepare() {
        player.prepare()
    }

    fun seekTo(newPosition: Long) {
        if (duration <= 0L) return
        player.seekTo(newPosition.coerceIn(0..duration))
    }

    fun updatePlayerItems(items: List<PlayerItem>) {
        playerItems = items
        currentMediaItem = null
        if (playerItems.isNotEmpty()) {
            val preferredItem = playerItems.find { it.quality == preferredQuality() }
            val fallbackItem = playerItems.last()
            selectPlayerItem(
                item = preferredItem ?: fallbackItem,
                persistPreference = false,
            )
        }
    }

    fun updateCurrentQuality(quality: String) {
        if (playerItems.isNotEmpty()) {
            val item = playerItems.find { it.quality == quality }
            if (item != null) {
                selectPlayerItem(item, persistPreference = true)
                player.prepare()
            }
        }
    }

    fun pause() {
        player.pause()
    }

    fun play() {
        player.prepare()
        player.play()
    }

    enum class State {
        IDLE,
        BUFFERING,
        READY,
        ENDED
    }

    data class PlayerItem(
        val quality: String,
        val mediaItem: MediaItem,
    )

    data class VideoSize(
        val width: Int,
        val height: Int,
    ) {
        companion object {
            val UNKNOWN = VideoSize(0, 0)

            fun from(videoSize: androidx.media3.common.VideoSize) = VideoSize(
                videoSize.width,
                videoSize.height,
            )
        }
    }
}
