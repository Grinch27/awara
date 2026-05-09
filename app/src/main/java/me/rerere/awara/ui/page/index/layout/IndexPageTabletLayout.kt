package me.rerere.awara.ui.page.index.layout

// TODO(user): The tablet home shell now keeps the primary navigation on the left like EhViewer; decide later whether secondary tools also need a denser two-pane content mode.
// TODO(agent): If more sections get added, promote this layout into a dedicated adaptive shell instead of stuffing more responsibilities into IndexPage.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.rerere.awara.BuildConfig
import me.rerere.awara.R
import me.rerere.awara.ui.LocalRouterProvider
import me.rerere.awara.ui.component.common.NamedHorizontalPager
import me.rerere.awara.ui.component.common.TodoStatus
import me.rerere.awara.ui.page.index.IndexDrawer
import me.rerere.awara.ui.page.index.IndexQuickAction
import me.rerere.awara.ui.page.index.IndexVM
import me.rerere.awara.ui.page.index.indexNavigations
import me.rerere.awara.ui.page.index.pager.IndexImagePage
import me.rerere.awara.ui.page.index.pager.IndexSubscriptionPage
import me.rerere.awara.ui.page.index.pager.IndexVideoPage
import me.rerere.awara.ui.stores.LocalUserStore
import me.rerere.awara.ui.stores.collectAsState

@Composable
fun IndexPageTabletLayout(vm: IndexVM) {
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
    val currentNavigation = navigations.getOrNull(pagerState.currentPage)
    val savedViews = when (currentNavigation?.name) {
        "video" -> vm.state.savedVideoViews
        "image" -> vm.state.savedImageViews
        else -> emptyList()
    }
    val selectedSavedViewId = when (currentNavigation?.name) {
        "video" -> vm.state.selectedVideoSavedViewId
        "image" -> vm.state.selectedImageSavedViewId
        else -> null
    }
    val onSavedViewSelected: (String?) -> Unit = { savedViewId ->
        when (currentNavigation?.name) {
            "video" -> vm.applyVideoSavedView(savedViewId)
            "image" -> vm.applyImageSavedView(savedViewId)
        }
    }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(currentNavigation?.titleRes ?: R.string.app_name))
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
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier
                    .width(304.dp)
                    .fillMaxHeight(),
            ) {
                IndexDrawer(
                    vm = vm,
                    navigations = navigations,
                    selectedNavigationName = currentNavigation?.name,
                    onNavigationSelected = { navigationName ->
                        val index = navigations.indexOfFirst { it.name == navigationName }
                        if (index >= 0) {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    },
                    quickActions = quickActions,
                    savedViews = savedViews,
                    selectedSavedViewId = selectedSavedViewId,
                    onSavedViewSelected = onSavedViewSelected,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )

            NamedHorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
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