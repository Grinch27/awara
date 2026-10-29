// Requirements: prove refresh coalescing and logout safety without real accounts.
// Verify: a failed refresh is bounded; a late success cannot revive a logged-out session.
package me.rerere.awara.data.session

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class SessionRepositoryTest {
    private class Store : CredentialStore {
        @Volatile var value = SessionSnapshot("fixture-refresh", "expired", "fixture-session", 0)
        override val snapshot get() = value
        @Synchronized override fun commitAccessToken(token: String, expectedEpoch: Long): Boolean {
            if (value.epoch != expectedEpoch || value.refreshToken.isBlank()) return false
            value = value.copy(accessToken = token)
            return true
        }
        @Synchronized fun logout() { value = SessionSnapshot(epoch = value.epoch + 1) }
    }

    @Test fun concurrentFailuresRefreshOnce() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"accessToken":"new-access"}""").setBodyDelay(150, TimeUnit.MILLISECONDS))
            server.start()
            val store = Store()
            val repo = SessionRepository(OkHttpClient(), store, server.url("/user/token"))
            val pool = Executors.newFixedThreadPool(8)
            try {
                val futures = (1..8).map { pool.submit<Boolean> { repo.refresh("expired") } }
                assertTrue(futures.all { it.get(5, TimeUnit.SECONDS) })
                assertEquals(1, server.requestCount)
                assertEquals("new-access", store.snapshot.accessToken)
            } finally { pool.shutdownNow() }
        }
    }

    @Test fun logoutRejectsLateRefresh() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"accessToken":"late-access"}""").setBodyDelay(250, TimeUnit.MILLISECONDS))
            server.start()
            val store = Store()
            val repo = SessionRepository(OkHttpClient(), store, server.url("/user/token"))
            val pool = Executors.newSingleThreadExecutor()
            try {
                val result = pool.submit<Boolean> { repo.refresh("expired") }
                assertNotNull(server.takeRequest(3, TimeUnit.SECONDS))
                store.logout()
                assertFalse(result.get(5, TimeUnit.SECONDS))
                assertTrue(store.snapshot.accessToken.isEmpty())
            } finally { pool.shutdownNow() }
        }
    }

    @Test fun failuresHaveCooldown() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(503))
            server.start()
            val repo = SessionRepository(OkHttpClient(), Store(), server.url("/user/token"))
            assertFalse(repo.refresh("expired"))
            assertFalse(repo.refresh("expired"))
            assertEquals(1, server.requestCount)
        }
    }
}
