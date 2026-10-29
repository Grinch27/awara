// Requirements: one bounded refresh for simultaneous failures; never replay writes.
// Open decisions: only API HTTP 401 indicates access-token expiry.
// Verify: refresh failure cooldown, concurrent refresh and logout during refresh.
package me.rerere.awara.data.session

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import me.rerere.awara.data.dto.AccessTokenRes
import me.rerere.awara.util.JsonInstance
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SessionRepository(
    publicClient: OkHttpClient,
    private val store: CredentialStore = SecureSession,
    private val endpoint: HttpUrl = "https://api.iwara.tv/user/token".toHttpUrl(),
) {
    private val lock = Any()
    private val client = publicClient.newBuilder().callTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false).build()
    private var failedEpoch = -1L
    private var failedAt = 0L

    suspend fun ensureAccessToken(): Boolean = withContext(Dispatchers.IO) { refresh(null) }

    fun refresh(rejectedToken: String?): Boolean = synchronized(lock) {
        val before = store.snapshot
        if (before.refreshToken.isBlank()) return@synchronized false
        if (before.accessToken.isNotBlank() && (rejectedToken == null || rejectedToken != before.accessToken)) {
            return@synchronized true
        }
        if (failedEpoch == before.epoch && System.nanoTime() - failedAt < TimeUnit.SECONDS.toNanos(10)) {
            return@synchronized false
        }
        val success = try {
            val request = Request.Builder().url(endpoint)
                .header("Authorization", "Bearer " + before.refreshToken)
                .post(ByteArray(0).toRequestBody()).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 401) store.invalidate(before.epoch)
                    false
                }
                else {
                    val body = response.body ?: return@use false
                    val token = JsonInstance.decodeFromString<AccessTokenRes>(body.string()).accessToken
                    store.commitAccessToken(token, before.epoch)
                }
            }
        } catch (_: Exception) { false }
        if (!success) {
            failedEpoch = before.epoch
            failedAt = System.nanoTime()
        }
        success
    }
}
