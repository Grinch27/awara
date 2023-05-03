package me.rerere.awara.util

/**
 * Returns a human-readable string representation of the given file size in bytes.
 *
 * @param bytes The file size in bytes.
 * @return A string representing the file size with unit (B, KB, MB, GB, TB, PB, EB).
 */
fun prettyFileSize(bytes: Long): String {
    val unit = 1024
    if (bytes < unit) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
    val pre = "KMGTPE"[exp - 1] + "i"
    return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
}