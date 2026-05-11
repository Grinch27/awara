package me.rerere.awara.ui.page.index

// TODO(user): Decide whether home should stay a simple landing shell or become the single place for section shortcuts.
// TODO(agent): Keep the chrome dense and EhViewer-like, but do not bring back saved-view strips or duplicate utility actions here.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.rerere.awara.R
import me.rerere.awara.ui.component.common.BetterTabBar

@Composable
fun IndexNavigationTabs(
    navigations: List<IndexNavigation>,
    selectedIndex: Int,
    onNavigationSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BetterTabBar(modifier = modifier, selectedTabIndex = selectedIndex) {
        navigations.forEachIndexed { index, navigation ->
            Tab(
                selected = selectedIndex == index,
                onClick = {
                    onNavigationSelected(index)
                },
                text = {
                    Text(
                        text = stringResource(navigation.titleRes),
                        maxLines = 1,
                    )
                },
                icon = {
                    navigation.icon()
                },
            )
        }
    }
}

@Composable
fun IndexHomeLandingPage(
    navigations: List<IndexNavigation>,
    onNavigationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IndexNavigationCardStrip(
            navigations = navigations.filterNot { it.name == "home" },
            onNavigationSelected = onNavigationSelected,
        )
    }
}

@Composable
private fun IndexNavigationCardStrip(
    navigations: List<IndexNavigation>,
    onNavigationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (navigations.isEmpty()) {
        return
    }

    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.index_drawer_primary_section_title),
            style = MaterialTheme.typography.titleSmall,
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items = navigations, key = { navigation -> navigation.name }) { navigation ->
                ElevatedCard(
                    onClick = {
                        onNavigationSelected(navigation.name)
                    },
                    modifier = Modifier.width(120.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        navigation.icon()
                        Text(
                            text = stringResource(navigation.titleRes),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
