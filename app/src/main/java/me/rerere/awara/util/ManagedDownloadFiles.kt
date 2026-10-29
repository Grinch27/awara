package me.rerere.awara.util

import android.content.Context
import android.os.Environment
import java.io.File
import me.rerere.awara.data.entity.DownloadItem
import me.rerere.awara.data.entity.DownloadType

/**
 * Download files live directly under the app-managed Downloads/<type> directory.
 * Persisted paths (including imported backups) are not trusted for deletion.
 */
internal object ManagedDownloadFiles {
    fun taskFile(context: Context, id: String, partial: Boolean): File {
        require(id.matches(Regex("[a-zA-Z0-9-]{1,80}")))
        val directory = managedDirectory(context, DownloadType.VIDEO, create = true) ?: error("Download storage unavailable")
        val file = File(directory, "task-" + id + if (partial) ".part" else ".mp4")
        require(file.canonicalFile.parentFile == directory)
        return file
    }

    fun create(context: Context, type: DownloadType): File? {
        val directory = managedDirectory(context, type, create = true) ?: return null
        val file = File.createTempFile("awara-", "", directory)
        if (file.canonicalFile.parentFile == directory) return file
        file.delete()
        return null
    }

    fun resolveIfManaged(context: Context, item: DownloadItem): File? {
        val directory = managedDirectory(context, item.type, create = false) ?: return null
        val file = File(item.path)
        return file.takeIf { it.isAbsolute && it.parentFile?.canonicalFile == directory &&
            it.canonicalFile.parentFile == directory && it.isFile }
    }

    fun deleteIfManaged(context: Context, item: DownloadItem): Boolean =
        resolveIfManaged(context, item)?.delete() == true

    private fun managedDirectory(context: Context, type: DownloadType, create: Boolean): File? {
        val root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?.canonicalFile ?: return null
        val candidate = File(root, type.name)
        if (create && !candidate.isDirectory && !candidate.mkdirs()) return null
        val directory = candidate.canonicalFile
        return directory.takeIf { it.isDirectory && it.parentFile == root }
    }
}
