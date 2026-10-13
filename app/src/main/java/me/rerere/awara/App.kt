package me.rerere.awara

import android.app.Application
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.svg.SvgDecoder
import coil3.disk.DiskCache
import coil3.asImage
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import me.rerere.awara.di.databaseModule
import me.rerere.awara.di.networkModule
import me.rerere.awara.di.repoModule
import me.rerere.awara.di.userCaseModule
import me.rerere.awara.di.viewModelModule
import me.rerere.awara.ui.registerErrorHandler
import me.rerere.awara.util.AppLogger
import me.rerere.compose_setting.preference.initComposeSetting
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin

class App : Application(), SingletonImageLoader.Factory, KoinComponent {
    override fun onCreate() {
        super.onCreate()
        registerErrorHandler()
        initComposeSetting()
        registerNotificationChannel()
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(
                networkModule,
                databaseModule,
                repoModule,
                viewModelModule,
                userCaseModule
            )
        }
        AppLogger.install(get())
    }

    private fun registerNotificationChannel() {
        val notificationManagerCompat = NotificationManagerCompat.from(this)
        notificationManagerCompat.createNotificationChannel(
            NotificationChannelCompat.Builder("download", NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName("Download")
                .build()
        )
    }

    override fun newImageLoader(context: android.content.Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { get<OkHttpClient>() }))
                add(SvgDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .fallback(context.getDrawable(R.drawable.default_background)?.asImage())
            .error(context.getDrawable(R.drawable.cancel)?.asImage())
            .build()
    }
}