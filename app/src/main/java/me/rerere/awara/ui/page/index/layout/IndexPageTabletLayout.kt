package me.rerere.awara.ui.page.index.layout

// TODO(user): Decide whether subscription should rejoin the tablet default entry choices after video/image/forum stabilizes.
// TODO(agent): Keep the tablet shell EhViewer-like and route utility entries directly instead of page-level placeholders.

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lens
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.rerere.awara.BuildConfig
import me.rerere.awara.R
import me.rerere.awara.ui.LocalRouterProvider
import me.rerere.awara.ui.component.common.TodoStatus
import me.rerere.awara.ui.page.index.IndexDrawer
import me.rerere.awara.ui.page.index.SETTING_HOME_DEFAULT_SECTION
import me.rerere.awara.ui.page.index.IndexVM
import me.rerere.awara.ui.page.index.indexNavigations
import me.rerere.awara.ui.page.index.pager.IndexImagePage
import me.rerere.awara.ui.page.index.pager.IndexSubscriptionPage
import me.rerere.awara.ui.page.index.pager.IndexVideoPage
import me.rerere.awara.ui.stores.LocalUserStore
import me.rerere.awara.ui.stores.collectAsState
import me.rerere.compose_setting.preference.rememberStringPreference

private val externalRouteNavigations = setOf("history", "download", "setting")

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
    val preferredHomeSection = rememberStringPreference(
        key = SETTING_HOME_DEFAULT_SECTION,
        default = "video",
    )
    val defaultNavigationName = when {
        navigations.any { it.name == preferredHomeSection.value } -> preferredHomeSection.value
        navigations.any { it.name == "video" } -> "video"
        navigations.any { it.name == "image" } -> "image"
        navigations.any { it.name == "forum" } -> "forum"
        userState.user != null && navigations.any { it.name == "subscription" } -> "subscription"
        else -> navigations.firstOrNull()?.name ?: "video"
    }
    var selectedNavigationName by rememberSaveable(userState.user != null) {
        mutableStateOf(defaultNavigationName)
    }
    LaunchedEffect(defaultNavigationName, navigations, selectedNavigationName) {
        if (navigations.none { it.name == selectedNavigationName }) {
            selectedNavigationName = defaultNavigationName
        }
    }
    val currentNavigation = navigations.firstOrNull { it.name == selectedNavigationName }
        ?: navigations.firstOrNull()

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
                        if (navigations.any { it.name == navigationName }) {
                            if (externalRouteNavigations.contains(navigationName)) {
                                navController.navigate(navigationName)
                            } else {
                                selectedNavigationName = navigationName
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                when (currentNavigation?.name) {
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

                    else -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            TodoStatus()
                        }
                    }
                }
            }
        }
    }
}