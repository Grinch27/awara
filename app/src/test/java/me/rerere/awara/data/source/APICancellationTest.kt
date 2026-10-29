// Requirements: cancellation must escape API result conversion.
// Verify: a cancelled request cannot become a normal business error.
package me.rerere.awara.data.source

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test

class APICancellationTest {
    @Test fun cancellationIsNotConverted() {
        assertThrows(CancellationException::class.java) {
            runBlocking { runAPICatching<Unit> { throw CancellationException("fixture cancellation") } }
        }
    }
}
