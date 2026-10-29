// Requirements: durable unique tasks; WorkManager input contains IDs, never signed URLs.
// Open decisions: ordinary video files first; Wi-Fi-only scheduling can be added as an option.
// Verify: duplicate taps, pause/resume, cancellation and account changes.
package me.rerere.awara.worker

import android.content.Context
import androidx.work.*
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.rerere.awara.data.entity.DownloadTask
import me.rerere.awara.data.session.SecureSession
import me.rerere.awara.di.AppDatabase

object DownloadScheduler {
    private val mutex = Mutex()
    suspend fun enqueue(context: Context, db: AppDatabase, resourceId: String, title: String, thumbnail: String, quality: String): String = withContext(Dispatchers.IO) { mutex.withLock {
        val owner = SecureSession.snapshot.sessionId
        val existing = db.transferDao().activeTask(resourceId, owner)
        val task = existing ?: DownloadTask(UUID.randomUUID().toString(), resourceId, title, thumbnail, quality, owner)
            .also { db.transferDao().insertTask(it) }
        if (existing == null) schedule(context, task.id)
        else if (existing.state in setOf("PAUSED", "FAILED", "WAITING_LOGIN")) {
            db.transferDao().control(task.id, "QUEUED")
            schedule(context, task.id)
        }
        task.id
    } }

    suspend fun control(context: Context, db: AppDatabase, id: String, state: String) = mutex.withLock {
        val task = db.transferDao().task(id) ?: return@withLock
        if (task.state == "COMPLETED") return@withLock
        if (state == "QUEUED" && task.owner.isNotBlank() && task.owner != SecureSession.snapshot.sessionId) {
            db.transferDao().control(id, "WAITING_LOGIN")
            return@withLock
        }
        db.transferDao().control(id, state)
        WorkManager.getInstance(context).cancelUniqueWork("download:" + id).result.get()
        if (state == "QUEUED") schedule(context, id)
    }

    private fun schedule(context: Context, id: String) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(DownloadWorker.KEY_TASK_ID to id))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("awara-download")
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("download:" + id, ExistingWorkPolicy.REPLACE, request)
    }
}
