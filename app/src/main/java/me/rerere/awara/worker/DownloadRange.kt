// Requirements: resume only the same representation; unknown totals remain indeterminate.
// Verify: 200 restart, valid/invalid 206, 416 restart, changed validators.
package me.rerere.awara.worker

object DownloadRange {
    data class Plan(val append: Boolean, val total: Long)
    fun plan(code: Int, offset: Long, contentRange: String?, length: Long): Plan {
        if (code == 200) return Plan(false, length.takeIf { it >= 0 } ?: -1)
        require(code == 206) { "Unexpected download response" }
        val match = Regex("bytes ([0-9]+)-([0-9]+)/([0-9]+)").matchEntire(contentRange.orEmpty())
            ?: error("Invalid Content-Range")
        val start = match.groupValues[1].toLong()
        val end = match.groupValues[2].toLong()
        val total = match.groupValues[3].toLong()
        require(start == offset && end >= start && total > end)
        require(length < 0 || length == end - start + 1)
        return Plan(offset > 0, total)
    }
}
