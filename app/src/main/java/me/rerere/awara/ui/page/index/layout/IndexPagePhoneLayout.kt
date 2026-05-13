package me.rerere.awara.ui.page.index.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import me.rerere.awara.R
import me.rerere.awara.ui.LocalRouterProvider
import me.rerere.awara.ui.page.index.IndexDrawer
import me.rerere.awara.ui.page.index.SETTING_HOME_DEFAULT_SECTION
import me.rerere.awara.ui.page.index.IndexVM
import me.rerere.awara.ui.page.index.indexNavigations
import me.rerere.awara.ui.page.index.pager.IndexForumPage
import me.rerere.awara.ui.page.index.pager.IndexImagePage
import me.rerere.awara.ui.page.index.pager.IndexSubscriptionPage
import me.rerere.awara.ui.page.index.pager.IndexVideoPage
import me.rerere.awara.ui.stores.LocalUserStore
import me.rerere.awara.ui.stores.collectAsState
import me.rerere.compose_setting.preference.rememberStringPreference

private val externalRouteNavigations = setOf("history", "download", "setting")
private val defaultEntryNavigations = setOf("subscription", "video", "image", "forum")

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
    val contentNavigations = remember(navigations) {
        navigations.filterNot { it.name in externalRouteNavigations }
    }
    val preferredHomeSection = rememberStringPreference(
        key = SETTING_HOME_DEFAULT_SECTION,
        default = "video",
    )
    val preferredDefaultEntry = preferredHomeSection.value.takeIf { preferredName ->
        preferredName in defaultEntryNavigations && contentNavigations.any { it.name == preferredName }
    }
    val defaultNavigationName = when {
        preferredDefaultEntry != null -> preferredDefaultEntry
        contentNavigations.any { it.name == "video" } -> "video"
        contentNavigations.any { it.name == "image" } -> "image"
        contentNavigations.any { it.name == "forum" } -> "forum"
        contentNavigations.any { it.name == "subscription" } -> "subscription"
        else -> contentNavigations.firstOrNull()?.name ?: "video"
    }
    var selectedNavigationName by rememberSaveable(userState.user != null) {
        mutableStateOf(defaultNavigationName)
    }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    LaunchedEffect(preferredHomeSection.value, defaultNavigationName) {
        if (preferredHomeSection.value !in defaultEntryNavigations) {
            preferredHomeSection.value = defaultNavigationName
        }
    }
    LaunchedEffect(defaultNavigationName, navigations, selectedNavigationName) {
        if (contentNavigations.none { it.name == selectedNavigationName }) {
            selectedNavigationName = defaultNavigationName
        }
    }
    val currentNavigation = contentNavigations.firstOrNull { it.name == selectedNavigationName }
        ?: contentNavigations.firstOrNull()

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
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
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
        drawerState = drawerState,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        IndexTopBarTitle(
                            currentNavigation = currentNavigation,
                            contentNavigations = contentNavigations,
                            onNavigationSelected = { navigationName ->
                                if (contentNavigations.any { it.name == navigationName }) {
                                    selectedNavigationName = navigationName
                                }
                            },
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch { drawerState.open() }
                            },
                        ) {
                            Icon(Icons.Outlined.Menu, stringResource(R.string.app_name))
                        }
                    },
                    actions = {
                        IndexTopBarActions(
                            onSearch = { navController.navigate("search") },
                        )
                    },
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
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
                        IndexForumPage(
                            onBrowseVideo = { selectedNavigationName = "video" },
                            onBrowseImage = { selectedNavigationName = "image" },
                        )
                    }

                    else -> {
                        IndexVideoPage(vm)
                    }
                }
            }
        }
    }
}