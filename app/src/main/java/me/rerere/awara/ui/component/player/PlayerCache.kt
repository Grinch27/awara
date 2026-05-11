package me.rerere.awara.ui.component.player

import android.content.Context
import android.os.storage.StorageManager
import android.util.Log
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import java.io.File

private const val TAG = "PlayerCache"
private const val PLAYER_USER_AGENT = "Mozilla/5.0 (Linux; Android 12; Pixel 6 Build/SD1A.210817.023; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/94.0.4606.71 Mobile Safari/537.36"

object PlayerCache {
    private lateinit var cacheFactory: CacheDataSource.Factory

    fun get(context: Context): CacheDataSource.Factory {
        if (!::cacheFactory.isInitialized) {
            val cacheFolder = File(context.cacheDir, "player_cache")
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val maxBytes =
                storageManager.getCacheQuotaBytes(storageManager.getUuidForPath(cacheFolder))
            val cache = SimpleCache(
                cacheFolder,
                LeastRecentlyUsedCacheEvictor(maxBytes),
                StandaloneDatabaseProvider(context)
            )
            val cacheFactory = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(
                    DefaultHttpDataSource.Factory()
                        .setUserAgent(PLAYER_USER_AGENT)
                        .setDefaultRequestProperties(
                            mapOf(
                                "Origin" to "https://www.iwara.tv",
                                "Referer" to "https://www.iwara.tv/",
                                "Accept-Language" to "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7",
                            )
                        )
                )
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                .setCacheKeyFactory { dataSpec ->
                    val filename = dataSpec.uri.getQueryParameter("filename")
                    Log.i(TAG, "rememberPlayerState: ${dataSpec.key} || $filename || ${dataSpec.uri}")
                    dataSpec.key ?: filename ?: dataSpec.uri.toString()
                }
            this.cacheFactory = cacheFactory
        }
        return this.cacheFactory
    }
}