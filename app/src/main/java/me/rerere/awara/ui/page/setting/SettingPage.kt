package me.rerere.awara.ui.page.setting

// TODO(user): Decide whether subscription should return as an optional default entry after video/image/forum behavior is fully validated.
// TODO(agent): Keep this page flat and searchable; avoid hidden nested routes for core navigation preferences.

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.rerere.awara.R
import me.rerere.awara.data.repo.LocalDataSummary
import me.rerere.awara.ui.LocalDialogProvider
import me.rerere.awara.ui.LocalMessageProvider
import me.rerere.awara.ui.LocalRouterProvider
import me.rerere.awara.ui.component.common.Avatar
import me.rerere.awara.ui.component.common.BackButton
import me.rerere.awara.ui.component.common.DialogHolder
import me.rerere.awara.ui.component.common.MessageHolder
import me.rerere.awara.ui.component.iwara.MEDIA_LIST_MODE_DETAIL
import me.rerere.awara.ui.component.iwara.MEDIA_LIST_MODE_THUMBNAIL
import me.rerere.awara.ui.component.iwara.SETTING_MEDIA_LIST_MODE
import me.rerere.awara.ui.component.iwara.rememberBlockMediaThumbnailsPreference
import me.rerere.awara.ui.component.iwara.param.rating.DEFAULT_MEDIA_RATING
import me.rerere.awara.ui.component.iwara.param.rating.MediaRatingKeys
import me.rerere.awara.ui.component.iwara.param.rating.SETTING_MEDIA_SEARCH_RATING
import me.rerere.awara.ui.page.index.SETTING_HOME_DEFAULT_SECTION
import me.rerere.awara.util.AppLogger
import me.rerere.awara.util.DEFAULT_NETWORK_DOH_ENDPOINT
import me.rerere.awara.util.DEFAULT_NETWORK_DOH_UPSTREAM
import me.rerere.awara.util.SETTING_NETWORK_DOH_ENABLED
import me.rerere.awara.util.SETTING_NETWORK_DOH_ENDPOINT
import me.rerere.awara.util.SETTING_NETWORK_DOH_UPSTREAM
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

private enum class SettingSection(
    val titleRes: Int,
    val summaryRes: Int,
    val icon: ImageVector,
) {
    SITE(
        titleRes = R.string.setting_site,
        summaryRes = R.string.setting_site_summary,
        icon = Icons.Outlined.Source,
    ),
    APPEARANCE(
        titleRes = R.string.setting_look,
        summaryRes = R.string.setting_appearance_summary,
        icon = Icons.Outlined.ColorLens,
    ),
    ADVANCED(
        titleRes = R.string.setting_advanced,
        summaryRes = R.string.setting_advanced_summary,
        icon = Icons.Outlined.Settings,
    ),
    ABOUT(
        titleRes = R.string.setting_about,
        summaryRes = R.string.setting_about_summary,
        icon = Icons.Outlined.Info,
    ),
}

private fun matchesSettingQuery(context: Context, query: String, vararg textRes: Int): Boolean {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isBlank()) {
        return true
    }
    return textRes.any { resId ->
        context.getString(resId).contains(normalizedQuery, ignoreCase = true)
    }
}

private fun siteSectionMatches(context: Context, query: String): Boolean = matchesSettingQuery(
    context,
    query,
    SettingSection.SITE.titleRes,
    SettingSection.SITE.summaryRes,
    R.string.setting_network_builtin_doh_title,
    R.string.setting_network_builtin_doh_text,
    R.string.setting_network_doh_endpoint_title,
    R.string.setting_network_doh_upstream_title,
)

private fun appearanceSectionMatches(context: Context, query: String): Boolean = matchesSettingQuery(
    context,
    query,
    SettingSection.APPEARANCE.titleRes,
    SettingSection.APPEARANCE.summaryRes,
    R.string.setting_look_darkmode_title,
    R.string.setting_look_darkmode_text,
    R.string.setting_look_dynamic_color_title,
    R.string.setting_look_dynamic_color_text,
    R.string.setting_look_work_mode_title,
    R.string.setting_look_work_mode_text,
    R.string.setting_look_media_list_mode_title,
    R.string.setting_look_media_list_mode_text,
    R.string.setting_look_search_rating_title,
    R.string.setting_look_search_rating_text,
    R.string.setting_look_home_default_title,
    R.string.setting_look_home_default_text,
    R.string.setting_player,
    R.string.setting_player_auto_play_title,
    R.string.setting_player_auto_play_text,
    R.string.setting_player_loop_title,
    R.string.setting_player_loop_text,
)

