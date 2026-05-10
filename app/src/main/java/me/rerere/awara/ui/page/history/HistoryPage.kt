package me.rerere.awara.ui.page.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.material3.Card
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import me.rerere.awara.R
import me.rerere.awara.data.entity.HistoryItem
import me.rerere.awara.data.entity.HistoryType
import me.rerere.awara.ui.LocalRouterProvider
import me.rerere.awara.ui.component.common.BackButton
import me.rerere.awara.ui.component.ext.items
import me.rerere.awara.ui.component.ext.plus
import me.rerere.awara.ui.component.iwara.MEDIA_LIST_MODE_THUMBNAIL
import me.rerere.awara.ui.component.iwara.MediaListModeButton
import me.rerere.awara.ui.component.iwara.mediaListGridCells
import me.rerere.awara.ui.component.iwara.rememberMediaListModePreference
import me.rerere.awara.util.toLocalDateTimeString
import org.koin.androidx.compose.koinViewModel

@Composable
fun HistoryPage(vm: HistoryVM = koinViewModel()) {
    val itemsPaged = vm.historyItems.collectAsLazyPagingItems()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var listMode by rememberMediaListModePreference()
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(stringResource(R.string.history))
                },
                navigationIcon = {
                    BackButton()
                },
                actions = {
                    MediaListModeButton(
                        value = listMode,
                        onValueChange = { listMode = it },
                    )
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) {
        LazyVerticalStaggeredGrid(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = it + PaddingValues(8.dp),
            columns = mediaListGridCells(listMode),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(itemsPaged) {
                HistoryItem(item = it!!, listMode = listMode)
            }
        }
    }
}

@Composable
private fun HistoryItem(item: HistoryItem, listMode: String) {
    val router = LocalRouterProvider.current
    Card(
        onClick = {
            when(item.type) {
                HistoryType.VIDEO -> router.navigate("video/${item.resourceId}")
                HistoryType.IMAGE -> router.navigate("image/${item.resourceId}")
                else -> /* TODO */ {}
            }
        }
    ) {
        if (listMode == MEDIA_LIST_MODE_THUMBNAIL) {
            Column {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(22 / 16f),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = item.time.toLocalDateTimeString(),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        } else {
            androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .width(164.dp)
                        .aspectRatio(22 / 16f),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = when (item.type) {
                            HistoryType.VIDEO -> stringResource(R.string.video)
                            HistoryType.IMAGE -> stringResource(R.string.image)
                            else -> stringResource(R.string.post)
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = item.time.toLocalDateTimeString(),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}