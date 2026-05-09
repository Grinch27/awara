package me.rerere.awara.data.repo

// TODO(user): Keep local backup limited to safe local assets for now and do not include login tokens, cookies, or account passwords.
// TODO(agent): If settings backup grows beyond this whitelist, move the safe-key policy into a dedicated settings backup component with tests.

import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import me.rerere.awara.data.entity.DownloadItem
import me.rerere.awara.data.entity.DownloadType
import me.rerere.awara.data.entity.HistoryItem
import me.rerere.awara.data.entity.HistoryType
import me.rerere.awara.di.AppDatabase
import me.rerere.awara.domain.feed.SavedFeedView
import me.rerere.awara.util.DEFAULT_NETWORK_DOH_ENDPOINT
import me.rerere.awara.util.DEFAULT_NETWORK_DOH_UPSTREAM
import me.rerere.awara.util.InstantSerializer
import me.rerere.awara.util.JsonInstance
import me.rerere.awara.util.SETTING_NETWORK_ECH_ENABLED
import me.rerere.awara.util.SETTING_NETWORK_DOH_ENABLED
import me.rerere.awara.util.SETTING_NETWORK_DOH_ENDPOINT
import me.rerere.awara.util.SETTING_NETWORK_DOH_UPSTREAM
import me.rerere.compose_setting.preference.mmkvPreference

private const val SETTING_DARK_MODE = "setting.dark_mode"
private const val SETTING_WORK_MODE = "setting.work_mode"
private const val SETTING_DYNAMIC_COLOR = "setting.dynamic_color"
private const val SETTING_AUTO_PLAY = "setting.auto_play"
private const val SETTING_LOOP_PLAY = "setting.loop_play"
private const val SETTING_PLAYER_QUALITY = "setting.player_quality"

@Serializable
data class LocalDataBackupBundle(
    val version: Int = 2,
    @Serializable(with = InstantSerializer::class)
    val exportedAt: Instant = Instant.now(),
    val savedViews: List<SavedFeedView> = emptyList(),
    val historyItems: List<HistoryBackupItem> = emptyList(),
    val downloadItems: List<DownloadBackupItem> = emptyList(),
    val settings: SafeAppSettingsBackup = SafeAppSettingsBackup(),
)

data class LocalDataSummary(
    val savedViewCount: Int = 0,
    val historyCount: Int = 0,
    val downloadCount: Int = 0,
)

@Serializable
data class HistoryBackupItem(
    @Serializable(with = InstantSerializer::class)
    val time: Instant,
    val type: String,
    val resourceId: String,
    val title: String,
    val thumbnail: String,
)

@Serializable
data class DownloadBackupItem(
    @Serializable(with = InstantSerializer::class)
    val time: Instant,
    val title: String,
    val thumbnail: String,
    val path: String,
    val type: String,
    val resourceId: String,
)

@Serializable
data class SafeAppSettingsBackup(
    val darkMode: Int = 0,
    val workMode: Boolean = false,
    val dynamicColor: Boolean = true,
    val autoPlay: Boolean = true,
    val loopPlay: Boolean = false,
    val playerQuality: String = "",
    val builtInDohEnabled: Boolean = true,
    val builtInDohEndpoint: String = DEFAULT_NETWORK_DOH_ENDPOINT,
    val builtInDohUpstream: String = DEFAULT_NETWORK_DOH_UPSTREAM,
    val echEnabled: Boolean = false,
)

class LocalDataRepo(
    private val appDatabase: AppDatabase,
    private val savedFeedViewRepo: SavedFeedViewRepo,
) {
    suspend fun getSummary(): LocalDataSummary {
        return LocalDataSummary(
            savedViewCount = savedFeedViewRepo.count(),
            historyCount = appDatabase.historyDao().countHistory(),
            downloadCount = appDatabase.downloadDao().countDownloadedItems(),
        )
    }

    suspend fun exportBackupJson(): String {
        val bundle = LocalDataBackupBundle(
            savedViews = savedFeedViewRepo.getAll(),
            historyItems = appDatabase.historyDao().getAllHistory().map(HistoryItem::toBackupItem),
            downloadItems = appDatabase.downloadDao().getAllDownloadedItems().map(DownloadItem::toBackupItem),
            settings = SafeAppSettingsBackup.fromPreferences(),
        )
        return JsonInstance.encodeToString(bundle)
    }

    suspend fun importBackupJson(content: String): LocalDataSummary {
        val bundle = JsonInstance.decodeFromString<LocalDataBackupBundle>(content)
        savedFeedViewRepo.replaceAll(bundle.savedViews)
        appDatabase.historyDao().replaceAllHistory(
            bundle.historyItems.mapNotNull(HistoryBackupItem::toEntity),
        )
        appDatabase.downloadDao().replaceAllDownloadedItems(
            bundle.downloadItems.mapNotNull(DownloadBackupItem::toEntity),
        )
        bundle.settings.applyToPreferences()
        return getSummary()
    }
}

