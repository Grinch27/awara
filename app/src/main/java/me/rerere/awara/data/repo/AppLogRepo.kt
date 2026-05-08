package me.rerere.awara.data.repo

// TODO(user): Decide whether exported diagnostics should include only recent logs or also an opt-in full dump.
// TODO(agent): If log retention starts competing with user data, move the cap and purge policy into a dedicated diagnostics settings surface.

import kotlinx.serialization.encodeToString
import me.rerere.awara.data.entity.AppLogEntry
import me.rerere.awara.data.entity.AppLogExportBundle
import me.rerere.awara.data.entity.AppLogLevel
import me.rerere.awara.di.AppDatabase
import me.rerere.awara.util.JsonInstance

class AppLogRepo(
    private val appDatabase: AppDatabase,
) {
    companion object {
        const val DEFAULT_LOG_LIMIT = 10_000
    }

    suspend fun append(
        level: AppLogLevel,
        tag: String,
        message: String,
        throwable: String? = null,
    ) {
        appDatabase.appLogDao().append(
            entry = AppLogEntry(
                level = level,
                tag = tag,
                message = message,
                throwable = throwable,
            ),
            limit = DEFAULT_LOG_LIMIT,
        )
    }

    suspend fun count(): Int = appDatabase.appLogDao().count()

    suspend fun exportJson(limit: Int = DEFAULT_LOG_LIMIT): String {
        val logs = appDatabase.appLogDao().getLatest(limit)
        return JsonInstance.encodeToString(AppLogExportBundle(logs = logs))
    }
}