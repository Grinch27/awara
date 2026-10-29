// Requirements: persist download identity/progress, playback progress and import commit markers.
// Open decisions: ordinary video files first; adaptive-stream offline packaging remains deferred.
// Verify: migration 6→7, pause/cancel generations and process restart.
package me.rerere.awara.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_tasks")
data class DownloadTask(
    @PrimaryKey val id: String,
    val resourceId: String,
    val title: String,
    val thumbnail: String,
    val quality: String = "",
    val owner: String = "",
    val state: String = "QUEUED",
    val bytes: Long = 0,
    val totalBytes: Long = -1,
    val validator: String = "",
    val targetPath: String = "",
    val error: String = "",
    val generation: Long = 0,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "media_progress", primaryKeys = ["type", "resourceId"])
data class MediaProgress(
    val type: String,
    val resourceId: String,
    val fileId: String = "",
    val position: Long = 0,
    val duration: Long = 0,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "import_commits")
data class ImportCommit(@PrimaryKey val id: String)
