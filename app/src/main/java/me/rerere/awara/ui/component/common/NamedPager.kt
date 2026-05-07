package me.rerere.awara.ui.component.common

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun NamedHorizontalPager(
    modifier: Modifier = Modifier,
    state: PagerState,
    pages: List<String>,
    content: @Composable (String) -> Unit
) {
    HorizontalPager(
        modifier = modifier,
        state = state
    ) { page ->
        content(pages[page])
    }
}