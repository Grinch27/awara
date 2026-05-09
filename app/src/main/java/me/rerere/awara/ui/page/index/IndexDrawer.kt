package me.rerere.awara.ui.page.index

// TODO(user): Decide whether the phone home toolbar also needs a one-tap filter/search entry, or if keeping navigation-focused actions there is enough.
// TODO(agent): If the left sidebar keeps growing, split it into account, home navigation, and utility sections instead of letting this file become another mini-shell.

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.TextButton
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
    onManageSavedViews: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val userStore = LocalUserStore.current
    val userState = userStore.collectAsState()
    val router = LocalRouterProvider.current
    val message = LocalMessageProvider.current
    val primaryNavigations = navigations.filter { it.name != "forum" }
    val communityNavigations = navigations.filter { it.name == "forum" }
    val selectedNavigationTitle = navigations
        .firstOrNull { it.name == selectedNavigationName }
        ?.let { stringResource(it.titleRes) }

    LaunchedEffect(userState.user?.id) {
        userState.user?.id?.let { userId ->
            vm.loadCounts(userId)
        }
    }

    val accountSubtitle = userState.user?.displayName ?: "未登录"

    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 2.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 16.dp),
            ) {
                Avatar(
                    modifier = Modifier.size(52.dp),
                    user = userState.user,
                    onClick = {
                        userState.user?.id?.let { userId ->
                            router.navigate("user/$userId")
                        } ?: router.navigate("login")
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
                        text = accountSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    userState.user?.let { user ->
                        Text(
                            text = user.displayHandle,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        DrawerHeaderChip(
                            text = selectedNavigationTitle ?: stringResource(R.string.app_name),
                        )
                        DrawerHeaderChip(
                            text = if (userState.user != null) {
                                accountSubtitle
                            } else {
                                stringResource(R.string.saved_views_guest_state)
                            },
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
                        Icon(Icons.Outlined.ExitToApp, contentDescription = "Logout")
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

        DrawerNavigationSection(
            title = stringResource(R.string.index_drawer_primary_section_title),
            navigations = primaryNavigations,
            selectedNavigationName = selectedNavigationName,
            onNavigationSelected = onNavigationSelected,
        )

        DrawerNavigationSection(
            title = stringResource(R.string.index_drawer_community_section_title),
            navigations = communityNavigations,
            selectedNavigationName = selectedNavigationName,
            onNavigationSelected = onNavigationSelected,
        )

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

        if (savedViews.isNotEmpty() || onManageSavedViews != null) {
            DrawerSectionTitle(
                text = stringResource(R.string.saved_views_title),
                supportingText = stringResource(
                    R.string.saved_views_manage_summary,
                    savedViews.size,
                    savedViews.count(SavedFeedView::pinned),
                ),
                actionLabel = if (onManageSavedViews != null) {
                    stringResource(R.string.saved_views_manage_action)
                } else {
                    null
                },
                onActionClick = onManageSavedViews,
            )
            DrawerItem(
                label = {
                    Text(stringResource(R.string.saved_view_chip_all))
                },
                supporting = {
                    Text(stringResource(R.string.saved_view_meta_current_filters))
                },
                selected = selectedSavedViewId == null,
                onClick = {
                    onSavedViewSelected(null)
                },
            )
            val smartViews = savedViews.filter { it.smartSubscription }
            val standardViews = savedViews.filterNot { it.smartSubscription }

            if (smartViews.isNotEmpty()) {
                DrawerSectionTitle(stringResource(R.string.saved_views_smart_title))
            }
            smartViews.forEach { savedView ->
                val supportingText = buildSavedViewSupportingText(savedView)
                DrawerItem(
                    label = {
                        Text(
                            text = savedView.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supporting = supportingText.takeIf(String::isNotEmpty)?.let { text ->
                        {
                            Text(
                                text = text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    selected = selectedSavedViewId == savedView.id,
                    onClick = {
                        onSavedViewSelected(savedView.id)
                    },
                )
            }

            if (standardViews.isNotEmpty()) {
                DrawerSectionTitle(stringResource(R.string.saved_views_manual_title))
            }
            standardViews.forEach { savedView ->
                val supportingText = buildList {
                    if (savedView.pinned) {
                        add(stringResource(R.string.saved_view_meta_pinned))
                    }
                    if (savedView.filters.isNotEmpty()) {
                        add(stringResource(R.string.saved_view_meta_filters, savedView.filters.size))
                    }
                    if (savedView.tags.isNotEmpty()) {
                        add(savedView.tags.joinToString(separator = "  ") { tag -> "#$tag" })
                    }
                    savedView.description.trim().takeIf(String::isNotEmpty)?.let(::add)
                }.joinToString(separator = " · ")
                DrawerItem(
                    label = {
                        Text(
                            text = savedView.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supporting = supportingText.takeIf(String::isNotEmpty)?.let { text ->
                        {
                            Text(
                                text = text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
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
private fun DrawerNavigationSection(
    title: String,
    navigations: List<IndexNavigation>,
    selectedNavigationName: String?,
    onNavigationSelected: (String) -> Unit,
) {
    if (navigations.isEmpty()) {
        return
    }

    DrawerSectionTitle(title)
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
}

@Composable
private fun buildSavedViewSupportingText(savedView: SavedFeedView): String {
    return buildList {
        if (savedView.pinned) {
            add(stringResource(R.string.saved_view_meta_pinned))
        }
        if (savedView.filters.isNotEmpty()) {
            add(stringResource(R.string.saved_view_meta_filters, savedView.filters.size))
        }
        if (savedView.tags.isNotEmpty()) {
            add(savedView.tags.joinToString(separator = "  ") { tag -> "#$tag" })
        }
        savedView.description.trim().takeIf(String::isNotEmpty)?.let(::add)
    }.joinToString(separator = " · ")
}

@Composable
private fun DrawerSectionTitle(
    text: String,
    supportingText: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            supportingText?.takeIf(String::isNotEmpty)?.let { meta ->
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (actionLabel != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
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
    supporting: (@Composable () -> Unit)? = null,
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
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            border = if (selected) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
            } else {
                null
            },
            tonalElevation = if (selected) 1.dp else 0.dp,
            shape = RoundedCornerShape(24.dp),
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .fillMaxWidth(),
            ) {
                if (icon != null) {
                    Surface(
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .padding(6.dp),
                        ) {
                            icon()
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(36.dp))
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    label()
                    supporting?.let {
                        ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                            it()
                        }
                    }
                }
                tail?.invoke()
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .padding(end = 2.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.fillMaxSize(),
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerHeaderChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}