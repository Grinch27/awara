package me.rerere.awara.data.repo

// TODO(user): Keep local backup limited to safe local assets for now and do not include login tokens, cookies, or account passwords.
// TODO(agent): If settings backup grows beyond this whitelist, move the safe-key policy into a dedicated settings backup component with tests.

import java.time.Instant
import java.util.UUID
import java.io.File
import android.content.Context
import android.util.AtomicFile
import androidx.room.withTransaction
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import me.rerere.awara.data.entity.ImportCommit
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import me.rerere.awara.data.entity.DownloadItem
import me.rerere.awara.data.entity.DownloadType
import me.rerere.awara.data.entity.HistoryItem
import me.rerere.awara.data.entity.HistoryType
import me.rerere.awara.di.AppDatabase
import me.rerere.awara.util.DEFAULT_NETWORK_DOH_ENDPOINT
import me.rerere.awara.util.DEFAULT_NETWORK_DOH_UPSTREAM
import me.rerere.awara.util.InstantSerializer
import me.rerere.awara.util.JsonInstance
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
    val version: Int = 3,
    val format: String = "awara.local-data",
    @Serializable(with = InstantSerializer::class)
    val exportedAt: Instant = Instant.now(),
    val historyItems: List<HistoryBackupItem> = emptyList(),
    val downloadItems: List<DownloadBackupItem> = emptyList(),
    val settings: SafeAppSettingsBackup = SafeAppSettingsBackup(),
)

data class LocalDataSummary(
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
)

// Requirements: bounded strict parsing, explicit merge/replace and recoverable cross-store commit.
// Risks: files referenced by metadata are not embedded; imported paths never authorize deletion.
// Verify: malformed/empty/unsupported documents, rollback and interrupted-import recovery.
object LocalDataBackupCodec {
    const val MAX_BYTES = 16 * 1024 * 1024
    val json = Json { encodeDefaults = true; ignoreUnknownKeys = false; coerceInputValues = false }

    fun decode(content: String): LocalDataBackupBundle {
        require(content.length <= MAX_BYTES && content.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup exceeds 16 MiB" }
        val root = json.parseToJsonElement(content) as? JsonObject ?: error("Expected backup object")
        require(root.containsKey("exportedAt")) { "Missing backup timestamp" }
        val version = root["version"]?.jsonPrimitive?.intOrNull ?: if (root.containsKey("version")) error("Invalid version") else 2
        require(version in 2..3) { "Unsupported backup version" }
        if (version == 3) {
            require(root["format"]?.jsonPrimitive?.contentOrNull == "awara.local-data") { "Invalid backup format" }
            require(listOf("historyItems", "downloadItems", "settings").all(root::containsKey)) { "Incomplete backup" }
        }
        val bundle = json.decodeFromJsonElement<LocalDataBackupBundle>(root)
        require(bundle.historyItems.size + bundle.downloadItems.size <= 50000) { "Too many records" }
        fun text(value: String, max: Int = 4096) { require(value.length <= max && !value.contains('\u0000')) { "Invalid field" } }
        fun id(value: String) { require(value.isNotBlank()); text(value, 256) }
        bundle.historyItems.forEach {
            HistoryType.valueOf(it.type); id(it.resourceId); text(it.title); text(it.thumbnail, 16384)
        }
        bundle.downloadItems.forEach {
            DownloadType.valueOf(it.type); id(it.resourceId); text(it.title); text(it.thumbnail, 16384); text(it.path)
        }
        require(bundle.settings.darkMode in 0..2)
        text(bundle.settings.playerQuality, 128)
        text(bundle.settings.builtInDohEndpoint, 4096)
        text(bundle.settings.builtInDohUpstream, 4096)
        require(runCatching {
            me.rerere.awara.util.strictDohUrl(bundle.settings.builtInDohEndpoint, "Backup DoH endpoint")
            me.rerere.awara.util.strictDohUrl(bundle.settings.builtInDohUpstream, "Backup DoH upstream")
        }.isSuccess) { "Invalid DNS settings" }
        return bundle
    }
}

@Serializable
private data class ImportJournal(val id: String, val previous: SafeAppSettingsBackup, val next: SafeAppSettingsBackup)

class LocalDataRepo(
    private val appDatabase: AppDatabase,
    context: Context,
) {
    private val mutex = Mutex()
    private val journal = AtomicFile(File(context.noBackupFilesDir, "local-data-import.json"))
    fun needsRecovery(): Boolean = journal.baseFile.exists() || File(journal.baseFile.path + ".bak").exists()

    suspend fun recoverInterruptedImport() = withContext(Dispatchers.IO) { mutex.withLock { recover() } }

    private suspend fun recover() {
        if (!needsRecovery()) return
        val entry = journal.openRead().bufferedReader().use { LocalDataBackupCodec.json.decodeFromString<ImportJournal>(it.readText()) }
        if (appDatabase.transferDao().importCommitted(entry.id) > 0) entry.next.applyToPreferences() else entry.previous.applyToPreferences()
        journal.delete()
        appDatabase.transferDao().forgetImport(entry.id)
    }

    suspend fun getSummary(): LocalDataSummary = LocalDataSummary(
        historyCount = appDatabase.historyDao().countHistory(),
        downloadCount = appDatabase.downloadDao().countDownloadedItems(),
    )

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) { mutex.withLock {
        recover()
        val bundle = LocalDataBackupBundle(
            historyItems = appDatabase.historyDao().getAllHistory().map(HistoryItem::toBackupItem),
            downloadItems = appDatabase.downloadDao().getAllDownloadedItems().map(DownloadItem::toBackupItem),
            settings = SafeAppSettingsBackup.fromPreferences(),
        )
        LocalDataBackupCodec.json.encodeToString(bundle)
    } }

