package me.rerere.awara.ui.page.index

// TODO(user): Decide whether the phone home toolbar also needs a one-tap filter/search entry, or if keeping navigation-focused actions there is enough.
// TODO(agent): If the left sidebar keeps growing, split it into account, home navigation, and utility sections instead of letting this file become another mini-shell.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.rerere.awara.R
import me.rerere.awara.domain.feed.SavedFeedView
import me.rerere.awara.ui.LocalMessageProvider
import me.rerere.awara.ui.LocalRouterProvider
import me.rerere.awara.ui.component.iwara.Avatar
import me.rerere.awara.ui.component.iwara.RequireLoginVisible
import me.rerere.awara.ui.stores.LocalUserStore
import me.rerere.awara.ui.stores.UserStoreAction
import me.rerere.awara.ui.stores.collectAsState

@Composable
fun IndexDrawer(
    vm: IndexVM,
    navigations: List<IndexNavigation>,
    selectedNavigationName: String?,
    onNavigationSelected: (String) -> Unit,
    quickActions: List<IndexQuickAction>,
    savedViews: List<SavedFeedView>,
    selectedSavedViewId: String?,
    onSavedViewSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val userStore = LocalUserStore.current
    val userState = userStore.collectAsState()
    val router = LocalRouterProvider.current
    val message = LocalMessageProvider.current
    val selectedNavigationTitle = navigations
        .firstOrNull { it.name == selectedNavigationName }
        ?.let { stringResource(it.titleRes) }

    LaunchedEffect(userState.user?.id) {
        userState.user?.id?.let { userId ->
            vm.loadCounts(userId)
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Avatar(
                    modifier = Modifier.size(52.dp),
                    user = userState.user,
                    onClick = {
                        router.navigate("login")
                    },
                    showOnlineStatus = false,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                    Text(
                        text = selectedNavigationTitle ?: stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = userState.user?.displayName ?: "未登录",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (userState.user != null) {
                        Text(
                            text = userState.user.displayHandle,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (userState.user != null) {
                    IconButton(
                        onClick = {
                            userStore(UserStoreAction.Logout)
                            message.info {
                                Text("已登出")
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.ExitToApp, "Logout")
                    }
                }
            }
        }

        RequireLoginVisible {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                DrawerStatCard(
                    value = vm.state.followingCount.toString(),
                    label = stringResource(R.string.following),
                    onClick = {
                        router.navigate("user/${userState.user?.id}/follow")
                    },
                    modifier = Modifier.weight(1f),
                )
                DrawerStatCard(
                    value = vm.state.followerCount.toString(),
                    label = stringResource(R.string.follower),
                    onClick = {
                        router.navigate("user/${userState.user?.id}/follow")
                    },
                    modifier = Modifier.weight(1f),
                )
                DrawerStatCard(
                    value = vm.state.friendsCount.toString(),
                    label = stringResource(R.string.friends),
                    onClick = {
                        router.navigate("friends/${userState.user?.id}?self=true")
                    },
                    badgeCount = vm.state.notificationCounts.friendRequests,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        navigations.forEach { navigation ->
            DrawerItem(
                icon = {
                    navigation.icon()
                },
                label = {
                    Text(
                        text = stringResource(navigation.titleRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                selected = selectedNavigationName == navigation.name,
                onClick = {
                    onNavigationSelected(navigation.name)
                },
            )
        }

        if (quickActions.isNotEmpty()) {
            DrawerSectionTitle(stringResource(R.string.index_home_shortcuts_title))
            quickActions.forEach { action ->
                DrawerItem(
                    icon = {
                        Icon(action.icon, stringResource(action.labelRes))
                    },
                    label = {
                        Text(
                            text = stringResource(action.labelRes),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    tail = if (action.badgeCount > 0) {
                        {
                            Badge {
                                Text(action.badgeCount.toString())
                            }
                        }
                    } else {
                        null
                    },
                    onClick = action.onClick,
                )
            }
        }

        if (savedViews.isNotEmpty()) {
            DrawerSectionTitle(stringResource(R.string.saved_views_title))
            DrawerItem(
                label = {
                    Text(stringResource(R.string.saved_view_chip_all))
                },
                selected = selectedSavedViewId == null,
                onClick = {
                    onSavedViewSelected(null)
                },
            )
            savedViews.forEach { savedView ->
                DrawerItem(
                    label = {
                        Text(
                            text = savedView.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    selected = selectedSavedViewId == savedView.id,
                    onClick = {
                        onSavedViewSelected(savedView.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun DrawerSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun DrawerStatCard(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
) {
    Card(
        modifier = modifier,
        onClick = onClick,
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (badgeCount > 0) {
                Badge(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd),
                ) {
                    Text(badgeCount.toString())
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(
    label: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    tail: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
) {
    ProvideTextStyle(
        if (selected) {
            MaterialTheme.typography.titleMedium
        } else {
            MaterialTheme.typography.bodyLarge
        },
    ) {
        Surface(
            color = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
            },
            shape = RoundedCornerShape(24.dp),
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .fillMaxWidth(),
            ) {
                if (icon != null) {
                    icon()
                } else {
                    Spacer(modifier = Modifier.width(24.dp))
                }
                label()
                Spacer(modifier = Modifier.weight(1f))
                tail?.invoke()
            }
        }
    }
}