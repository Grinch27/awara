package me.rerere.awara.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rerere.awara.R
import me.rerere.awara.data.entity.DownloadItem
import me.rerere.awara.data.entity.DownloadType
import me.rerere.awara.di.AppDatabase
import me.rerere.awara.util.await
import me.rerere.awara.util.prettyFileSize
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant

private const val TAG = "DownloadWorker"

class DownloadWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {
    private val okHttpClient = OkHttpClient.Builder().build()
    private val appDatabase by inject<AppDatabase>()
    private val notificationId = notificationIdCounter++
    private val notification = NotificationCompat.Builder(appContext, "download")
        .setContentTitle(appContext.getString(R.string.download))
        .setSmallIcon(R.drawable.baseline_cloud_download_24)
        .setOngoing(true)
        .setSilent(true)

    override suspend fun doWork(): Result {
        if (ActivityCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure()
        }

        setForeground(ForegroundInfo(notificationId, notification.build()))

        val url = inputData.getString(KEY_DOWNLOAD_URL) ?: return Result.failure()
        val title = inputData.getString(KEY_DOWNLOAD_TITLE) ?: return Result.failure()
        val thumbnail = inputData.getString(KEY_DOWNLOAD_THUMBNAIL) ?: return Result.failure()
        val type = inputData.getString(KEY_DOWNLOAD_TYPE) ?: return Result.failure()
        val resourceId = inputData.getString(KEY_DOWNLOAD_RESOURCE_ID) ?: return Result.failure()

        val destinationDir =
            appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.resolve(type)
                ?: return Result.failure()
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        val fileName = "${title.trim()}.$resourceId.${Instant.now().epochSecond}"
        val destinationFile = destinationDir.resolve(fileName)
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        withContext(Dispatchers.IO) {
            destinationFile.createNewFile()
        }

        Log.i(TAG, "doWork: 开始下载: $url => ${destinationFile.absolutePath}")
        setForeground(ForegroundInfo(
            notificationId,
            notification
                .setContentText(title)
                .setProgress(100, 0, true)
                .build()
        ))

        return withContext(Dispatchers.IO) {
            kotlin.runCatching {
                val request = Request.Builder()
                    .url(url)
                    .get()

                val response = okHttpClient.newCall(request.build()).await()
                if (response.isSuccessful) {
                    val total = response.body?.contentLength() ?: 0
                    val fileSizePretty = prettyFileSize(total)
                    Log.i(TAG, "doWork: 文件大小: $total")

                    destinationFile.outputStream().use { out ->
                        response.body?.byteStream()?.use { input ->
                            val buffer = ByteArray(1024 * 8)
                            var len: Int
                            var step = 0
                            while (input.read(buffer).also { len = it } != -1) {
                                out.write(buffer, 0, len)

                                if(step++ % 25 == 0 || len != buffer.size){
                                    val percent = (destinationFile.length() * 100 / total).toInt()
                                    val currentSizePretty = prettyFileSize(destinationFile.length())

                                    setForeground(
                                        ForegroundInfo(
                                            notificationId,
                                            notification
                                                .setContentTitle(title)
                                                .setContentText("$percent% ($currentSizePretty / $fileSizePretty)")
                                                .setProgress(100, percent, false)
                                                .build()
                                        )
                                    )

                                    Log.i(TAG, "doWork: $percent% #$title")
                                }
                            }
                        }
                    }

                    Log.i(TAG, "doWork: 下载成功！当前文件大小: ${destinationFile.length()} 路径: ${destinationFile.absolutePath}")

                    appDatabase.downloadDao().insertDownloadItem(DownloadItem(
                        title = title,
                        thumbnail = thumbnail,
                        type = DownloadType.valueOf(type),
                        resourceId = resourceId,
                        path = destinationFile.absolutePath,
                        time = Instant.now()
                    ))

                    Log.i(TAG, "doWork: 已插入数据库")
                } else {
                    error("下载失败: ${response.code}")
                }
            }
        }.fold(
            onSuccess = {
                Result.success()
            },
            onFailure = {
                Log.e(TAG, "doWork: 下载失败", it)
                kotlin.runCatching {  destinationFile.delete() }
                Result.failure()
            }
        )
    }

    companion object {
        private var notificationIdCounter = 0

        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_DOWNLOAD_TITLE = "download_title"
        const val KEY_DOWNLOAD_THUMBNAIL = "download_thumbnail"
        const val KEY_DOWNLOAD_TYPE = "download_type"
        const val KEY_DOWNLOAD_RESOURCE_ID = "download_resource_id"
    }
}