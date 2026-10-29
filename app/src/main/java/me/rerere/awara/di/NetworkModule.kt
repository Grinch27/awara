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
import me.rerere.awara.data.session.SecureSession
import me.rerere.awara.data.session.SessionRepository
import okhttp3.OkHttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit

private const val UA = "Mozilla/5.0 (Linux; Android 12; Pixel 6 Build/SD1A.210817.023; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/94.0.4606.71 Mobile Safari/537.36"

// Authorize only the genuine Iwara domain and its subdomains; never a lookalike.
internal fun isTrustedIwaraHost(host: String): Boolean =
    host == "iwara.tv" || host.endsWith(".iwara.tv")

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

    single { SessionRepository(get<OkHttpClient>()) }

    single(named(IWARA_HTTP_CLIENT)) {
        val session = get<SessionRepository>()
        get<OkHttpClient>().newBuilder()
            .addInterceptor { chain ->
                val request = chain.request()
                val builder = request.newBuilder()
                if (request.url.isHttps && request.url.host == "api.iwara.tv") {
                    builder.header("User-Agent", UA).header("Origin", "https://www.iwara.tv")
                        .header("Referer", "https://www.iwara.tv/")
                    val snapshot = SecureSession.snapshot
                    val token = if (request.url.encodedPath == "/user/token") snapshot.refreshToken else snapshot.accessToken
                    if (token.isNotBlank()) builder.header("Authorization", "Bearer " + token)
                }
                chain.proceed(builder.build())
            }
            .authenticator { _, response ->
                val request = response.request
                val rejected = request.header("Authorization")?.removePrefix("Bearer ")
                if (response.code != 401 || response.priorResponse != null ||
                    !request.url.isHttps || request.url.host != "api.iwara.tv" ||
                    request.url.encodedPath == "/user/token" ||
                    request.method !in setOf("GET", "HEAD") || rejected.isNullOrBlank()
                ) null
                else if (session.refresh(rejected)) {
                    request.newBuilder().header("Authorization", "Bearer " + SecureSession.snapshot.accessToken).build()
                } else null
            }
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