private fun advancedSectionMatches(context: Context, query: String): Boolean = matchesSettingQuery(
    context,
    query,
    SettingSection.ADVANCED.titleRes,
    SettingSection.ADVANCED.summaryRes,
    R.string.setting_data_local_backup_export_title,
    R.string.setting_data_local_backup_export_text,
    R.string.setting_data_local_backup_import_title,
    R.string.setting_data_local_backup_import_text,
    R.string.setting_data_logs_export_title,
    R.string.setting_data_logs_export_text,
)

private fun aboutSectionMatches(context: Context, query: String): Boolean = matchesSettingQuery(
    context,
    query,
    SettingSection.ABOUT.titleRes,
    SettingSection.ABOUT.summaryRes,
    R.string.author_profile,
    R.string.click_to_view_my_iwara_profile,
    R.string.setting_about_source_title,
    R.string.setting_about_source_text,
)

@Composable
fun SettingPage(vm: SettingVM = koinViewModel()) {
    val context = LocalContext.current
    val dialog = LocalDialogProvider.current
    val message = LocalMessageProvider.current
    val router = LocalRouterProvider.current
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }
    var pendingExport by remember {
        mutableStateOf<PendingExport?>(null)
    }
    var localDataSummary by remember {
        mutableStateOf(LocalDataSummary())
    }

    LaunchedEffect(Unit) {
        localDataSummary = vm.getLocalDataSummary()
    }

    val siteVisible = siteSectionMatches(context, searchQuery)
    val appearanceVisible = appearanceSectionMatches(context, searchQuery)
    val advancedVisible = advancedSectionMatches(context, searchQuery)
    val aboutVisible = aboutSectionMatches(context, searchQuery)
    val hasVisibleSections = siteVisible || appearanceVisible || advancedVisible || aboutVisible

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
                    BackButton(onClick = { router.popBackStack() })
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Close, null)
                            }
                        }
                    },
                    label = {
                        Text(stringResource(R.string.setting_search_placeholder))
                    },
                    placeholder = {
                        Text(stringResource(R.string.setting_search_placeholder))
                    },
                )
            }

            if (siteVisible) {
                item {
                    SiteSettingsSection(
                        query = searchQuery,
                        dialog = dialog,
                        context = context,
                    )
                }
            }

            if (appearanceVisible) {
                item {
                    AppearanceSettingsSection(
                        query = searchQuery,
                        context = context,
                    )
                }
                item {
                    PlayerSettingsSection(
                        query = searchQuery,
                        context = context,
                    )
                }
            }

            if (advancedVisible) {
                item {
                    AdvancedSettingsSection(
                        query = searchQuery,
                        context = context,
                        coroutineScope = coroutineScope,
                        message = message,
                        localDataSummary = localDataSummary,
                        vm = vm,
                        onExportPrepared = { export ->
                            pendingExport = export
                            exportDocumentLauncher.launch(export.fileName)
                        },
                        onImportClick = {
                            importLocalDataLauncher.launch(arrayOf("application/json", "text/plain"))
                        },
                    )
                }
            }

            if (aboutVisible) {
                item {
                    AboutSettingsSection(
                        query = searchQuery,
                        context = context,
                        router = router,
                    )
                }
            }

            if (!hasVisibleSections) {
                item {
                    Text(
                        text = stringResource(R.string.setting_search_empty),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SiteSettingsSection(
    query: String,
    dialog: DialogHolder,
    context: Context,
) {
    val showWholeSection = matchesSettingQuery(
        context,
        query,
        SettingSection.SITE.titleRes,
        SettingSection.SITE.summaryRes,
    )
    val builtInDohEnabled = rememberBooleanPreference(
        key = SETTING_NETWORK_DOH_ENABLED,
        default = true,
    )
    val builtInDohEndpoint = rememberStringPreference(
        key = SETTING_NETWORK_DOH_ENDPOINT,
        default = DEFAULT_NETWORK_DOH_ENDPOINT,
    )
    val builtInDohUpstream = rememberStringPreference(
        key = SETTING_NETWORK_DOH_UPSTREAM,
        default = DEFAULT_NETWORK_DOH_UPSTREAM,
    )
    val showBuiltinDoh = showWholeSection || matchesSettingQuery(
        context,
        query,
        R.string.setting_network_builtin_doh_title,
        R.string.setting_network_builtin_doh_text,
    )
    val showDohEndpoint = showWholeSection || matchesSettingQuery(
        context,
        query,
        R.string.setting_network_doh_endpoint_title,
    )
    val showDohUpstream = showWholeSection || matchesSettingQuery(
        context,
        query,
        R.string.setting_network_doh_upstream_title,
    )

    SettingItemCategory(title = { Text(stringResource(R.string.setting_site)) }) {
        if (showBuiltinDoh) {
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
                },
            )
        }

        if (showDohEndpoint) {
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
                },
            )
        }

        if (showDohUpstream) {
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
                },
            )
        }
    }
}

