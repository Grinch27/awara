package me.rerere.awara.ui.page.download

import android.content.pm.ActivityInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.media3.common.Player as MediaPlayer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import me.rerere.awara.data.session.SecureSession
import me.rerere.awara.util.ManagedDownloadFiles
import me.rerere.awara.ui.component.iwara.rememberBlockMediaThumbnailsPreference
import me.rerere.awara.util.prettyFileSize
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
import coil3.compose.AsyncImage
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.ui.AspectRatioFrameLayout
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
import okhttp3.OkHttpClient
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

@Composable
fun DownloadPage(
    vm: DownloadVM = koinViewModel(),
    publicHttpClient: OkHttpClient = koinInject(),
) {
    val items = vm.downloadedItems.collectAsLazyPagingItems()
    val tasks by vm.tasks.collectAsState(initial = emptyList())
    val session by SecureSession.state.collectAsState()
    val queue = tasks.filter { it.state != "COMPLETED" && it.state != "CANCELLED" && (it.owner.isBlank() || it.owner == session.sessionId) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf(setOf<Int>()) }
    var pendingDelete by remember { mutableStateOf<List<DownloadItem>>(emptyList()) }
    var deleteFiles by remember { mutableStateOf(false) }
    var operationFailed by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<DownloadItem?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { uri ->
        val item = pendingExport
        pendingExport = null
        if (uri != null && item != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val file = ManagedDownloadFiles.resolveIfManaged(context, item) ?: error("File unavailable")
                    context.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                        ?: error("Unable to open destination")
                }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) { operationFailed = true }
        }
    }
    if (pendingDelete.isNotEmpty()) AlertDialog(
        onDismissRequest = { pendingDelete = emptyList() },
        title = { Text(stringResource(R.string.download_delete_title, pendingDelete.size)) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = deleteFiles, onCheckedChange = { deleteFiles = it })
                Text(stringResource(R.string.download_delete_files))
            }
        },
        confirmButton = { TextButton(onClick = {
            val deleting = pendingDelete
            pendingDelete = emptyList()
            scope.launch {
                try { vm.delete(deleting, deleteFiles); selected = emptySet()
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (_: Exception) { operationFailed = true }
            }
        }) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = { pendingDelete = emptyList() }) { Text(stringResource(R.string.cancel)) } },
    )
    val appBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Local downloaded files keep their existing ExoPlayer path: no network request.
    val state = rememberPlayerState(publicHttpClient) { context, _ ->
        ExoPlayer.Builder(context).build()
    }
    var offlineRequest by remember { mutableStateOf(0) }
    var currentOfflineItem by remember { mutableStateOf<DownloadItem?>(null) }
    fun saveOfflineProgress() {
        val item = currentOfflineItem ?: return
        if (state.player.currentMediaItem?.mediaId == "offline:" + item.id) {
            vm.saveProgress(item, state.player.currentPosition, state.player.duration)
        }
    }
    DisposableEffect(state) {
        val listener = object : MediaPlayer.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { if (!isPlaying) saveOfflineProgress() }
        }
        state.player.addListener(listener)
        onDispose { saveOfflineProgress(); state.player.removeListener(listener) }
    }
    LaunchedEffect(state) {
        while (isActive) { delay(5000); if (state.player.isPlaying) saveOfflineProgress() }
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
                if (operationFailed) item { Text(stringResource(R.string.setting_data_action_failed), color = MaterialTheme.colorScheme.error) }
                if (selected.isNotEmpty()) item {
                    TextButton(onClick = {
                        deleteFiles = false
                        pendingDelete = items.itemSnapshotList.items.filter { it.id in selected }
                    }) { Text(stringResource(R.string.download_delete_title, selected.size)) }
                }
                if (queue.isNotEmpty()) item {
                    Row {
                        TextButton(onClick = { scope.launch { queue.forEach { vm.controlTask(it.id, "PAUSED") } } }) { Text(stringResource(R.string.download_pause_all)) }
                        TextButton(onClick = { scope.launch { queue.filter { it.state in setOf("PAUSED", "FAILED") }.forEach { vm.controlTask(it.id, "QUEUED") } } }) { Text(stringResource(R.string.download_resume_all)) }
                    }
                }
                items(count = queue.size, key = { "task:" + queue[it].id }) { index ->
                    val task = queue[index]
                    Card {
                        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(task.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                            val stateLabel = when (task.state) {
                                "RUNNING" -> R.string.download_running
                                "PAUSED" -> R.string.download_paused
                                "FAILED" -> R.string.download_failed
                                "WAITING_LOGIN" -> R.string.download_waiting_login
                                else -> R.string.download_queued
                            }
                            Text(stringResource(stateLabel) + " · " + prettyFileSize(task.bytes) + if (task.totalBytes > 0) " / " + prettyFileSize(task.totalBytes) else "",
                                style = MaterialTheme.typography.bodySmall)
                            if (task.totalBytes > 0) LinearProgressIndicator(progress = { (task.bytes.toFloat() / task.totalBytes).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                            else if (task.state == "RUNNING") LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Row {
                                val resume = task.state in setOf("PAUSED", "FAILED", "WAITING_LOGIN")
                                TextButton(onClick = { scope.launch { vm.controlTask(task.id, if (resume) "QUEUED" else "PAUSED") } }) {
                                    Text(stringResource(if (resume) R.string.download_resume else R.string.download_pause))
                                }
                                TextButton(onClick = { scope.launch { vm.controlTask(task.id, "CANCELLED") } }) { Text(stringResource(R.string.cancel)) }
                            }
                        }
                    }
                }
                items(count = items.itemCount, key = { items.peek(it)?.id ?: -it - 1 }) { index ->
                    val item = items[index]
                    if (item != null) {
                        DownloadItem(
                            item = item,
                            selected = item.id in selected,
                            onSelect = { selected = if (item.id in selected) selected - item.id else selected + item.id },
                            onExport = { pendingExport = item; exportLauncher.launch("awara-" + item.resourceId + ".mp4") },
                            onDelete = { deleteFiles = false; pendingDelete = listOf(item) }
                        ) {
                            val file = ManagedDownloadFiles.resolveIfManaged(context, item)
                            if (file == null) { operationFailed = true; return@DownloadItem }
                            val request = ++offlineRequest
                            scope.launch {
                                saveOfflineProgress()
                                val saved = vm.progress(item)
                                if (request != offlineRequest) return@launch
                                currentOfflineItem = item
                                val position = if (saved?.fileId == item.path && saved.duration > 0 && saved.position < saved.duration - 5000) saved.position else 0
                                state.player.setMediaItem(MediaItem.Builder().setMediaId("offline:" + item.id)
                                    .setUri(Uri.fromFile(file)).build(), position)
                                state.player.prepare()
                                fullscreen = true
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadItem(item: DownloadItem, selected: Boolean, onSelect: () -> Unit, onExport: () -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    val blocked by rememberBlockMediaThumbnailsPreference()
    val context = LocalContext.current
    val missing = ManagedDownloadFiles.resolveIfManaged(context, item) == null
    Card(onClick = onClick) {
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
                Checkbox(checked = selected, onCheckedChange = { onSelect() })
                if (!blocked) AsyncImage(
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
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1
                    )

                    if (missing) Text(stringResource(R.string.download_file_missing), color = MaterialTheme.colorScheme.error)
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
                TextButton(onClick = onExport, enabled = !missing) { Text(stringResource(R.string.download_export)) }
                IconButton(
                    onClick = onDelete
                ) {
                    Icon(Icons.Outlined.Delete, null)
                }
            }
        }
    }
}