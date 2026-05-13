package me.rerere.awara.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.rerere.awara.ui.component.common.DialogProvider
import me.rerere.awara.ui.component.common.MessageProvider
import me.rerere.awara.ui.page.download.DownloadPage
import me.rerere.awara.ui.page.favorites.FavoritesPage
import me.rerere.awara.ui.page.follow.FollowPage
import me.rerere.awara.ui.page.forum.ForumSectionPage
import me.rerere.awara.ui.page.forum.ForumThreadPage
import me.rerere.awara.ui.page.friends.FriendsPage
import me.rerere.awara.ui.page.history.HistoryPage
import me.rerere.awara.ui.page.image.ImagePage
import me.rerere.awara.ui.page.index.IndexPage
import me.rerere.awara.ui.page.lab.LabPage
import me.rerere.awara.ui.page.login.LoginPage
import me.rerere.awara.ui.page.message.ConversationsPage
import me.rerere.awara.ui.page.message.MessagePage
import me.rerere.awara.ui.page.playlist.PlaylistDetailPage
import me.rerere.awara.ui.page.playlist.PlaylistsPage
import me.rerere.awara.ui.page.search.SearchPage
import me.rerere.awara.ui.page.search.SearchMediaType
import me.rerere.awara.ui.page.setting.SettingPage
import me.rerere.awara.ui.page.user.UserPage
import me.rerere.awara.ui.page.video.VideoPage
import me.rerere.awara.ui.stores.LocalUserStore
import me.rerere.awara.ui.stores.UserStoreProvider
import me.rerere.awara.ui.stores.collectAsState
import me.rerere.awara.ui.theme.AwaraTheme

private const val TAG = "RouterActivity"

class RouterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val splashScreen = installSplashScreen().apply { setKeepOnScreenCondition { true } }
        super.onCreate(savedInstanceState)
        setContent {
            AwaraTheme {
                ContextProvider {
                    UserStoreProvider {
                        val userState = LocalUserStore.current.collectAsState()
                        LaunchedEffect(userState.refreshing) {
                            splashScreen.setKeepOnScreenCondition { userState.refreshing }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!userState.refreshing) {
                                Routes()
                            } else {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ContextProvider(
        content: @Composable () -> Unit
    ) {
        MessageProvider {
            DialogProvider {
                content()
            }
        }
    }

    @Composable
    private fun Routes() {
        val navController = rememberNavController()
        CompositionLocalProvider(
            LocalRouterProvider provides navController
        ) {
            NavHost(
                modifier = Modifier
                    .fillMaxSize()
                    // 防止夜间模式下切换页面闪白屏
                    .background(MaterialTheme.colorScheme.background),
                navController = navController,
                startDestination = "index"
            ) {
                composable(route = "index") {
                    IndexPage()
                }

                composable("login") {
                    LoginPage()
                }

                composable(
                    route = "video/{id}",
                    arguments = listOf(
                        navArgument("id") {
                            type = NavType.StringType
                        }
                    )
                ) {
                    VideoPage()
                }

                composable("image/{id}") {
                    ImagePage()
                }

                composable("user/{id}") {
                    UserPage()
                }

                composable(
                    route = "user/{userId}/follow",
                    arguments = listOf(
                        navArgument("userId") {
                            type = NavType.StringType
                        }
                    )
                ) {
                    FollowPage()
                }

                composable(
                    route = "playlists/{userId}",
                    arguments = listOf(
                        navArgument("userId") {
                            type = NavType.StringType
                        }
                    )
                ) {
                    PlaylistsPage()
                }

                composable(
                    route = "playlist/{id}",
                    arguments = listOf(
                        navArgument("id") {
                            type = NavType.StringType
                        }
                    )
                ) {
                    PlaylistDetailPage()
                }

                composable("favorites") {
                    FavoritesPage()
                }

                composable(
                    route = "forum/section/{sectionId}",
                    arguments = listOf(
                        navArgument("sectionId") {
                            type = NavType.StringType
                        }
                    ),
                ) {
                    ForumSectionPage()
                }

                composable(
                    route = "forum/thread/{threadId}",
                    arguments = listOf(
                        navArgument("threadId") {
                            type = NavType.StringType
                        }
                    ),
                ) {
                    ForumThreadPage()
                }

                composable("setting") {
                    SettingPage()
                }

                composable(
                    route = "search?type={type}&tag={tag}",
                    arguments = listOf(
                        navArgument("type") {
                            type = NavType.StringType
                            defaultValue = "video"
                        },
                        navArgument("tag") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                    ),
                ) {
                    SearchPage(
                        initialSearchType = it.arguments?.getString("type"),
                        initialTag = it.arguments?.getString("tag"),
                        onBack = {
                            navController.popBackStack()
                        },
                        onOpenMedia = { media ->
                            when (media.type) {
                                SearchMediaType.VIDEO -> navController.navigate("video/${media.id}")
                                SearchMediaType.IMAGE -> navController.navigate("image/${media.id}")
                            }
                        },
                        onOpenUser = { user ->
                            if (user.hasNavigableProfile) {
                                navController.navigate("user/${Uri.encode(user.username)}")
                            }
                        },
                        onOpenPlaylist = { playlist ->
                            navController.navigate("playlist/${Uri.encode(playlist.id)}")
                        },
                        onOpenForumThread = { threadId ->
                            if (threadId.isNotBlank()) {
                                navController.navigate("forum/thread/${Uri.encode(threadId)}")
                            }
                        },
                    )
                }

                composable("history") {
                    HistoryPage()
                }

                composable("download") {
                    DownloadPage()
                }

                composable("friends/{userId}?self={self}", arguments = listOf(
                    navArgument("userId") {
                        type = NavType.StringType
                    },
                    navArgument("self") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )) {
                    FriendsPage()
                }

                composable("conversations") {
                    ConversationsPage()
                }

                composable(
                    "message/{id}", arguments = listOf(
                        navArgument("id") {
                            type = NavType.StringType
                        })
                ) {
                    MessagePage()
                }

                composable("lab") {
                    LabPage()
                }
            }
        }
    }
}