@Composable
private fun AppearanceSettingsSection(
    query: String,
    context: Context,
) {
    val showWholeSection = matchesSettingQuery(
        context,
        query,
        SettingSection.APPEARANCE.titleRes,
        SettingSection.APPEARANCE.summaryRes,
    )
    val darkMode = rememberIntPreference(
        key = "setting.dark_mode",
        default = 0,
    )
    val workMode = rememberBooleanPreference(
        key = "setting.work_mode",
        default = false,
    )
    val mediaListMode = rememberStringPreference(
        key = SETTING_MEDIA_LIST_MODE,
        default = MEDIA_LIST_MODE_DETAIL,
    )
    val blockMediaThumbnails = rememberBlockMediaThumbnailsPreference()
    val mediaSearchRating = rememberStringPreference(
        key = SETTING_MEDIA_SEARCH_RATING,
        default = DEFAULT_MEDIA_RATING,
    )
    val homeDefaultSection = rememberStringPreference(
        key = SETTING_HOME_DEFAULT_SECTION,
        default = "video",
    )
    val dynamicColor = rememberBooleanPreference(
        key = "setting.dynamic_color",
        default = true,
    )
    val showDarkMode = showWholeSection || matchesSettingQuery(
        context,
        query,
        R.string.setting_look_darkmode_title,
        R.string.setting_look_darkmode_text,
    )
    val showDynamicColor = showWholeSection || matchesSettingQuery(
        context,
        query,
        R.string.setting_look_dynamic_color_title,
        R.string.setting_look_dynamic_color_text,
    )
    val showWorkMode = showWholeSection || matchesSettingQuery(
        context,
        query,
        R.string.setting_look_work_mode_title,
        R.string.setting_look_work_mode_text,
    )
    val showListMode = showWholeSection || matchesSettingQuery(
        context,
        query,
        R.string.setting_look_media_list_mode_title,
        R.string.setting_look_media_list_mode_text,
    )
    val showBlockMediaThumbnails = showWholeSection || matchesSettingQuery(
        context,
        query,
        R.string.setting_look_block_media_thumbnails_title,
        R.string.setting_look_block_media_thumbnails_text,
    )
    val showSearchRating = showWholeSection || matchesSettingQuery(
        context,
        query,
        R.string.setting_look_search_rating_title,
        R.string.setting_look_search_rating_text,
    )
    val showHomeDefault = showWholeSection || matchesSettingQuery(
        context,
        query,
        R.string.setting_look_home_default_title,
        R.string.setting_look_home_default_text,
    )

    SettingItemCategory(title = { Text(stringResource(R.string.setting_look)) }) {
        if (showDarkMode) {
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
                        },
                    )
                },
                title = {
                    Text(stringResource(R.string.setting_look_darkmode_title))
                },
                icon = {
                    Icon(Icons.Outlined.LightMode, null)
                },
                text = {
                    Text(stringResource(R.string.setting_look_darkmode_text))
                },
            )
        }

        if (showDynamicColor) {
            SettingBooleanItem(
                state = dynamicColor,
                title = {
                    Text(stringResource(R.string.setting_look_dynamic_color_title))
                },
                text = {
                    Text(stringResource(R.string.setting_look_dynamic_color_text))
                },
                icon = {
                    Icon(Icons.Outlined.ColorLens, null)
                },
            )
        }

        if (showWorkMode) {
            SettingBooleanItem(
                state = workMode,
                title = {
                    Text(stringResource(R.string.setting_look_work_mode_title))
                },
                text = {
                    Text(stringResource(R.string.setting_look_work_mode_text))
                },
                icon = {
                    Icon(Icons.Outlined.HomeWork, null)
                },
            )
        }

        if (showListMode) {
            SettingPickerItem(
                state = mediaListMode,
                items = listOf(MEDIA_LIST_MODE_DETAIL, MEDIA_LIST_MODE_THUMBNAIL),
                itemLabel = { mode ->
                    Text(
                        text = when (mode) {
                            MEDIA_LIST_MODE_DETAIL -> stringResource(R.string.media_list_mode_detail)
                            MEDIA_LIST_MODE_THUMBNAIL -> stringResource(R.string.media_list_mode_thumbnail)
                            else -> "?"
                        },
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
                },
            )
        }

        if (showBlockMediaThumbnails) {
            SettingBooleanItem(
                state = blockMediaThumbnails,
                title = {
                    Text(stringResource(R.string.setting_look_block_media_thumbnails_title))
                },
                icon = {
                    Icon(Icons.Outlined.Image, null)
                },
                text = {
                    Text(stringResource(R.string.setting_look_block_media_thumbnails_text))
                },
            )
        }

        if (showSearchRating) {
            SettingPickerItem(
                state = mediaSearchRating,
                items = MediaRatingKeys,
                itemLabel = { rating ->
                    Text(
                        text = when (rating) {
                            "ecchi" -> stringResource(R.string.rating_ecchi)
                            "general" -> stringResource(R.string.rating_general)
                            else -> stringResource(R.string.rating_all)
                        },
                    )
                },
                title = {
                    Text(stringResource(R.string.setting_look_search_rating_title))
                },
                icon = {
                    Icon(Icons.Outlined.Star, null)
                },
                text = {
                    Text(stringResource(R.string.setting_look_search_rating_text))
                },
            )
        }

        if (showHomeDefault) {
            SettingPickerItem(
                state = homeDefaultSection,
                items = listOf("subscription", "video", "image", "forum"),
                itemLabel = { section ->
                    Text(
                        text = when (section) {
                            "subscription" -> stringResource(R.string.index_nav_subscription)
                            "video" -> stringResource(R.string.index_nav_video)
                            "image" -> stringResource(R.string.index_nav_image)
                            "forum" -> stringResource(R.string.index_nav_forum)
                            else -> section
                        },
                    )
                },
                title = {
                    Text(stringResource(R.string.setting_look_home_default_title))
                },
                icon = {
                    Icon(Icons.Outlined.ViewAgenda, null)
                },
                text = {
                    Text(stringResource(R.string.setting_look_home_default_text))
                },
            )
        }
    }
}

