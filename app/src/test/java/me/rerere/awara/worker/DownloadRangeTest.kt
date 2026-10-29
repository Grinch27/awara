// Requirements: never append an unrelated or malformed range.
package me.rerere.awara.worker

import org.junit.Assert.*
import org.junit.Test

class DownloadRangeTest {
    @Test fun fullResponseRestarts() {
        assertEquals(DownloadRange.Plan(false, 100), DownloadRange.plan(200, 50, null, 100))
    }
    @Test fun unknownLengthIsIndeterminate() {
        assertEquals(-1L, DownloadRange.plan(200, 0, null, -1).total)
    }
    @Test fun validRangeAppends() {
        assertEquals(DownloadRange.Plan(true, 100), DownloadRange.plan(206, 50, "bytes 50-99/100", 50))
    }
    @Test fun wrongOffsetRejected() {
        assertThrows(IllegalArgumentException::class.java) { DownloadRange.plan(206, 50, "bytes 0-49/100", 50) }
    }
    @Test fun truncatedRangeRejected() {
        assertThrows(IllegalArgumentException::class.java) { DownloadRange.plan(206, 50, "bytes 50-99/100", 20) }
    }
    @Test fun unsatisfiableIsNotCompletion() {
        assertThrows(IllegalArgumentException::class.java) { DownloadRange.plan(416, 100, "bytes */100", 0) }
    }
}
