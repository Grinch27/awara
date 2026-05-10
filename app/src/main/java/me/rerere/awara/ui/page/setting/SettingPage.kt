package me.rerere.awara.ui.page.setting

// TODO(user): Decide whether saved-view import/export should stay under Settings or move into a dedicated diagnostics/data management page.
// TODO(agent): If more diagnostics actions land, split this screen into smaller settings sections or feature-specific pages instead of growing one large composable.

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.FeaturedPlayList
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.rerere.awara.R
import me.rerere.awara.data.repo.LocalDataSummary
import me.rerere.awara.domain.feed.FeedScope
import me.rerere.awara.ui.LocalDialogProvider
import me.rerere.awara.ui.LocalMessageProvider
import me.rerere.awara.ui.LocalRouterProvider
import me.rerere.awara.ui.component.common.Avatar
import me.rerere.awara.ui.component.common.BackButton
import me.rerere.awara.ui.component.iwara.MEDIA_LIST_MODE_DETAIL
import me.rerere.awara.ui.component.iwara.MEDIA_LIST_MODE_THUMBNAIL
import me.rerere.awara.ui.component.iwara.SETTING_MEDIA_LIST_MODE
import me.rerere.awara.ui.page.savedview.savedFeedViewsRoute
import me.rerere.awara.util.DEFAULT_NETWORK_DOH_ENDPOINT
import me.rerere.awara.util.DEFAULT_NETWORK_DOH_UPSTREAM
import me.rerere.awara.util.SETTING_NETWORK_DOH_ENABLED
import me.rerere.awara.util.SETTING_NETWORK_DOH_ENDPOINT
import me.rerere.awara.util.SETTING_NETWORK_DOH_UPSTREAM
import me.rerere.awara.util.AppLogger
import me.rerere.awara.util.openUrl
import me.rerere.awara.util.readTextDocument
import me.rerere.awara.util.writeTextDocument
import me.rerere.compose_setting.components.SettingItemCategory
import me.rerere.compose_setting.components.types.SettingBooleanItem
import me.rerere.compose_setting.components.types.SettingLinkItem
import me.rerere.compose_setting.components.types.SettingPickerItem
import me.rerere.compose_setting.preference.rememberBooleanPreference
import me.rerere.compose_setting.preference.rememberIntPreference
import me.rerere.compose_setting.preference.rememberStringPreference
import org.koin.androidx.compose.koinViewModel

private data class PendingExport(
    val fileName: String,
    val content: String,
    val successMessage: String,
)

