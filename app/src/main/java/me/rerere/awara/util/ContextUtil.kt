package me.rerere.awara.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.util.Log

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
    if(url.matches(Regex("https?://.*"))) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    } else {
        Log.w(TAG, "openUrl: url is not valid: $url")
    }
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