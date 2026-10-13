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
    fun create(context: Context, type: DownloadType): File? {
        val directory = managedDirectory(context, type, create = true) ?: return null
        val file = File.createTempFile("awara-", "", directory)
        if (file.canonicalFile.parentFile == directory) return file
        file.delete()
        return null
    }

    fun deleteIfManaged(context: Context, item: DownloadItem): Boolean {
        val directory = managedDirectory(context, item.type, create = false) ?: return false
        val file = File(item.path)
        if (!file.isAbsolute ||
            file.parentFile?.canonicalFile != directory ||
            file.canonicalFile.parentFile != directory ||
            !file.isFile
        ) return false
        return file.delete()
    }

    private fun managedDirectory(context: Context, type: DownloadType, create: Boolean): File? {
        val root = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?.canonicalFile ?: return null
        val candidate = File(root, type.name)
        if (create && !candidate.isDirectory && !candidate.mkdirs()) return null
        val directory = candidate.canonicalFile
        return directory.takeIf { it.isDirectory && it.parentFile == root }
    }
}
