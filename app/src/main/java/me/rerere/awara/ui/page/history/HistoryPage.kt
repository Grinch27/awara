// Requirements: compact history with explicit bulk local deletion; no remote writes.
// Verify: stable paging keys, selection, blocked covers and empty state.
package me.rerere.awara.ui.page.history

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import me.rerere.awara.R
import me.rerere.awara.data.entity.HistoryType
import me.rerere.awara.domain.media.MediaCardModel
import me.rerere.awara.ui.LocalRouterProvider
import me.rerere.awara.ui.component.common.BackButton
import me.rerere.awara.ui.component.ext.items
import me.rerere.awara.ui.component.ext.plus
import me.rerere.awara.ui.component.iwara.*
import me.rerere.awara.util.toLocalDateTimeString
import org.koin.androidx.compose.koinViewModel

@Composable
fun HistoryPage(vm: HistoryVM = koinViewModel()) {
    val itemsPaged = vm.historyItems.collectAsLazyPagingItems()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listMode by rememberMediaListModePreference()
    val router = LocalRouterProvider.current
    var selected by remember { mutableStateOf(setOf<Long>()) }
    var pendingDelete by remember { mutableStateOf(false) }
    var clearAll by remember { mutableStateOf(false) }
    if (pendingDelete) AlertDialog(
        onDismissRequest = { pendingDelete = false },
        title = { Text(stringResource(R.string.history_remove_title)) },
        text = { Text(stringResource(if (clearAll) R.string.history_remove_all_text else R.string.history_remove_selected_text)) },
        confirmButton = { TextButton(onClick = {
            if (clearAll) vm.clearAll() else vm.delete(selected.toList())
            selected = emptySet(); pendingDelete = false
        }) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = { pendingDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
    Scaffold(topBar = {
        LargeTopAppBar(title = { Text(stringResource(R.string.history)) }, navigationIcon = { BackButton() },
            actions = {
                if (selected.isNotEmpty()) TextButton(onClick = { clearAll = false; pendingDelete = true }) {
                    Text(stringResource(R.string.history_remove_selected, selected.size))
                }
                TextButton(onClick = { clearAll = true; pendingDelete = true }) { Text(stringResource(R.string.history_clear_all)) }
            }, scrollBehavior = scrollBehavior)
    }) { padding ->
        LazyVerticalStaggeredGrid(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = padding + PaddingValues(8.dp), columns = mediaListGridCells(listMode),
            verticalItemSpacing = 6.dp, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (itemsPaged.itemCount == 0 && itemsPaged.loadState.refresh is androidx.paging.LoadState.NotLoading) {
                item(span = StaggeredGridItemSpan.FullLine) { Text(stringResource(R.string.history_empty), Modifier.padding(16.dp)) }
            }
            items(itemsPaged, key = { it.id }) { item ->
                if (item != null) CompactMediaCard(
                    media = MediaCardModel(item.resourceId, item.title,
                        stringResource(if (item.type == HistoryType.IMAGE) R.string.image else R.string.video),
                        item.time.toLocalDateTimeString(), item.thumbnail, item.type == HistoryType.IMAGE),
                    listMode = listMode, viewsLabel = item.time.toLocalDateTimeString(), likesLabel = "",
                    privateLabel = "", selected = item.id in selected,
                    onLongClick = { selected = if (item.id in selected) selected - item.id else selected + item.id },
                    onClick = {
                        if (selected.isNotEmpty()) selected = if (item.id in selected) selected - item.id else selected + item.id
                        else if (item.resourceId.isNotBlank()) when (item.type) {
                            HistoryType.VIDEO -> router.navigate("video/" + Uri.encode(item.resourceId))
                            HistoryType.IMAGE -> router.navigate("image/" + Uri.encode(item.resourceId))
                            HistoryType.USER -> router.navigate("user/" + Uri.encode(item.resourceId))
                        }
                    },
                )
            }
        }
    }
}
