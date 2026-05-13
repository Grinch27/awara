package me.rerere.awara.ui.page.index.pager

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.awara.ui.LocalRouterProvider
import me.rerere.awara.ui.component.common.UiStateBox
import me.rerere.awara.ui.page.forum.ForumIndexVM
import me.rerere.awara.ui.page.forum.ForumSectionCard
import org.koin.androidx.compose.koinViewModel

@Suppress("UNUSED_PARAMETER")
@Composable
fun IndexForumPage(
    onBrowseVideo: () -> Unit,
    onBrowseImage: () -> Unit,
    vm: ForumIndexVM = koinViewModel(),
) {
    val router = LocalRouterProvider.current
    val listState = rememberLazyListState()

    UiStateBox(
        modifier = Modifier.fillMaxSize(),
        state = vm.state.uiState,
        onErrorRetry = vm::loadSections,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(vm.state.sections, key = { it.id }) { section ->
                ForumSectionCard(
                    section = section,
                    onClick = {
                        router.navigate("forum/section/${Uri.encode(section.id)}")
                    },
                )
            }
        }
    }
}
