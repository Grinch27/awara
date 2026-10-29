package me.rerere.awara.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import java.io.IOException
import android.content.ActivityNotFoundException

private const val TAG = "ContextUtil"

/**
 * 获取当前应用程序的版本代码。
 *
 * @return 当前应用程序的版本代码。
 */
val Context.versionCode: Int
    get() = packageManager.getPackageInfo(packageName, 0).versionCode

/**
 * Open the url in browser
 *
 * @receiver Context
 * @param url String
 */
fun Context.openUrl(url: String) {
    val uri = runCatching { Uri.parse(url) }.getOrNull()
    if (uri?.scheme?.lowercase() in setOf("http", "https") && !uri?.host.isNullOrBlank() && uri?.userInfo.isNullOrBlank()) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(if (this is Activity) 0 else Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) { AppLogger.w(TAG, "No browser available") }
    } else AppLogger.w(TAG, "Rejected invalid external URL")
}

/**
 * Find the activity from the context
 *
 * @receiver Context
 * @return Activity
 * @throws IllegalStateException if the context is not an activity
 */
fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> {
        baseContext.findActivity()
    }
    else -> throw IllegalStateException("Context is not an Activity")
}

/**
 * 将文本复制到剪贴板。
 *
 * @param label 剪贴板条目的标签。
 * @param text 要复制到剪贴板的文本。
 */
fun Context.writeToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}

/**
 * 分享链接
 *
 * @param url 需要分享的链接
 */
fun Context.shareLink(url: String) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.putExtra(Intent.EXTRA_TEXT, url)
    intent.type = "text/plain"
    startActivity(Intent.createChooser(intent, "Share"))
}

@Throws(IOException::class)
fun Context.writeTextDocument(uri: Uri, text: String) {
    contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
        writer.write(text)
    } ?: throw IOException("Unable to open output stream")
}

@Throws(IOException::class)
fun Context.readTextDocument(uri: Uri): String {
    return contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
        val limit = me.rerere.awara.data.repo.LocalDataBackupCodec.MAX_BYTES
        val result = StringBuilder()
        val buffer = CharArray(8192)
        while (true) {
            val count = reader.read(buffer)
            if (count < 0) break
            if (result.length + count > limit) throw IOException("Document exceeds 16 MiB")
            result.append(buffer, 0, count)
        }
        result.toString()
    } ?: throw IOException("Unable to open input stream")
}