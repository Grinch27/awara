package me.rerere.awara.ui.page.image

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import coil3.request.ImageRequest
import kotlinx.coroutines.flow.distinctUntilChanged
import me.rerere.awara.data.session.SecureSession
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import me.rerere.awara.R
import me.rerere.awara.data.entity.toImageLarge
import me.rerere.awara.ui.LocalRouterProvider
import me.rerere.awara.ui.component.common.BackButton
import me.rerere.awara.ui.component.common.Button
import me.rerere.awara.ui.component.common.ButtonType
import me.rerere.awara.ui.component.common.Spin
import me.rerere.awara.ui.component.common.zoomable.rememberZoomState
import me.rerere.awara.ui.component.common.zoomable.zoomable
import me.rerere.awara.ui.component.iwara.AuthorCard
import me.rerere.awara.ui.component.iwara.RichText
import me.rerere.awara.ui.component.iwara.TagRow
import me.rerere.awara.util.openUrl
import me.rerere.awara.util.shareLink
import me.rerere.awara.util.toLocalDateTimeString
import org.koin.androidx.compose.koinViewModel

@Composable
fun ImagePage(vm: ImageVM = koinViewModel()) {
    val state = vm.state
    val context = LocalContext.current
    val router = LocalRouterProvider.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.state?.title ?: "Image",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    BackButton()
                }
            )
        },
        bottomBar = {
            Surface {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .animateContentSize()
                        .padding(16.dp),
                ) {
                    Card {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            var expaned by remember { mutableStateOf(false) }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = state.state?.title ?: "",
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = if (expaned) Int.MAX_VALUE else 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    if (expaned) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .clickable {
                                            expaned = !expaned
                                        }
                                        .padding(4.dp)
                                )
                            }

                            if (expaned) {
                                RichText(
                                    text = state.state?.body ?: "",
                                    overflow = TextOverflow.Ellipsis,
                                    onLinkClick = {
                                        context.openUrl(it)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    maxLines = if (expaned) Int.MAX_VALUE else 2,
                                )
                            }

                            val tags = state.state?.tags.orEmpty()
                            if (tags.isNotEmpty()) {
                                TagRow(
                                    tags = tags,
                                    onTagClick = { tag ->
                                        router.navigate("search?type=image&tag=${Uri.encode(tag.id)}")
                                    },
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = state.state?.createdAt?.toLocalDateTimeString() ?: "",
                                    style = MaterialTheme.typography.labelMedium
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                Text(
                                    text = stringResource(R.string.num_likes, state.state?.numLikes ?: 0),
                                    style = MaterialTheme.typography.labelMedium
                                )

                                Text(
                                    text = stringResource(R.string.num_views, state.state?.numViews ?: 0),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = {
                                        context.shareLink("https://www.iwara.tv/image/${vm.id}")
                                    }
                                ) {
                                    Icon(Icons.Outlined.Share, null)
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Button(
                                    onClick = { vm.likeOrDislike() },
                                    type = if (state.state?.liked == true) ButtonType.Outlined else ButtonType.Default,
                                    loading = state.likeLoading
                                ) {
                                    Text(
                                        text = if (state.state?.liked == true) stringResource(R.string.dislike) else stringResource(
                                            R.string.like
                                        )
                                    )
                                }
                            }
                        }
                    }

                    AuthorCard(user = state.state?.user, onClickSub = { vm.followOrUnfollow() })
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            Spin(
                modifier = Modifier
                    .background(Color.Black)
                    .fillMaxSize(),
                show = state.loading
            ) {
                val files = state.state?.files.orEmpty()
                val session by SecureSession.state.collectAsState()
                val pagerState = rememberPagerState(pageCount = { files.size })
                val zoomed = remember { mutableStateMapOf<Int, Boolean>() }
                var restored by remember(vm.id) { mutableStateOf(false) }
                LaunchedEffect(files, vm.id) {
                    if (files.isEmpty()) return@LaunchedEffect
                    if (!restored) { pagerState.scrollToPage(state.savedPage.coerceIn(files.indices)); restored = true }
                    snapshotFlow { pagerState.settledPage }.distinctUntilChanged().collect { vm.savePage(it) }
                }
                if (!state.loading && files.isEmpty()) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(if (state.failed) R.string.gallery_failed else R.string.gallery_empty), color = Color.White)
                        TextButton(onClick = vm::load) { Text(stringResource(R.string.gallery_retry)) }
                    }
                } else if (files.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        key = { files.getOrNull(it)?.id ?: it.toString() },
                        beyondViewportPageCount = 1,
                        userScrollEnabled = zoomed[pagerState.currentPage] != true,
                    ) { index ->
                        val file = files.getOrNull(index) ?: return@HorizontalPager
                        val zoomState = rememberZoomState()
                        LaunchedEffect(zoomState.scale) { zoomed[index] = zoomState.scale > 1.01f }
                        var failed by remember(file.id) { mutableStateOf(false) }
                        var loading by remember(file.id) { mutableStateOf(true) }
                        var retry by remember(file.id) { mutableStateOf(0) }
                        val request = remember(file, session.sessionId, retry) {
                            val cacheKey = session.sessionId.ifBlank { "public" } + ":image:" + vm.id + ":" + file.id + ":" + file.name
                            ImageRequest.Builder(context).data(file.toImageLarge()).memoryCacheKey(cacheKey).diskCacheKey(cacheKey).build()
                        }
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            key(retry) {
                                AsyncImage(model = request, contentDescription = stringResource(R.string.gallery_page, index + 1, files.size),
                                    onLoading = { loading = true; failed = false },
                                    onSuccess = { loading = false; failed = false },
                                    onError = { loading = false; failed = true },
                                    modifier = Modifier.fillMaxSize().zoomable(zoomState))
                            }
                            if (loading) CircularProgressIndicator()
                            if (failed) TextButton(onClick = { retry++ }) { Text(stringResource(R.string.gallery_retry)) }
                        }
                    }
                    Text(text = "${pagerState.currentPage + 1}/${files.size}",
                        modifier = Modifier.align(Alignment.BottomCenter), color = Color.White)
                }
            }
        }
    }
}