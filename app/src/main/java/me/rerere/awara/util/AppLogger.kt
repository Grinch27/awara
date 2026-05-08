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
        val safeThrowable = throwable?.stackTraceToString()?.let(::sanitizeThrowable)

        when (level) {
            AppLogLevel.DEBUG -> Log.d(tag, safeMessage, throwable)
            AppLogLevel.INFO -> Log.i(tag, safeMessage, throwable)
            AppLogLevel.WARNING -> Log.w(tag, safeMessage, throwable)
            AppLogLevel.ERROR -> Log.e(tag, safeMessage, throwable)
        }

        val repo = appLogRepo ?: return
        scope.launch {
            repo.append(
                level = level,
                tag = tag,
                message = safeMessage,
                throwable = safeThrowable,
            )
        }
    }

    private fun sanitize(message: String): String {
        var sanitized = message.replace(Regex("\\s+"), " ").trim()
        sanitized = sanitized.replace(Regex("(?i)(authorization: bearer)\\s+[^\\s]+"), "$1 [redacted]")
        sanitized = sanitized.replace(Regex("(?i)(access_token=)[^&\\s]+"), "$1[redacted]")
        sanitized = sanitized.replace(Regex("(?i)(refresh_token=)[^&\\s]+"), "$1[redacted]")
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
        append(encodedPath)
    }
}

private fun defaultPort(scheme: String): Int {
    return when (scheme) {
        "http" -> 80
        "https" -> 443
        else -> -1
    }
}