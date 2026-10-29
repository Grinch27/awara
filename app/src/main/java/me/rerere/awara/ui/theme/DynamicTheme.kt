package me.rerere.awara.ui.theme

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import android.util.Log
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.google.android.material.color.utilities.QuantizerCelebi
import com.google.android.material.color.utilities.Scheme
import com.google.android.material.color.utilities.Score
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rerere.awara.util.await
import me.rerere.compose_setting.preference.mmkvPreference
import me.rerere.compose_setting.preference.rememberBooleanPreference
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

private const val TAG = "DynamicTheme"

@Composable
fun DynamicColorTheme(
    state: DynamicThemeState,
    content: @Composable () -> Unit
) {
    val dynamicColor by rememberBooleanPreference(
        key = "setting.dynamic_color",
        default = true
    )
    val darkMode = LocalDarkMode.current
    val scheme = if (dynamicColor) {
        if (darkMode) state.darkScheme else state.lightScheme
    } else null
    MaterialTheme(
        colorScheme = scheme ?: MaterialTheme.colorScheme,
    ) {
        content()
    }
}

@Stable
@SuppressLint("RestrictedApi")
class DynamicThemeState : KoinComponent {
    var lightScheme by mutableStateOf<ColorScheme?>(null)
        private set

    var darkScheme by mutableStateOf<ColorScheme?>(null)
        private set

    suspend fun updateColorScheme(
        url: String
    ) {
        if (url.isEmpty() || !mmkvPreference.getBoolean("setting.dynamic_color", true) ||
            mmkvPreference.getBoolean("setting.block_media_thumbnails", false)) return
        try {
            val seed = withContext(Dispatchers.IO) {
                val request = Request.Builder().url(url).get().build()
                get<OkHttpClient>().newCall(request).await().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body ?: return@withContext null
                    val bytes = ByteArrayOutputStream().use { out ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                require(out.size() + count <= 4 * 1024 * 1024) { "Thumbnail too large" }
                                out.write(buffer, 0, count)
                            }
                        }
                        out.toByteArray()
                    }
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
                    var sample = 1
                    while (bounds.outWidth / sample > 192 || bounds.outHeight / sample > 192) sample *= 2
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
                        BitmapFactory.Options().apply { inSampleSize = sample }) ?: return@withContext null
                    try {
                        val pixels = IntArray(bitmap.width * bitmap.height)
                        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                        Score.score(QuantizerCelebi.quantize(pixels, 64)).firstOrNull()
                    } finally { bitmap.recycle() }
                }
            } ?: return
            withContext(Dispatchers.Main.immediate) {
                lightScheme = Scheme.light(seed).toColorScheme()
                darkScheme = Scheme.dark(seed).toColorScheme()
            }
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) { /* Keep the Material 3 fallback palette. */ }
    }

    private fun Scheme.toColorScheme(): ColorScheme {
        return ColorScheme(
            primary = Color(primary),
            onPrimary = Color(onPrimary),
            primaryContainer = Color(primaryContainer),
            onPrimaryContainer = Color(onPrimaryContainer),
            inversePrimary = Color(inversePrimary),
            secondary = Color(secondary),
            onSecondary = Color(onSecondary),
            secondaryContainer = Color(secondaryContainer),
            onSecondaryContainer = Color(onSecondaryContainer),
            tertiary = Color(tertiary),
            onTertiary = Color(onTertiary),
            tertiaryContainer = Color(tertiaryContainer),
            onTertiaryContainer = Color(onTertiaryContainer),
            background = Color(background),
            onBackground = Color(onBackground),
            surface = Color(surface),
            onSurface = Color(onSurface),
            surfaceVariant = Color(surfaceVariant),
            onSurfaceVariant = Color(onSurfaceVariant),
            surfaceTint = Color(primary),
            inverseSurface = Color(inverseSurface),
            inverseOnSurface = Color(inverseOnSurface),
            error = Color(error),
            onError = Color(onError),
            errorContainer = Color(errorContainer),
            onErrorContainer = Color(onErrorContainer),
            outline = Color(outline),
            outlineVariant = Color(outlineVariant),
            scrim = Color(scrim),
        )
    }
}

@Composable
fun rememberDynamicColorSchemeState() = remember { DynamicThemeState() }