package me.rerere.awara.ui.page.index.layout

// TODO(user): Keep the phone home page aligned with an EhViewer-like workflow: fast top tab switching, quick entry cards, and no dependency on bottom navigation.
// TODO(agent): If the home chrome needs more sections later, extract a shared home shell composable instead of duplicating more control logic in phone and tablet layouts.

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FeaturedPlayList
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lens
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.rerere.awara.BuildConfig
import me.rerere.awara.R
import me.rerere.awara.ui.LocalRouterProvider
import me.rerere.awara.ui.component.common.NamedHorizontalPager
import me.rerere.awara.ui.component.common.TodoStatus
import me.rerere.awara.ui.component.iwara.Avatar
import me.rerere.awara.ui.page.index.IndexDrawer
import me.rerere.awara.ui.page.index.IndexNavigationTabs
import me.rerere.awara.ui.page.index.IndexQuickAccessStrip
import me.rerere.awara.ui.page.index.IndexQuickAction
import me.rerere.awara.ui.page.index.IndexSavedViewStrip
import me.rerere.awara.ui.page.index.IndexVM
import me.rerere.awara.ui.page.index.indexNavigations
import me.rerere.awara.ui.page.index.pager.IndexImagePage
import me.rerere.awara.ui.page.index.pager.IndexSubscriptionPage
import me.rerere.awara.ui.page.index.pager.IndexVideoPage
import me.rerere.awara.ui.stores.LocalUserStore
import me.rerere.awara.ui.stores.collectAsState

@Composable
fun IndexPagePhoneLayout(vm: IndexVM) {
    val userStore = LocalUserStore.current
    val userState = userStore.collectAsState()
    val navController = LocalRouterProvider.current
    val navigations = remember(userState.user) {
        indexNavigations.filter {
            !it.needLogin || userState.user != null
        }
    }
    val pagerState = rememberPagerState(pageCount = { navigations.size })
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val currentNavigation = navigations.getOrNull(pagerState.currentPage)
    val quickActions = buildList {
        if (userState.user != null) {
            add(
                IndexQuickAction(
                    key = "favorite",
                    labelRes = R.string.drawer_favorite,
                    icon = Icons.Outlined.FavoriteBorder,
                    onClick = {
                        navController.navigate("favorites")
                    },
                )
            )
            add(
                IndexQuickAction(
                    key = "playlist",
                    labelRes = R.string.drawer_playlists,
                    icon = Icons.Outlined.FeaturedPlayList,
                    onClick = {
                        userState.user?.id?.let { userId ->
                            navController.navigate("playlists/$userId")
                        }
                    },
                )
            )
        }
        add(
            IndexQuickAction(
                key = "download",
                labelRes = R.string.drawer_downloads,
                icon = Icons.Outlined.Download,
                onClick = {
                    navController.navigate("download")
                },
            )
        )
        add(
            IndexQuickAction(
                key = "history",
                labelRes = R.string.drawer_history,
                icon = Icons.Outlined.History,
                onClick = {
                    navController.navigate("history")
                },
            )
        )
        add(
            IndexQuickAction(
                key = "setting",
                labelRes = R.string.drawer_setting,
                icon = Icons.Outlined.Settings,
                onClick = {
                    navController.navigate("setting")
                },
            )
        )
    }

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                IndexDrawer(vm)
            }
        },
        drawerState = drawerState,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(text = stringResource(currentNavigation?.titleRes ?: R.string.app_name))
                            Text(
                                text = userState.user?.displayName ?: stringResource(R.string.app_name),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    },
                    navigationIcon = {
                        Avatar(
                            user = userState.user,
                            onClick = {
                                scope.launch { drawerState.open() }
                            },
                            modifier = Modifier.size(32.dp),
                            showOnlineStatus = false,
                        )
                    },
                    actions = {
                        if (BuildConfig.DEBUG) {
                            IconButton(
                                onClick = {
                                    navController.navigate("lab")
                                }
                            ) {
                                Icon(Icons.Outlined.Lens, "App Lab")
                            }
                        }

                        Box {
                            IconButton(
                                onClick = {
                                    navController.navigate("conversations")
                                }
                            ) {
                                Icon(Icons.Outlined.Message, null)
                            }

                            if (vm.state.notificationCounts.messages > 0) {
                                Badge(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp),
                                ) {
                                    Text(vm.state.notificationCounts.messages.toString())
                                }
                            }
                        }

                        Box {
                            IconButton(onClick = { /*TODO*/ }) {
                                Icon(Icons.Outlined.Notifications, null)
                            }

                            if (vm.state.notificationCounts.notifications > 0) {
                                Badge(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp),
                                ) {
                                    Text(vm.state.notificationCounts.notifications.toString())
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                navController.navigate("search")
                            }
                        ) {
                            Icon(Icons.Outlined.Search, "Search")
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                IndexNavigationTabs(
                    navigations = navigations,
                    selectedIndex = pagerState.currentPage,
                    onNavigationSelected = { index ->
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                IndexQuickAccessStrip(
                    actions = quickActions,
                    modifier = Modifier.fillMaxWidth(),
                )

                when (currentNavigation?.name) {
                    "video" -> {
                        IndexSavedViewStrip(
                            savedViews = vm.state.savedVideoViews,
                            selectedSavedViewId = vm.state.selectedVideoSavedViewId,
                            onSavedViewSelected = vm::applyVideoSavedView,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    "image" -> {
                        IndexSavedViewStrip(
                            savedViews = vm.state.savedImageViews,
                            selectedSavedViewId = vm.state.selectedImageSavedViewId,
                            onSavedViewSelected = vm::applyImageSavedView,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                NamedHorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    pages = navigations.map { it.name },
                ) { page ->
                    when (page) {
                        "subscription" -> {
                            IndexSubscriptionPage(vm)
                        }

                        "video" -> {
                            IndexVideoPage(vm)
                        }

                        "image" -> {
                            IndexImagePage(vm)
                        }

                        "forum" -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                TodoStatus()
                            }
                        }
                    }
                }
            }
        }
    }
}