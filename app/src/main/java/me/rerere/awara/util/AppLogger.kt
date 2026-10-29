package me.rerere.awara.util

// TODO(user): Decide whether network request summaries should remain always on or become configurable in settings.
// TODO(agent): If more subsystems need structured diagnostics, move from plain text messages to a typed event payload instead of overloading message strings.

import android.util.Log
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.rerere.awara.data.entity.AppLogLevel
import me.rerere.awara.data.repo.AppLogRepo
import okhttp3.HttpUrl
import okhttp3.Interceptor

private const val NETWORK_TAG = "Network"
private const val MAX_MESSAGE_LENGTH = 512
private const val MAX_THROWABLE_LENGTH = 4_000

object AppLogger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var appLogRepo: AppLogRepo? = null

    fun install(repo: AppLogRepo) {
        appLogRepo = repo
    }

    fun d(tag: String, message: String) {
        write(AppLogLevel.DEBUG, tag, message, null)
    }

    fun i(tag: String, message: String) {
        write(AppLogLevel.INFO, tag, message, null)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        write(AppLogLevel.WARNING, tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        write(AppLogLevel.ERROR, tag, message, throwable)
    }

    fun createSafeNetworkLogInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val summary = "${request.method} ${request.url.toSafeLogString()}"
            val startedAt = System.nanoTime()
            try {
                val response = chain.proceed(request)
                val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
                i(NETWORK_TAG, "$summary -> ${response.code} (${elapsedMs}ms)")
                response
            } catch (exception: Exception) {
                val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
                e(NETWORK_TAG, "$summary -> failed (${elapsedMs}ms)", exception)
                throw exception
            }
        }
    }

    private fun write(
        level: AppLogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        val safeMessage = sanitize(message)
        val safeThrowable = throwable?.let { error ->
            (error.javaClass.simpleName + "\n" + error.stackTrace.take(12).joinToString("\n")).take(MAX_THROWABLE_LENGTH)
        }

        when (level) {
            AppLogLevel.DEBUG -> Log.d(tag, listOfNotNull(safeMessage, safeThrowable).joinToString("\n"))
            AppLogLevel.INFO -> Log.i(tag, listOfNotNull(safeMessage, safeThrowable).joinToString("\n"))
            AppLogLevel.WARNING -> Log.w(tag, listOfNotNull(safeMessage, safeThrowable).joinToString("\n"))
            AppLogLevel.ERROR -> Log.e(tag, listOfNotNull(safeMessage, safeThrowable).joinToString("\n"))
        }

        val repo = appLogRepo ?: return
        scope.launch {
            runCatching { repo.append(
                level = level,
                tag = tag,
                message = safeMessage,
                throwable = safeThrowable,
            ) }
        }
    }

    fun formatThrowable(error: Throwable): String =
        (error.javaClass.simpleName + "\n" + error.stackTrace.take(24).joinToString("\n")).take(MAX_THROWABLE_LENGTH)

    fun sanitizeForExport(message: String): String = sanitize(message)

    private fun sanitize(message: String): String {
        var sanitized = message.replace(Regex("\\s+"), " ").trim()
        sanitized = sanitized.replace(Regex("(?i)(authorization: bearer)\\s+[^\\s]+"), "$1 [redacted]")
        sanitized = sanitized.replace(Regex("(?i)(access_token=)[^&\\s]+"), "$1[redacted]")
        sanitized = sanitized.replace(Regex("(?i)(refresh_token=)[^&\\s]+"), "$1[redacted]")
        sanitized = sanitized.replace(Regex("https?://[^\\s\"'<>]+")) { match ->
            runCatching { java.net.URI(match.value).let { it.scheme + "://" + it.host + "/[path]" } }.getOrDefault("[url]")
        }
        sanitized = sanitized.replace(Regex("(?i)\\bBearer\\s+[^\\s,;]+"), "Bearer [redacted]")
        sanitized = sanitized.replace(Regex("(?i)(password|access_token|refresh_token|token|cookie|authorization)([\"'\\s]*[:=][\"'\\s]*)[^\\s,;}]+"), "$1$2[redacted]")
        return sanitized.take(MAX_MESSAGE_LENGTH)
    }

    private fun sanitizeThrowable(throwable: String): String {
        return sanitize(throwable).take(MAX_THROWABLE_LENGTH)
    }
}

private fun HttpUrl.toSafeLogString(): String {
    return buildString {
        append(scheme)
        append("://")
        append(host)
        if (port != defaultPort(scheme)) {
            append(':')
            append(port)
        }
        append("/[path]")
    }
}

private fun defaultPort(scheme: String): Int {
    return when (scheme) {
        "http" -> 80
        "https" -> 443
        else -> -1
    }
}
