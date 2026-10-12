package me.rerere.awara.util

import java.net.UnknownHostException
import org.junit.Assert.assertThrows
import org.junit.Test

class StrictDohDnsTest {
    @Test fun rejectsPlaintextSchemes() {
        assertThrows(UnknownHostException::class.java) {
            strictDohUrl("http://doh.opendns.com/dns-query", "upstream")
        }
    }

    @Test fun rejectsMissingDohPath() {
        assertThrows(UnknownHostException::class.java) {
            strictDohUrl("https://doh.opendns.com/", "upstream")
        }
    }

    @Test fun rejectsUnknownBootstrapWithoutNetwork() {
        val dns = ConfigurableDohDns(snapshotProvider = {
            NetworkStackSnapshot(
                dohEnabled = true,
                dohEndpoint = "doh.opendns.com/dns-query",
                dohUpstream = "untrusted.example/dns-query",
            )
        })
        assertThrows(UnknownHostException::class.java) { dns.lookup("example.com") }
    }

    @Test fun rejectsInvalidEndpointWithoutNetwork() {
        val dns = ConfigurableDohDns(snapshotProvider = {
            NetworkStackSnapshot(
                dohEnabled = true,
                dohEndpoint = "http://example.org/dns-query",
                dohUpstream = "doh.opendns.com/dns-query",
            )
        })
        assertThrows(UnknownHostException::class.java) { dns.lookup("example.com") }
    }
}
