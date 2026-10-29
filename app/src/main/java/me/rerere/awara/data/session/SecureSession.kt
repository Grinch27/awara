// Requirements: passwords stay in memory; retained credentials use Android Keystore.
// Open decisions: production signing and biometric UI are separate features.
// Risks/verification: key loss requires login; logout must reject late refreshes;
// no session bytes may enter system backups or diagnostic logs.
package me.rerere.awara.data.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.rerere.compose_setting.preference.mmkvPreference
import org.json.JSONObject

data class SessionSnapshot(
    val refreshToken: String = "",
    val accessToken: String = "",
    val sessionId: String = "",
    val epoch: Long = 0,
) {
    override fun toString() = "SessionSnapshot(authenticated=" + refreshToken.isNotBlank() + ", epoch=" + epoch + ")"
}

interface CredentialStore {
    val snapshot: SessionSnapshot
    fun invalidate(expectedEpoch: Long)
    fun commitAccessToken(token: String, expectedEpoch: Long): Boolean
}

object SecureSession : CredentialStore {
    private const val ALIAS = "awara.session.v1"
    private val mutableState = MutableStateFlow(SessionSnapshot())
    val state = mutableState.asStateFlow()
    override val snapshot get() = state.value
    private lateinit var file: AtomicFile

    @Synchronized
    fun initialize(context: Context) {
        file = AtomicFile(File(context.noBackupFilesDir, "session-v1.bin"))
        mutableState.value = runCatching {
            if (!file.baseFile.exists()) return@runCatching SessionSnapshot()
            val bytes = file.readFully()
            require(bytes.size in 30..65_536 && bytes[0].toInt() == 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
            val json = JSONObject(String(cipher.doFinal(bytes.copyOfRange(13, bytes.size)), Charsets.UTF_8))
            SessionSnapshot(json.getString("refresh"), json.optString("access"), json.getString("session"))
        }.getOrElse { error ->
            file.delete()
            if (error is java.security.GeneralSecurityException) runCatching {
                KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(ALIAS) }
            }
            SessionSnapshot()
        }
        if (snapshot.refreshToken.isBlank()) {
            val legacy = mmkvPreference.getString("refresh_token", "").orEmpty()
            if (legacy.isNotBlank()) runCatching { signIn(legacy) }
        }
        listOf("login.password", "refresh_token", "access_token").forEach { mmkvPreference.remove(it) }
    }

    @Synchronized
    fun signIn(refreshToken: String) {
        require(refreshToken.isNotBlank() && refreshToken.length <= 16_384)
        val next = SessionSnapshot(refreshToken = refreshToken, sessionId = UUID.randomUUID().toString(), epoch = snapshot.epoch + 1)
        persist(next)
        mutableState.value = next
    }

    @Synchronized
    override fun commitAccessToken(token: String, expectedEpoch: Long): Boolean {
        if (expectedEpoch != snapshot.epoch || snapshot.refreshToken.isBlank()) return false
        require(token.isNotBlank() && token.length <= 16_384)
        val next = snapshot.copy(accessToken = token)
        persist(next)
        mutableState.value = next
        return true
    }

    @Synchronized
    override fun invalidate(expectedEpoch: Long) {
        if (snapshot.epoch == expectedEpoch) logout()
    }

    @Synchronized
    fun logout() {
        mutableState.value = SessionSnapshot(epoch = snapshot.epoch + 1)
        if (::file.isInitialized) file.delete()
        listOf("login.password", "refresh_token", "access_token").forEach { mmkvPreference.remove(it) }
    }

    private fun persist(value: SessionSnapshot) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val json = JSONObject().put("refresh", value.refreshToken).put("access", value.accessToken).put("session", value.sessionId)
        val encrypted = cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
        val stream = file.startWrite()
        try {
            stream.write(byteArrayOf(cipher.iv.size.toByte()))
            stream.write(cipher.iv)
            stream.write(encrypted)
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build())
        }.generateKey()
    }
}
