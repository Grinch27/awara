package me.rerere.awara

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import me.rerere.awara.util.ConfigurableDohDns
import me.rerere.awara.util.NetworkStackSnapshot
import okhttp3.OkHttpClient
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Emulator-only smoke tests: no live Iwara account and no external web requests.
 * These DO NOT prove a complete absence of plaintext DNS packets.
 */
@RunWith(AndroidJUnit4::class)
class Media3NetworkSmokeTest {
    @Test
    fun media3HttpDataSourceUsesStrictDnsAndFailsClosedOnInvalidSettings() {
        val snapshots = AtomicInteger()
        val strictDns = ConfigurableDohDns(snapshotProvider = {
            snapshots.incrementAndGet()
            NetworkStackSnapshot(
                dohEnabled = true,
                dohEndpoint = "http://invalid.example/dns-query",
                dohUpstream = "doh.opendns.com/dns-query",
            )
        })
        val publicClient = OkHttpClient.Builder().dns(strictDns).build()
        val source = OkHttpDataSource.Factory(publicClient).createDataSource()
        try {
            source.open(DataSpec(Uri.parse("https://media.example.invalid/video.mp4")))
            fail("Strict DoH must not fall back to system DNS for an invalid endpoint")
        } catch (expected: IOException) {
            assertTrue("Strict DNS lookup must be invoked", snapshots.get() > 0)
        } finally {
            runCatching { source.close() }
        }
    }

    @Test
    fun media3PreparesLocallyGeneratedPcmWaveWithoutNetwork() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val file = File(context.cacheDir, "media3-smoke-${System.nanoTime()}.wav")
        val sampleCount = 8000
        val pcmBytes = sampleCount * 2
        val wave = ByteBuffer.allocate(44 + pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        wave.put("RIFF".toByteArray(Charsets.US_ASCII))
        wave.putInt(36 + pcmBytes)
        wave.put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
        wave.putInt(16)
        wave.putShort(1.toShort()) // PCM
        wave.putShort(1.toShort()) // mono
        wave.putInt(8000)
        wave.putInt(8000 * 2)
        wave.putShort(2.toShort())
        wave.putShort(16.toShort())
        wave.put("data".toByteArray(Charsets.US_ASCII))
        wave.putInt(pcmBytes)
        // Remaining samples are zero; valid half-second silence.
        file.writeBytes(wave.array())

        val prepared = CountDownLatch(1)
        val playbackError = AtomicReference<PlaybackException?>(null)
        var player: ExoPlayer? = null
        try {
            instrumentation.runOnMainSync {
                val current = ExoPlayer.Builder(context).build()
                player = current
                current.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) prepared.countDown()
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        playbackError.set(error)
                        prepared.countDown()
                    }
                })
                current.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                current.prepare()
            }
            assertTrue("Media3 did not prepare the local WAV", prepared.await(30, TimeUnit.SECONDS))
            assertNull("Media3 failed to prepare the local WAV", playbackError.get())
        } finally {
            player?.let { current ->
                instrumentation.runOnMainSync { current.release() }
            }
            file.delete()
        }
    }
}
