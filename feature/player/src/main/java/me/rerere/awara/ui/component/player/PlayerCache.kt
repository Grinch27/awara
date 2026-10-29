package me.rerere.awara.ui.component.player

import android.content.Context
import android.os.storage.StorageManager
import android.util.Log
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import okhttp3.OkHttpClient

private const val TAG = "PlayerCache"
private const val PLAYER_USER_AGENT = "Mozilla/5.0 (Linux; Android 12; Pixel 6 Build/SD1A.210817.023; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/94.0.4606.71 Mobile Safari/537.36"

object PlayerCache {
    // The on-disk cache remains shared, but each DataSource.Factory captures the explicitly
    // supplied PUBLIC OkHttpClient. Never reuse the authenticated Iwara API client for media.
    @Volatile
    private var sharedCache: SimpleCache? = null

    fun get(context: Context, publicHttpClient: OkHttpClient, scope: String = "public"): CacheDataSource.Factory {
        val cache = sharedCache ?: synchronized(this) {
            sharedCache ?: createCache(context.applicationContext).also { sharedCache = it }
        }
        val httpDataSource = OkHttpDataSource.Factory(publicHttpClient)
            .setUserAgent(PLAYER_USER_AGENT)
            .setDefaultRequestProperties(
                mapOf(
                    "Origin" to "https://www.iwara.tv",
                    "Referer" to "https://www.iwara.tv/",
                    "Accept-Language" to "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7",
                )
            )
        return CacheDataSource.Factory()
            .setCache(cache)
            // Keep non-HTTP (file/content) handling; all HTTP(S) playback uses OkHttp/DoH.
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context, httpDataSource))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            .setCacheKeyFactory { dataSpec ->
                dataSpec.key ?: scope + ":" + dataSpec.uri.toString()
            }
    }

    private fun createCache(context: Context): SimpleCache {
        val cacheFolder = File(context.cacheDir, "player_cache")
        cacheFolder.mkdirs()
        val maxBytes = runCatching {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            (storageManager.getCacheQuotaBytes(storageManager.getUuidForPath(cacheFolder)) / 4)
                .coerceIn(64L * 1024 * 1024, 512L * 1024 * 1024)
        }.getOrDefault(128L * 1024 * 1024)
        return SimpleCache(
            cacheFolder,
            LeastRecentlyUsedCacheEvictor(maxBytes),
            StandaloneDatabaseProvider(context)
        )
    }
}