@Composable
private fun PlayerSettingsSection(
    query: String,
    context: Context,
) {
    val showWholeSection = matchesSettingQuery(
        context,
        query,
        SettingSection.APPEARANCE.titleRes,
        SettingSection.APPEARANCE.summaryRes,
        R.string.setting_player,
    )
    val autoPlay = rememberBooleanPreference(
        key = "setting.auto_play",
        default = true,
    )
    val loopPlay = rememberBooleanPreference(
        key = "setting.loop_play",
        default = false,
    )
    val showAutoPlay = showWholeSection || matchesSettingQuery(
        context,
        query,
        R.string.setting_player_auto_play_title,
        R.string.setting_player_auto_play_text,
    )
    val showLoopPlay = showWholeSection || matchesSettingQuery(
        context,
        query,
        R.string.setting_player_loop_title,
        R.string.setting_player_loop_text,
    )

    SettingItemCategory(title = { Text(stringResource(R.string.setting_player)) }) {
        if (showAutoPlay) {
            SettingBooleanItem(
                state = autoPlay,
                title = {
                    Text(stringResource(R.string.setting_player_auto_play_title))
                },
                text = {
                    Text(stringResource(R.string.setting_player_auto_play_text))
                },
                icon = {
                    Icon(Icons.Outlined.AutoMode, null)
                },
            )
        }

        if (showLoopPlay) {
            SettingBooleanItem(
                state = loopPlay,
                title = {
                    Text(stringResource(R.string.setting_player_loop_title))
                },
                text = {
                    Text(stringResource(R.string.setting_player_loop_text))
                },
                icon = {
                    Icon(Icons.Outlined.Replay, null)
                },
            )
        }
    }
}

