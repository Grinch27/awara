package me.rerere.awara.ui.page.index.pager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import me.rerere.awara.ui.component.common.Spin
import me.rerere.awara.ui.component.common.UiStateBox
import me.rerere.awara.ui.component.iwara.MediaCard
import me.rerere.awara.ui.component.iwara.MediaListModeButton
import me.rerere.awara.ui.component.iwara.mediaListGridCells
import me.rerere.awara.ui.component.iwara.rememberMediaListModePreference
import me.rerere.awara.ui.page.index.IndexVM

@Composable
fun IndexSubscriptionPage(
    vm: IndexVM,
) {
    var listMode by rememberMediaListModePreference()
    val gridState = rememberLazyStaggeredGridState()

    LaunchedEffect(gridState, vm.state.subscriptions.size, vm.state.subscriptionHasMore, vm.state.subscriptionLoadingMore) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }.collectLatest { lastVisibleIndex ->
            if (
                vm.state.subscriptionHasMore &&
                !vm.state.subscriptionLoadingMore &&
                vm.state.subscriptions.isNotEmpty() &&
                lastVisibleIndex >= vm.state.subscriptions.size - 6
            ) {
                vm.loadNextSubscriptionPage()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            var showDropdown by remember {
                mutableStateOf(false)
            }

            FilledTonalButton(
                onClick = { showDropdown = true },
            ) {
                Text(stringResource(vm.state.subscriptionType.id))
            }

            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
            ) {
                IndexVM.SubscriptionType.values().forEach {
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(it.id))
                        },
                        onClick = {
                            vm.changeSubscriptionType(it)
                            showDropdown = false
                        },
                    )
                }
            }

            MediaListModeButton(
                value = listMode,
                onValueChange = { listMode = it },
            )
        }

        UiStateBox(
            state = vm.state.subscriptionState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            onErrorRetry = {
                vm.loadSubscriptions()
            },
        ) {
            LazyVerticalStaggeredGrid(
                modifier = Modifier.fillMaxSize(),
                state = gridState,
                columns = mediaListGridCells(listMode),
                verticalItemSpacing = 8.dp,
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(vm.state.subscriptions) {
                    MediaCard(media = it, listMode = listMode)
                }

                if (vm.state.subscriptionLoadingMore) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Spin(
                            show = true,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}