private fun HistoryItem.toBackupItem(): HistoryBackupItem {
    return HistoryBackupItem(
        time = time,
        type = type.name,
        resourceId = resourceId,
        title = title,
        thumbnail = thumbnail,
    )
}

private fun HistoryBackupItem.toEntity(): HistoryItem? {
    val historyType = runCatching { HistoryType.valueOf(type) }.getOrNull() ?: return null
    return HistoryItem(
        time = time,
        type = historyType,
        resourceId = resourceId,
        title = title,
        thumbnail = thumbnail,
    )
}

private fun DownloadItem.toBackupItem(): DownloadBackupItem {
    return DownloadBackupItem(
        time = time,
        title = title,
        thumbnail = thumbnail,
        path = path,
        type = type.name,
        resourceId = resourceId,
    )
}

private fun DownloadBackupItem.toEntity(): DownloadItem? {
    val downloadType = runCatching { DownloadType.valueOf(type) }.getOrNull() ?: return null
    return DownloadItem(
        title = title,
        thumbnail = thumbnail,
        path = path,
        type = downloadType,
        resourceId = resourceId,
        time = time,
    )
}

private fun SafeAppSettingsBackup.applyToPreferences() {
    mmkvPreference.putInt(SETTING_DARK_MODE, darkMode)
    mmkvPreference.putBoolean(SETTING_WORK_MODE, workMode)
    mmkvPreference.putBoolean(SETTING_DYNAMIC_COLOR, dynamicColor)
    mmkvPreference.putBoolean(SETTING_AUTO_PLAY, autoPlay)
    mmkvPreference.putBoolean(SETTING_LOOP_PLAY, loopPlay)
    mmkvPreference.putString(SETTING_PLAYER_QUALITY, playerQuality)
    mmkvPreference.putBoolean(SETTING_NETWORK_DOH_ENABLED, builtInDohEnabled)
    mmkvPreference.putString(SETTING_NETWORK_DOH_ENDPOINT, builtInDohEndpoint)
    mmkvPreference.putString(SETTING_NETWORK_DOH_UPSTREAM, builtInDohUpstream)
    mmkvPreference.putBoolean(SETTING_NETWORK_ECH_ENABLED, echEnabled)
}

private fun SafeAppSettingsBackup.Companion.fromPreferences(): SafeAppSettingsBackup {
    return SafeAppSettingsBackup(
        darkMode = mmkvPreference.getInt(SETTING_DARK_MODE, 0),
        workMode = mmkvPreference.getBoolean(SETTING_WORK_MODE, false),
        dynamicColor = mmkvPreference.getBoolean(SETTING_DYNAMIC_COLOR, true),
        autoPlay = mmkvPreference.getBoolean(SETTING_AUTO_PLAY, true),
        loopPlay = mmkvPreference.getBoolean(SETTING_LOOP_PLAY, false),
        playerQuality = mmkvPreference.getString(SETTING_PLAYER_QUALITY, "") ?: "",
        builtInDohEnabled = mmkvPreference.getBoolean(SETTING_NETWORK_DOH_ENABLED, true),
        builtInDohEndpoint = mmkvPreference.getString(
            SETTING_NETWORK_DOH_ENDPOINT,
            DEFAULT_NETWORK_DOH_ENDPOINT,
        ) ?: DEFAULT_NETWORK_DOH_ENDPOINT,
        builtInDohUpstream = mmkvPreference.getString(
            SETTING_NETWORK_DOH_UPSTREAM,
            DEFAULT_NETWORK_DOH_UPSTREAM,
        ) ?: DEFAULT_NETWORK_DOH_UPSTREAM,
        echEnabled = mmkvPreference.getBoolean(SETTING_NETWORK_ECH_ENABLED, false),
    )
}