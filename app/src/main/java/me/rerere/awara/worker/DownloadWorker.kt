// Requirements: persistent file downloads with bounded concurrency and safe resumable writes.
// Risks: signed URLs are re-resolved; HLS/DASH packages are deliberately unsupported here.
// Verify: unknown length, 200/206/416, cancellation, expired URLs and atomic completion.
package me.rerere.awara.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.room.withTransaction
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import me.rerere.awara.R
import me.rerere.awara.data.entity.*
import me.rerere.awara.data.repo.MediaRepo
import me.rerere.awara.data.session.SecureSession
import me.rerere.awara.di.AppDatabase
import me.rerere.awara.util.ManagedDownloadFiles
import me.rerere.awara.util.await
import me.rerere.awara.util.prettyFileSize
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant

class DownloadWorker(private val appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params), KoinComponent {
    private val client by inject<okhttp3.OkHttpClient>()
    private val db by inject<AppDatabase>()
    private val mediaRepo by inject<MediaRepo>()
    private val dao get() = db.transferDao()
    private val notificationId = id.hashCode() and Int.MAX_VALUE
    private fun foreground(title: String, bytes: Long = 0, total: Long = -1): ForegroundInfo {
        val percent = if (total > 0) ((bytes.toDouble() / total) * 100).toInt().coerceIn(0, 100) else 0
        val notification = NotificationCompat.Builder(appContext, "download")
            .setSmallIcon(R.drawable.baseline_cloud_download_24).setOngoing(true).setSilent(true)
            .setContentTitle(title)
            .setContentText(prettyFileSize(bytes) + if (total > 0) " / " + prettyFileSize(total) else "")
            .setProgress(100, percent, total <= 0).build()
        return if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else ForegroundInfo(notificationId, notification)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Legacy queued work is reconstructed from resource metadata; its URL is never reused.
        val taskId = inputData.getString(KEY_TASK_ID) ?: run {
            val resource = inputData.getString(KEY_DOWNLOAD_RESOURCE_ID) ?: return@withContext Result.failure()
            DownloadScheduler.enqueue(appContext, db, resource, inputData.getString(KEY_DOWNLOAD_TITLE).orEmpty(),
                inputData.getString(KEY_DOWNLOAD_THUMBNAIL).orEmpty(), "")
            return@withContext Result.success()
        }
        val task = dao.task(taskId) ?: return@withContext Result.failure()
        if (task.state in setOf("PAUSED", "CANCELLED", "COMPLETED")) return@withContext Result.success()
        var bytes = task.bytes
        var total = task.totalBytes
        var validator = task.validator
        val files = runCatching {
            ManagedDownloadFiles.taskFile(appContext, taskId, true) to ManagedDownloadFiles.taskFile(appContext, taskId, false)
        }.getOrElse {
            dao.progress(taskId, task.generation, "FAILED", bytes, total, validator, task.targetPath, "STORAGE_UNAVAILABLE")
            return@withContext Result.failure()
        }
        val (partial, target) = files
        suspend fun update(state: String, error: String = "") =
            dao.progress(taskId, task.generation, state, bytes, total, validator, target.absolutePath, error)
        fun checkOwner() {
            check(task.owner.isBlank() || task.owner == SecureSession.snapshot.sessionId) { "Account changed" }
        }
        try {
            checkOwner()
            setForeground(foreground(task.title))
            slots.withPermit {
                coroutineContext.ensureActive()
                if (update("RUNNING") == 0) return@withPermit
                // Recover a rename that happened immediately before an interrupted DB commit.
                if (target.isFile && !partial.exists()) check(target.renameTo(partial))
                var completed = false
                repeat(3) { attempt ->
                    if (completed) return@repeat
                    checkOwner()
                    coroutineContext.ensureActive()
                    val video = mediaRepo.getVideo(task.resourceId)
                    val choices = mediaRepo.getVideoStreamManifest(video).filter { it.name != "preview" }
                    val selected = if (task.quality.isBlank()) choices.lastOrNull() else choices.find { it.name == task.quality }
                    requireNotNull(selected) { "Selected quality unavailable" }
                    val url = selected.src.download.ifBlank { selected.playbackUrl() }.fixUrl().toHttpUrl()
                    require(url.isHttps && !url.encodedPath.endsWith(".m3u8", true) && !url.encodedPath.endsWith(".mpd", true)) { "Unsupported file source" }
                    var offset = if (validator.isNotBlank()) partial.length() else 0L
                    if (offset == 0L && partial.exists()) FileOutputStream(partial).use { }
                    val request = Request.Builder().url(url).header("Accept-Encoding", "identity").apply {
                        if (offset > 0) { header("Range", "bytes=" + offset + "-"); header("If-Range", validator) }
                    }.build()
                    val call = client.newCall(request)
                    coroutineScope {
                        val cancellation = launch(start = CoroutineStart.UNDISPATCHED) {
                            try { awaitCancellation() } finally { call.cancel() }
                        }
                        try {
                            call.await().use { response ->
                                if ((response.code == 401 || response.code == 403) && attempt < 2) return@use
                                if (response.code == 416 && attempt < 2) {
                                    FileOutputStream(partial).use { }
                                    bytes = 0; total = -1; validator = ""; update("RUNNING")
                                    return@use
                                }
                                if (!response.isSuccessful) throw IOException("Download HTTP " + response.code)
                                val body = response.body ?: throw IOException("Missing download body")
                                val mime = body.contentType()?.toString().orEmpty().lowercase()
                                require(!mime.contains("mpegurl") && !mime.contains("dash+xml") && !mime.contains("text/html")) { "Unsupported content type" }
                                val plan = DownloadRange.plan(response.code, offset, response.header("Content-Range"), body.contentLength())
                                val nextValidator = response.header("ETag")?.takeUnless { it.startsWith("W/") }
                                    ?: response.header("Last-Modified").orEmpty()
                                if (plan.append && nextValidator.isNotBlank() && nextValidator != validator) {
                                    FileOutputStream(partial).use { }; bytes = 0; validator = ""; total = -1
                                    update("RUNNING")
                                    return@use
                                }
                                if (!plan.append) offset = 0
                                if (plan.total > 0) require(partial.parentFile!!.usableSpace > plan.total - offset + 16L * 1024 * 1024) { "Insufficient storage" }
                                bytes = offset; total = plan.total; validator = nextValidator.ifBlank { if (plan.append) validator else "" }
                                check(update("RUNNING") > 0) { "Task changed" }
                                var lastUpdate = System.currentTimeMillis()
                                FileOutputStream(partial, plan.append).use { output ->
                                    body.byteStream().use { input ->
                                        val buffer = ByteArray(64 * 1024)
                                        while (true) {
                                            coroutineContext.ensureActive()
                                            checkOwner()
                                            val count = input.read(buffer)
                                            if (count < 0) break
                                            output.write(buffer, 0, count); bytes += count
                                            if (System.currentTimeMillis() - lastUpdate >= 750) {
                                                check(update("RUNNING") > 0) { "Task changed" }
                                                setForeground(foreground(task.title, bytes, total))
                                                lastUpdate = System.currentTimeMillis()
                                            }
                                        }
                                    }
                                    output.fd.sync()
                                }
                                require(bytes > 0 && (total < 0 || bytes == total)) { "Incomplete file" }
                                coroutineContext.ensureActive(); checkOwner()
                                db.withTransaction {
                                    val latest = dao.task(taskId)
                                    check(latest != null && latest.generation == task.generation && latest.state == "RUNNING") { "Task changed" }
                                    check(partial.renameTo(target)) { "Unable to commit download" }
                                    val old = db.downloadDao().getDownloadItem(task.resourceId)
                                    db.downloadDao().insertDownloadItem(DownloadItem(id = old?.id ?: 0, title = task.title,
                                        thumbnail = task.thumbnail, path = target.absolutePath, type = DownloadType.VIDEO,
                                        resourceId = task.resourceId, time = Instant.now()))
                                    check(update("COMPLETED") > 0)
                                }
                                completed = true
                            }
                        } finally { cancellation.cancel() }
                    }
                }
                check(completed) { "Source could not be refreshed" }
            }
            Result.success()
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { update("QUEUED") }
            throw cancelled
        } catch (error: Exception) {
            val ownerChanged = task.owner.isNotBlank() && task.owner != SecureSession.snapshot.sessionId
            val retry = error is IOException && runAttemptCount < 3 && !ownerChanged
            update(if (ownerChanged) "WAITING_LOGIN" else if (retry) "QUEUED" else "FAILED",
                if (ownerChanged) "Account changed" else error.javaClass.simpleName)
            if (retry) Result.retry() else Result.failure()
        } finally {
            withContext(NonCancellable) {
                if (dao.task(taskId)?.state == "CANCELLED") partial.delete()
            }
        }
    }

    companion object {
        private val slots = Semaphore(2)
        const val KEY_TASK_ID = "task_id"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_DOWNLOAD_TITLE = "download_title"
        const val KEY_DOWNLOAD_THUMBNAIL = "download_thumbnail"
        const val KEY_DOWNLOAD_TYPE = "download_type"
        const val KEY_DOWNLOAD_RESOURCE_ID = "download_resource_id"
    }
}
