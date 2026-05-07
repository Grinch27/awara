package me.rerere.awara.util

// TODO(user): Decide whether built-in DoH should stay enabled by default for all installs.
// TODO(agent): If future Iwara networking needs per-host routing, split media and API DNS policies instead of overloading one global resolver.
// TODO(agent): If users report resolver incompatibilities, add explicit validation and a one-tap reset action in Settings.

import java.net.InetAddress
import java.util.concurrent.TimeUnit
import me.rerere.compose_setting.preference.mmkvPreference
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps

const val SETTING_NETWORK_DOH_ENABLED = "setting.network_doh_enabled"
const val SETTING_NETWORK_DOH_ENDPOINT = "setting.network_doh_endpoint"
const val SETTING_NETWORK_DOH_UPSTREAM = "setting.network_doh_upstream"

const val DEFAULT_NETWORK_DOH_ENDPOINT = "doh.opendns.com/dns-query"
const val DEFAULT_NETWORK_DOH_UPSTREAM = "dns.alidns.com/dns-query"

private data class DohPreferenceSnapshot(
    val enabled: Boolean,
    val endpoint: String,
    val upstream: String,
) {
    companion object {
        fun fromPreferences(): DohPreferenceSnapshot {
            return DohPreferenceSnapshot(
                enabled = mmkvPreference.getBoolean(SETTING_NETWORK_DOH_ENABLED, true),
                endpoint = mmkvPreference.getString(
                    SETTING_NETWORK_DOH_ENDPOINT,
                    DEFAULT_NETWORK_DOH_ENDPOINT,
                ).orEmpty(),
                upstream = mmkvPreference.getString(
                    SETTING_NETWORK_DOH_UPSTREAM,
                    DEFAULT_NETWORK_DOH_UPSTREAM,
                ).orEmpty(),
            )
        }
    }
}

private data class DohResolverCache(
    val snapshot: DohPreferenceSnapshot,
    val dns: Dns,
)

private fun String.toDohUrlOrNull(fallback: String): HttpUrl? {
    val value = trim().ifBlank { fallback }
    val normalized = when {
        value.startsWith("https://") -> value
        value.startsWith("http://") -> "https://${value.removePrefix("http://")}"
        else -> "https://$value"
    }

    return normalized.toHttpUrlOrNull()?.takeIf {
        it.isHttps && it.encodedPath != "/"
    }
}

class ConfigurableDohDns(
    private val timeoutSeconds: Long = 10,
) : Dns {
    @Volatile
    private var cache: DohResolverCache? = null

    override fun lookup(hostname: String): List<InetAddress> {
        return currentDns().lookup(hostname)
    }

    private fun currentDns(): Dns {
        val snapshot = DohPreferenceSnapshot.fromPreferences()
        val cached = cache
        if (cached?.snapshot == snapshot) {
            return cached.dns
        }

        synchronized(this) {
            val synchronizedCache = cache
            if (synchronizedCache?.snapshot == snapshot) {
                return synchronizedCache.dns
            }

            val dns = buildDns(snapshot)
            cache = DohResolverCache(snapshot = snapshot, dns = dns)
            return dns
        }
    }

    private fun buildDns(snapshot: DohPreferenceSnapshot): Dns {
        if (!snapshot.enabled) {
            return Dns.SYSTEM
        }

        val upstreamUrl = snapshot.upstream.toDohUrlOrNull(DEFAULT_NETWORK_DOH_UPSTREAM)
            ?: return Dns.SYSTEM
        val endpointUrl = snapshot.endpoint.toDohUrlOrNull(DEFAULT_NETWORK_DOH_ENDPOINT)
            ?: return Dns.SYSTEM

        val upstreamDns = DnsOverHttps.Builder()
            .client(baseClient())
            .url(upstreamUrl)
            .build()

        return DnsOverHttps.Builder()
            .client(baseClient(dns = upstreamDns))
            .url(endpointUrl)
            .build()
    }

    private fun baseClient(dns: Dns = Dns.SYSTEM): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(dns)
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build()
    }
}