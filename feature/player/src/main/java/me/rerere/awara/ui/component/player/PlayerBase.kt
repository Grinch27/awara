package me.rerere.awara.ui.component.player

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ui.StyledPlayerView

private const val TAG = "PlayerBase"

// 纯粹的播放器组件，无UI附加
@Composable
fun PlayerBase(
    modifier: Modifier = Modifier,
    state: PlayerState,
) {
    AndroidView(
        factory = {
            StyledPlayerView(it).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                keepScreenOn = true
                player = state.player
                resizeMode = state.resizeMode
                useController = false
                clipToOutline = true
            }
        },
        update = {
            it.resizeMode = state.resizeMode
        },
        modifier = modifier.background(Color.Black)
    )
}