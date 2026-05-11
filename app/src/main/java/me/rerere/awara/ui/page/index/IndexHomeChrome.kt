package me.rerere.awara.ui.page.index

// TODO(user): Decide whether home should stay a simple landing shell or become the single place for section shortcuts.
// TODO(agent): Keep the chrome dense and EhViewer-like, but do not bring back saved-view strips here.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.rerere.awara.R
import me.rerere.awara.ui.component.common.BetterTabBar

data class IndexQuickAction(
    val key: String,
    val labelRes: Int,
    val icon: ImageVector,
    val badgeCount: Int = 0,
    val onClick: () -> Unit,
)

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
fun IndexQuickAccessStrip(
    actions: List<IndexQuickAction>,
    titleRes: Int = R.string.index_home_shortcuts_title,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) {
        return
    }

    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items = actions, key = { action -> action.key }) { action ->
                ElevatedCard(
                    onClick = action.onClick,
                    modifier = Modifier.width(112.dp),
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = stringResource(action.labelRes),
                            )
                            Text(
                                text = stringResource(action.labelRes),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (action.badgeCount > 0) {
                            Badge(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .align(Alignment.TopEnd),
                            ) {
                                Text(action.badgeCount.toString())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IndexHomeLandingPage(
    navigations: List<IndexNavigation>,
    onNavigationSelected: (String) -> Unit,
    quickActions: List<IndexQuickAction>,
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
        IndexQuickAccessStrip(actions = quickActions)
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