    suspend fun previewBackup(content: String): LocalDataSummary = withContext(Dispatchers.IO) {
        val bundle = LocalDataBackupCodec.decode(content)
        LocalDataSummary(bundle.historyItems.size, bundle.downloadItems.size)
    }

    suspend fun importBackupJson(content: String, replace: Boolean = false): LocalDataSummary = withContext(Dispatchers.IO) { mutex.withLock {
        recover()
        val bundle = LocalDataBackupCodec.decode(content)
        val previous = SafeAppSettingsBackup.fromPreferences()
        // Merge adds records and preserves current settings. Replacement also restores safe settings.
        val entry = ImportJournal(UUID.randomUUID().toString(), previous, if (replace) bundle.settings else previous)
        val stream = journal.startWrite()
        try {
            stream.write(LocalDataBackupCodec.json.encodeToString(entry).toByteArray(Charsets.UTF_8))
            journal.finishWrite(stream)
        } catch (error: Exception) { journal.failWrite(stream); throw error }
        try {
            appDatabase.withTransaction {
                val history = (bundle.historyItems.map(HistoryBackupItem::toEntity) +
                    if (replace) emptyList() else appDatabase.historyDao().getAllHistory())
                    .sortedByDescending { it.time }.distinctBy { it.type to it.resourceId }.map { it.copy(id = 0L) }
                val downloads = (bundle.downloadItems.map(DownloadBackupItem::toEntity) +
                    if (replace) emptyList() else appDatabase.downloadDao().getAllDownloadedItems())
                    .sortedByDescending { it.time }.distinctBy { it.type to it.resourceId }.map { it.copy(id = 0) }
                appDatabase.historyDao().replaceAllHistory(history)
                appDatabase.downloadDao().replaceAllDownloadedItems(downloads)
                entry.next.applyToPreferences()
                appDatabase.transferDao().markImport(ImportCommit(entry.id))
            }
            journal.delete()
            appDatabase.transferDao().forgetImport(entry.id)
        } catch (error: Throwable) {
            withContext(NonCancellable) { recover() }
            throw error
        }
        getSummary()
    } }
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

private fun HistoryBackupItem.toEntity(): HistoryItem {
    val historyType = HistoryType.valueOf(type)
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

private fun DownloadBackupItem.toEntity(): DownloadItem {
    val downloadType = DownloadType.valueOf(type)
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
    )
}
