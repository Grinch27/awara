// Requirements: malformed backups cannot erase local data.
// Verify: strict versions, required structure and safe legacy v2 compatibility.
package me.rerere.awara.data.repo

import org.junit.Assert.*
import org.junit.Test
import kotlinx.serialization.encodeToString

class LocalDataBackupCodecTest {
    @Test fun emptyObjectRejected() {
        assertThrows(IllegalArgumentException::class.java) { LocalDataBackupCodec.decode("{}") }
    }
    @Test fun futureVersionRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            LocalDataBackupCodec.decode("""{"version":99,"exportedAt":"2026-01-01T00:00:00Z"}""")
        }
    }
    @Test fun incompleteNewFormatRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            LocalDataBackupCodec.decode("""{"version":3,"format":"awara.local-data","exportedAt":"2026-01-01T00:00:00Z"}""")
        }
    }
    @Test fun exportedEmptyBundleIsExplicitAndValid() {
        val encoded = LocalDataBackupCodec.json.encodeToString(LocalDataBackupBundle())
        val decoded = LocalDataBackupCodec.decode(encoded)
        assertEquals(3, decoded.version)
        assertTrue(decoded.historyItems.isEmpty())
        assertTrue(encoded.contains("historyItems"))
    }
    @Test fun legacyDefaultsRemainReadable() {
        val decoded = LocalDataBackupCodec.decode("""{"exportedAt":"2026-01-01T00:00:00Z"}""")
        assertTrue(decoded.historyItems.isEmpty())
    }
}
