package me.rerere.awara.di

// Privacy guardrails:
// 1. Keep Iwara auth headers on a dedicated client only.
// 2. If release metadata is needed again later, prefer a first-party endpoint or GitHub Releases instead of a standalone third-party API.

import me.rerere.awara.data.source.IwaraAPI
import me.rerere.awara.util.AppLogger
import me.rerere.awara.util.AppNetworkTransportPolicy
import me.rerere.awara.util.ConfigurableDohDns
import me.rerere.awara.util.NetworkTransportPolicy
import me.rerere.awara.util.SerializationConverterFactory
import me.rerere.compose_setting.preference.mmkvPreference
import okhttp3.OkHttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit

private const val UA = "Mozilla/5.0 (Linux; Android 12; Pixel 6 Build/SD1A.210817.023; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/94.0.4606.71 Mobile Safari/537.36"

private const val PUBLIC_HTTP_CLIENT = "publicHttpClient"
private const val IWARA_HTTP_CLIENT = "iwaraHttpClient"

val networkModule = module {
    single {
        ConfigurableDohDns()
    }

    single<NetworkTransportPolicy> {
        AppNetworkTransportPolicy(dns = get())
    }

    single(named(PUBLIC_HTTP_CLIENT)) {
        get<NetworkTransportPolicy>().apply(
            OkHttpClient.Builder()
        )
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(AppLogger.createSafeNetworkLogInterceptor())
            .build()
    }

    single<OkHttpClient> {
        get(named(PUBLIC_HTTP_CLIENT))
    }

    single(named(IWARA_HTTP_CLIENT)) {
        get<NetworkTransportPolicy>().apply(
            OkHttpClient.Builder()
        )
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor {
                val request = it.request()
                val url = request.url
                val newRequest = request.newBuilder()
                    .apply {
                        if(url.host.contains("iwara.tv")) {
                            addHeader("User-Agent", UA)
                            addHeader("Origin", "https://www.iwara.tv")
                            addHeader("Referer", "https://www.iwara.tv/")
                            addHeader("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7")

                            if (url.toString() == "https://api.iwara.tv/user/token") {
                                if ("refresh_token" in mmkvPreference) {
                                    addHeader(
                                        "Authorization",
                                        "Bearer ${mmkvPreference.getString("refresh_token", "")}"
                                    )
                                }
                            } else {
                                if ("access_token" in mmkvPreference) {
                                    addHeader(
                                        "Authorization",
                                        "Bearer ${mmkvPreference.getString("access_token", "")}"
                                    )
                                }
                            }
                        }
                    }
                    .build()
                it.proceed(newRequest)
            }
            .addInterceptor(AppLogger.createSafeNetworkLogInterceptor())
//            .addInterceptor {
//                val request = it.request()
//                val url = request.url
//                if(url.pathSegments.contains("video")){
//                    // 403模拟
//                    val response = okhttp3.Response.Builder()
//                        .request(request)
//                        .protocol(okhttp3.Protocol.HTTP_1_1)
//                        .code(403)
//                        .message("Forbidden")
//                        .body(
//                            "".toResponseBody()
//                        )
//                        .build()
//                    response
//                } else {
//                    it.proceed(request)
//                }
//            }
            .build()
    }

    single {
        Retrofit.Builder()
            .client(get(named(IWARA_HTTP_CLIENT)))
            .baseUrl("https://api.iwara.tv")
            .addConverterFactory(SerializationConverterFactory.create())
            .build()
            .create(IwaraAPI::class.java)
    }
}