@Composable
private fun AdvancedSettingsSection(
    query: String,
    context: Context,
    coroutineScope: CoroutineScope,
    message: MessageHolder,
    localDataSummary: LocalDataSummary,
    vm: SettingVM,
    onExportPrepared: (PendingExport) -> Unit,
    onImportClick: () -> Unit,
) {
    val showWholeSection = matchesSettingQuery(
        context,
        query,
        SettingSection.ADVANCED.titleRes,
        SettingSection.ADVANCED.summaryRes,
    )
    val showLocalExport = showWholeSection || matchesSettingQuery(
        context,
        query,
        R.string.setting_data_local_backup_export_title,
        R.string.setting_data_local_backup_export_text,
    )
    val showLocalImport = showWholeSection || matchesSettingQuery(
        context,
        query,
        R.string.setting_data_local_backup_import_title,
        R.string.setting_data_local_backup_import_text,
    )
    val showLogsExport = showWholeSection || matchesSettingQuery(
        context,
        query,
        R.string.setting_data_logs_export_title,
        R.string.setting_data_logs_export_text,
    )

    SettingItemCategory(title = { Text(stringResource(R.string.setting_advanced)) }) {
        if (showLocalExport) {
            SettingLinkItem(
                title = {
                    Text(stringResource(R.string.setting_data_local_backup_export_title))
                },
                text = {
                    Text(
                        stringResource(
                            R.string.setting_data_local_backup_export_text,
                            localDataSummary.historyCount,
                            localDataSummary.downloadCount,
                        ),
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
                            onExportPrepared(
                                PendingExport(
                                    fileName = "awara-local-data-${System.currentTimeMillis()}.json",
                                    content = content,
                                    successMessage = context.getString(
                                        R.string.setting_data_local_backup_export_success,
                                    ),
                                ),
                            )
                        }.onFailure {
                            AppLogger.e("SettingPage", "Failed to prepare local data backup export", it)
                            message.error {
                                Text(context.getString(R.string.setting_data_action_failed))
                            }
                        }
                    }
                },
            )
        }

        if (showLocalImport) {
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
                onClick = onImportClick,
            )
        }

        if (showLogsExport) {
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
                            onExportPrepared(
                                PendingExport(
                                    fileName = "awara-logs-${System.currentTimeMillis()}.json",
                                    content = content,
                                    successMessage = context.getString(R.string.setting_data_logs_export_success),
                                ),
                            )
                        }.onFailure {
                            AppLogger.e("SettingPage", "Failed to prepare app logs export", it)
                            message.error {
                                Text(context.getString(R.string.setting_data_action_failed))
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun AboutSettingsSection(
    query: String,
    context: Context,
    router: NavController,
) {
    val showWholeSection = matchesSettingQuery(
        context,
        query,
        SettingSection.ABOUT.titleRes,
        SettingSection.ABOUT.summaryRes,
    )
    val showAuthorProfile = showWholeSection || matchesSettingQuery(
        context,
        query,
        R.string.author_profile,
        R.string.click_to_view_my_iwara_profile,
    )
    val showSourceCode = showWholeSection || matchesSettingQuery(
        context,
        query,
        R.string.setting_about_source_title,
        R.string.setting_about_source_text,
    )

    SettingItemCategory(title = { Text(stringResource(R.string.setting_about)) }) {
        if (showAuthorProfile) {
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
                        modifier = Modifier.size(32.dp),
                    )
                },
                onClick = {
                    router.navigate("user/${Uri.encode("user294150")}")
                },
            )
        }

        if (showSourceCode) {
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
                },
            )
        }
    }
}
