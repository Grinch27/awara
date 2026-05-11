package me.rerere.awara.ui.component.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun PlayerScaffold(
    fullscreen: Boolean,
    player: @Composable () -> Unit,
    content: @Composable (@Composable () -> Unit) -> Unit
) {
    rememberPlayerFullScreenState().apply {
        if (fullscreen) enterFullScreen() else exitFullScreen()
    }

    if (fullscreen) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            player()
        }
    } else {
        content(player)
    }
}

@Composable
private fun rememberPlayerFullScreenState(): PlayerFullScreenState {
    val context = LocalContext.current
    val activity = context.findActivity()
    val window = activity.window
    val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
    val state = remember {
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        PlayerFullScreenState(windowInsetsController)
    }
    DisposableEffect(state) {
        onDispose {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    return state
}

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("PlayerScaffold requires an Activity context")
}

private class PlayerFullScreenState(
    private val windowInsetsController: WindowInsetsControllerCompat,
) {
    fun enterFullScreen() {
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    fun exitFullScreen() {
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }
}