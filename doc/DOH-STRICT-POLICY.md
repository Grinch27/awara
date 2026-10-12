# Strict application DNS policy (P0-A/P0-B initial patch)

- DoH remains enabled by default and now fails closed on a malformed URL, a
  disabled DoH endpoint, or an unrecognized upstream bootstrap host.
- The only implicit resolver fallback left is the explicit user setting
  `setting.network_doh_enabled=false`.
- Supported pinned *bootstrap upstream* hostnames: doh.opendns.com,
  dns.google, cloudflare-dns.com, dns.quad9.net. TLS hostname and certificate
  checks are not disabled. The OpenDNS IPs are listed by Cisco:
  https://blogs.cisco.com/security/cisco-interop-discovery-of-designated-resolvers-protocol-implemented
- Custom endpoint hostnames are resolved through the encrypted upstream.
  Unsupported custom upstream hostnames fail closed until an authenticated
  bootstrap IP is explicitly supported. IPv6-only bootstrap and CDN routing
  must still be tested on a real network.
- API metadata, Coil and WorkManager downloads share the application DNS
  transport policy. Bearer tokens are only injected for `https://api.iwara.tv`,
  never for unrelated CDN or lookalike hosts.
- IMPORTANT INCOMPLETE COVERAGE: legacy ExoPlayer 2 `DefaultHttpDataSource`
  is NOT YET connected to this DNS policy. Media3 migration P2-B must replace
  its transport with the shared public OkHttpClient. Other system components
  and DLNA are not controlled by this application-level policy.
- Manual validation must observe actual DNS and traffic; JVM unit tests
  cover invalid config and token host checks, not packet-level leak proof.