@Composable
fun SettingPage(vm: SettingVM = koinViewModel()) {
    val context = LocalContext.current
    val dialog = LocalDialogProvider.current
    val message = LocalMessageProvider.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val router = LocalRouterProvider.current
    val coroutineScope = rememberCoroutineScope()
    var pendingExport by remember {
        mutableStateOf<PendingExport?>(null)
    }
    var localDataSummary by remember {
        mutableStateOf(LocalDataSummary())
    }

    LaunchedEffect(Unit) {
        localDataSummary = vm.getLocalDataSummary()
    }

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val export = pendingExport
        pendingExport = null
        if (uri == null || export == null) {
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            runCatching {
                context.writeTextDocument(uri, export.content)
            }.onSuccess {
                message.success {
                    Text(export.successMessage)
                }
            }.onFailure {
                AppLogger.e("SettingPage", "Failed to write exported document", it)
                message.error {
                    Text(context.getString(R.string.setting_data_action_failed))
                }
            }
        }
    }

    val importSavedViewsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            runCatching {
                val content = context.readTextDocument(uri)
                vm.importSavedFeedViews(content)
            }.onSuccess { imported ->
                localDataSummary = vm.getLocalDataSummary()
                message.success {
                    Text(context.getString(R.string.setting_data_saved_views_import_success, imported))
                }
            }.onFailure {
                AppLogger.e("SettingPage", "Failed to import saved views", it)
                message.error {
                    Text(context.getString(R.string.setting_data_action_failed))
                }
            }
        }
    }

    val importLocalDataLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        coroutineScope.launch {
            runCatching {
                val content = context.readTextDocument(uri)
                vm.importLocalDataBackup(content)
            }.onSuccess { summary ->
                localDataSummary = summary
                message.success {
                    Text(
                        context.getString(
                            R.string.setting_data_local_backup_import_success,
                            summary.savedViewCount,
                            summary.historyCount,
                            summary.downloadCount,
                        )
                    )
                }
            }.onFailure {
                AppLogger.e("SettingPage", "Failed to import local data backup", it)
                message.error {
                    Text(context.getString(R.string.setting_data_action_failed))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(stringResource(R.string.setting_title))
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            item {
                SettingItemCategory(
                    title = {
                        Text(stringResource(R.string.setting_look))
                    }
                ) {
                    val darkMode = rememberIntPreference(
                        key = "setting.dark_mode",
                        default = 0
                    )
                    val workMode = rememberBooleanPreference(
                        key = "setting.work_mode",
                        default = false
                    )
                    val mediaListMode = rememberStringPreference(
                        key = SETTING_MEDIA_LIST_MODE,
                        default = MEDIA_LIST_MODE_DETAIL,
                    )
                    val dynamicColor = rememberBooleanPreference(
                        key = "setting.dynamic_color",
                        default = true
                    )
                    SettingPickerItem(
                        state = darkMode,
                        items = listOf(0, 1, 2),
                        itemLabel = { mode ->
                            Text(
                                text = when (mode) {
                                    0 -> stringResource(R.string.setting_look_darkmode_follow)
                                    1 -> stringResource(R.string.setting_look_darkmode_light_mode)
                                    2 -> stringResource(R.string.setting_look_darkmode_dark_mode)
                                    else -> "?"
                                }
                            )
                        },
                        title = {
                            Text(stringResource(R.string.setting_look_darkmode_title))
                        },
                        icon = {
                            Icon(Icons.Outlined.LightMode, "Light/Dark Mode")
                        },
                        text = {
                            Text(stringResource(R.string.setting_look_darkmode_text))
                        }
                    )

                    SettingBooleanItem(
                        state = dynamicColor,
                        title = {
                            Text(stringResource(R.string.setting_look_dynamic_color_title))
                        },
                        text = {
                            Text(stringResource(R.string.setting_look_dynamic_color_text))
                        },
                        icon = {
                            Icon(Icons.Outlined.ColorLens,  null)
                        }
                    )

                    SettingBooleanItem(
                        state = workMode,
                        title = {
                            Text(stringResource(R.string.setting_look_work_mode_title))
                        },
                        text = {
                            Text(stringResource(R.string.setting_look_work_mode_text))
                        },
                        icon = {
                            Icon(Icons.Outlined.HomeWork,  null)
                        }
                    )

                    SettingPickerItem(
                        state = mediaListMode,
                        items = listOf(MEDIA_LIST_MODE_DETAIL, MEDIA_LIST_MODE_THUMBNAIL),
                        itemLabel = { mode ->
                            Text(
                                text = when (mode) {
                                    MEDIA_LIST_MODE_DETAIL -> stringResource(R.string.media_list_mode_detail)
                                    MEDIA_LIST_MODE_THUMBNAIL -> stringResource(R.string.media_list_mode_thumbnail)
                                    else -> "?"
                                }
                            )
                        },
                        title = {
                            Text(stringResource(R.string.setting_look_media_list_mode_title))
                        },
                        icon = {
                            Icon(Icons.Outlined.ViewAgenda, null)
                        },
                        text = {
                            Text(stringResource(R.string.setting_look_media_list_mode_text))
                        }
                    )
                }
            }

            item {
                val autoPlay = rememberBooleanPreference(
                    key = "setting.auto_play",
                    default = true
                )
                val loopPlay = rememberBooleanPreference(
                    key = "setting.loop_play",
                    default = false
                )
                SettingItemCategory(title = { Text(stringResource(R.string.setting_player)) }) {
                    SettingBooleanItem(
                        state = autoPlay,
                        title = {
                            Text(stringResource(R.string.setting_player_auto_play_title))
                        },
                        text = {
                            Text(stringResource(R.string.setting_player_auto_play_text))
                        },
                        icon = {
                            Icon(Icons.Outlined.AutoMode,  null)
                        }
                    )

                    SettingBooleanItem(
                        state = loopPlay,
                        title = {
                            Text(stringResource(R.string.setting_player_loop_title))
                        },
                        text = {
                            Text(stringResource(R.string.setting_player_loop_text))
                        },
                        icon = {
                            Icon(Icons.Outlined.Replay,  null)
                        }
                    )
                }
            }

            item {
                val builtInDohEnabled = rememberBooleanPreference(
                    key = SETTING_NETWORK_DOH_ENABLED,
                    default = true
                )
                val builtInDohEndpoint = rememberStringPreference(
                    key = SETTING_NETWORK_DOH_ENDPOINT,
                    default = DEFAULT_NETWORK_DOH_ENDPOINT
                )
                val builtInDohUpstream = rememberStringPreference(
                    key = SETTING_NETWORK_DOH_UPSTREAM,
                    default = DEFAULT_NETWORK_DOH_UPSTREAM
                )

                SettingItemCategory(title = { Text(stringResource(R.string.setting_network)) }) {
                    SettingBooleanItem(
                        state = builtInDohEnabled,
                        title = {
                            Text(stringResource(R.string.setting_network_builtin_doh_title))
                        },
                        text = {
                            Text(stringResource(R.string.setting_network_builtin_doh_text))
                        },
                        icon = {
                            Icon(Icons.Outlined.Source, null)
                        }
                    )

                    SettingLinkItem(
                        title = {
                            Text(stringResource(R.string.setting_network_doh_endpoint_title))
                        },
                        text = {
                            Text(builtInDohEndpoint.value)
                        },
                        icon = {
                            Icon(Icons.Outlined.Source, null)
                        },
                        onClick = {
                            dialog.input(
                                title = {
                                    Text(stringResource(R.string.setting_network_doh_endpoint_title))
                                },
                                initialValue = builtInDohEndpoint.value,
                            ) { value ->
                                builtInDohEndpoint.value = value.trim().ifBlank {
                                    DEFAULT_NETWORK_DOH_ENDPOINT
                                }
                            }
                        }
                    )

                    SettingLinkItem(
                        title = {
                            Text(stringResource(R.string.setting_network_doh_upstream_title))
                        },
                        text = {
                            Text(builtInDohUpstream.value)
                        },
                        icon = {
                            Icon(Icons.Outlined.Replay, null)
                        },
                        onClick = {
                            dialog.input(
                                title = {
                                    Text(stringResource(R.string.setting_network_doh_upstream_title))
                                },
                                initialValue = builtInDohUpstream.value,
                            ) { value ->
                                builtInDohUpstream.value = value.trim().ifBlank {
                                    DEFAULT_NETWORK_DOH_UPSTREAM
                                }
                            }
                        }
                    )
                }
            }

            item {
                SettingItemCategory(title = { Text(stringResource(R.string.setting_data)) }) {
                    SettingLinkItem(
                        title = {
                            Text(stringResource(R.string.setting_data_saved_views_manage_title))
                        },
                        text = {
                            Text(stringResource(R.string.setting_data_saved_views_manage_text))
                        },
                        icon = {
                            Icon(Icons.Outlined.FeaturedPlayList, null)
                        },
                        onClick = {
                            router.navigate(savedFeedViewsRoute(FeedScope.HOME_VIDEO))
                        }
                    )

                    SettingLinkItem(
                        title = {
                            Text(stringResource(R.string.setting_data_local_backup_export_title))
                        },
                        text = {
                            Text(
                                stringResource(
                                    R.string.setting_data_local_backup_export_text,
                                    localDataSummary.savedViewCount,
                                    localDataSummary.historyCount,
                                    localDataSummary.downloadCount,
                                )
                            )
                        },
                        icon = {
                            Icon(Icons.Outlined.Source, null)
                        },
                        onClick = {
                            coroutineScope.launch {
                                runCatching {
                                    vm.exportLocalDataBackup()
                                }.onSuccess { content ->
                                    pendingExport = PendingExport(
                                        fileName = "awara-local-data-${System.currentTimeMillis()}.json",
                                        content = content,
                                        successMessage = context.getString(
                                            R.string.setting_data_local_backup_export_success,
                                        ),
                                    )
                                    exportDocumentLauncher.launch(pendingExport?.fileName)
                                }.onFailure {
                                    AppLogger.e("SettingPage", "Failed to prepare local data backup export", it)
                                    message.error {
                                        Text(context.getString(R.string.setting_data_action_failed))
                                    }
                                }
                            }
                        }
                    )

                    SettingLinkItem(
                        title = {
                            Text(stringResource(R.string.setting_data_local_backup_import_title))
                        },
                        text = {
                            Text(stringResource(R.string.setting_data_local_backup_import_text))
                        },
                        icon = {
                            Icon(Icons.Outlined.Replay, null)
                        },
                        onClick = {
                            importLocalDataLauncher.launch(arrayOf("application/json", "text/plain"))
                        }
                    )

                    SettingLinkItem(
                        title = {
                            Text(stringResource(R.string.setting_data_logs_export_title))
                        },
                        text = {
                            Text(stringResource(R.string.setting_data_logs_export_text))
                        },
                        icon = {
                            Icon(Icons.Outlined.Source, null)
                        },
                        onClick = {
                            coroutineScope.launch {
                                runCatching {
                                    vm.exportAppLogs()
                                }.onSuccess { content ->
                                    pendingExport = PendingExport(
                                        fileName = "awara-logs-${System.currentTimeMillis()}.json",
                                        content = content,
                                        successMessage = context.getString(R.string.setting_data_logs_export_success),
                                    )
                                    exportDocumentLauncher.launch(pendingExport?.fileName)
                                }.onFailure {
                                    AppLogger.e("SettingPage", "Failed to prepare app logs export", it)
                                    message.error {
                                        Text(context.getString(R.string.setting_data_action_failed))
                                    }
                                }
                            }
                        }
                    )

                    SettingLinkItem(
                        title = {
                            Text(stringResource(R.string.setting_data_saved_views_export_title))
                        },
                        text = {
                            Text(
                                stringResource(
                                    R.string.setting_data_saved_views_export_text,
                                    localDataSummary.savedViewCount,
                                )
                            )
                        },
                        icon = {
                            Icon(Icons.Outlined.Replay, null)
                        },
                        onClick = {
                            coroutineScope.launch {
                                runCatching {
                                    vm.exportSavedFeedViews()
                                }.onSuccess { content ->
                                    pendingExport = PendingExport(
                                        fileName = "awara-saved-views-${System.currentTimeMillis()}.json",
                                        content = content,
                                        successMessage = context.getString(R.string.setting_data_saved_views_export_success),
                                    )
                                    exportDocumentLauncher.launch(pendingExport?.fileName)
                                }.onFailure {
                                    AppLogger.e("SettingPage", "Failed to prepare saved views export", it)
                                    message.error {
                                        Text(context.getString(R.string.setting_data_action_failed))
                                    }
                                }
                            }
                        }
                    )

                    SettingLinkItem(
                        title = {
                            Text(stringResource(R.string.setting_data_saved_views_import_title))
                        },
                        text = {
                            Text(stringResource(R.string.setting_data_saved_views_import_text))
                        },
                        icon = {
                            Icon(Icons.Outlined.Replay, null)
                        },
                        onClick = {
                            importSavedViewsLauncher.launch(arrayOf("application/json", "text/plain"))
                        }
                    )
                }
            }

            item {
                SettingItemCategory(
                    title = {
                        Text(stringResource(R.string.setting_about))
                    }
                ) {
                    SettingLinkItem(
                        title = {
                            Text(stringResource(R.string.author_profile))
                        },
                        text = {
                            Text(stringResource(R.string.click_to_view_my_iwara_profile))
                        },
                        icon = {
                            Avatar(
                                model = "https://i.iwara.tv/image/avatar/a90cf846-fb84-4965-adbd-131c411abc93/picture-294150-1628430683.jpg",
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        onClick = {
                            router.navigate("user/user294150")
                        }
                    )

                    SettingLinkItem(
                        title = {
                            Text(stringResource(R.string.setting_about_source_title))
                        },
                        text = {
                            Text(stringResource(R.string.setting_about_source_text))
                        },
                        icon = {
                            Icon(Icons.Outlined.Source, null)
                        },
                        onClick = {
                            context.openUrl("https://github.com/re-ovo/awara")
                        }
                    )
                }
            }
        }
    }
}