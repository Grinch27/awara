package me.rerere.awara.util

import java.net.InetAddress
import java.net.UnknownHostException
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
const val DEFAULT_NETWORK_DOH_UPSTREAM = DEFAULT_NETWORK_DOH_ENDPOINT

// This list is intentionally explicit. We must never resolve a DoH bootstrap host using Dns.SYSTEM.
// Cisco publishes doh.opendns.com -> 146.112.41.2 / 2620:119:fc::2.
// Other providers are included only as optional upstream choices.
private val pinnedBootstrapAddresses: Map<String, List<InetAddress>> by lazy {
    mapOf(
        "doh.opendns.com" to listOf("146.112.41.2", "2620:119:fc::2"),
        "dns.google" to listOf("8.8.8.8", "8.8.4.4", "2001:4860:4860::8888"),
        "cloudflare-dns.com" to listOf("1.1.1.1", "1.0.0.1", "2606:4700:4700::1111"),
        "dns.quad9.net" to listOf("9.9.9.9", "149.112.112.112", "2620:fe::fe"),
    ).mapValues { (_, literals) ->
        // InetAddress.getByName with numeric IP literals never performs hostname resolution.
        literals.map(InetAddress::getByName)
    }
}

data class NetworkStackSnapshot(
    val dohEnabled: Boolean,
    val dohEndpoint: String,
    val dohUpstream: String,
) {
    companion object {
        fun fromPreferences(): NetworkStackSnapshot {
            return NetworkStackSnapshot(
                dohEnabled = mmkvPreference.getBoolean(SETTING_NETWORK_DOH_ENABLED, true),
                dohEndpoint = mmkvPreference.getString(
                    SETTING_NETWORK_DOH_ENDPOINT,
                    DEFAULT_NETWORK_DOH_ENDPOINT,
                ).orEmpty(),
                dohUpstream = mmkvPreference.getString(
                    SETTING_NETWORK_DOH_UPSTREAM,
                    DEFAULT_NETWORK_DOH_UPSTREAM,
                ).orEmpty(),
            )
        }
    }
}

interface NetworkTransportPolicy {
    fun currentSnapshot(): NetworkStackSnapshot
    fun apply(builder: OkHttpClient.Builder): OkHttpClient.Builder
}

private data class DohResolverCache(
    val snapshot: NetworkStackSnapshot,
    val dns: Dns,
)

internal fun strictDohUrl(value: String, field: String): HttpUrl {
    val trimmed = value.trim()
    if (trimmed.isBlank() || trimmed.startsWith("http://", ignoreCase = true)) {
        throw UnknownHostException("$field must be a non-empty HTTPS DoH endpoint")
    }
    val url = (if (trimmed.startsWith("https://", ignoreCase = true)) trimmed else "https://$trimmed")
        .toHttpUrlOrNull()
    if (url == null || !url.isHttps || url.encodedPath == "/" ||
        url.username.isNotEmpty() || url.password.isNotEmpty() ||
        url.query != null || url.fragment != null) {
        throw UnknownHostException("$field is not a valid HTTPS DoH endpoint")
    }
    return url
}

class ConfigurableDohDns(
    private val timeoutSeconds: Long = 10,
    private val snapshotProvider: () -> NetworkStackSnapshot = NetworkStackSnapshot::fromPreferences,
) : Dns {
    @Volatile
    private var cache: DohResolverCache? = null

    override fun lookup(hostname: String): List<InetAddress> = currentDns().lookup(hostname)

    private fun currentDns(): Dns {
        val snapshot = snapshotProvider()
        val cached = cache
        if (cached?.snapshot == snapshot) return cached.dns

        synchronized(this) {
            val synchronizedCache = cache
            if (synchronizedCache?.snapshot == snapshot) return synchronizedCache.dns
            return buildDns(snapshot).also {
                cache = DohResolverCache(snapshot, it)
            }
        }
    }

    private fun buildDns(snapshot: NetworkStackSnapshot): Dns {
        // The only path allowed to use system DNS is an explicit user opt-out.
        if (!snapshot.dohEnabled) return Dns.SYSTEM

        val upstreamUrl = strictDohUrl(snapshot.dohUpstream, "DoH upstream")
        val endpointUrl = strictDohUrl(snapshot.dohEndpoint, "DoH endpoint")
        val bootstrap = pinnedBootstrapAddresses[upstreamUrl.host]
            ?: throw UnknownHostException(
                "No trusted bootstrap IP is configured for ${upstreamUrl.host}; DoH fails closed"
            )

        // Reject any DNS lookup that is not for the approved bootstrap hostname.
        val bootstrapOnly = Dns { hostname ->
            if (hostname.equals(upstreamUrl.host, ignoreCase = true)) bootstrap
            else throw UnknownHostException("Plaintext DNS bootstrap denied: $hostname")
        }
        val upstreamDns = DnsOverHttps.Builder()
            .client(baseClient(bootstrapOnly))
            .url(upstreamUrl)
            .bootstrapDnsHosts(bootstrap)
            .build()

        // The upstream DoH resolver is already sufficient when both URLs are identical.
        if (endpointUrl == upstreamUrl) return upstreamDns

        // Resolve a distinct endpoint through the encrypted upstream. Set systemDns
        // explicitly: DnsOverHttps.Builder must not silently replace the client's DNS.
        // TLS validation still uses endpointUrl.host, never the bootstrap IP.
        return DnsOverHttps.Builder()
            .client(baseClient(upstreamDns))
            .url(endpointUrl)
            .systemDns(upstreamDns)
            .build()
    }

    private fun baseClient(dns: Dns): OkHttpClient = OkHttpClient.Builder()
        .dns(dns)
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build()
}

class AppNetworkTransportPolicy(
    private val dns: ConfigurableDohDns,
) : NetworkTransportPolicy {
    override fun currentSnapshot(): NetworkStackSnapshot = NetworkStackSnapshot.fromPreferences()

    override fun apply(builder: OkHttpClient.Builder): OkHttpClient.Builder = builder.dns(dns)
}
