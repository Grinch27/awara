package me.rerere.awara.di

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedIwaraHostTest {
    @Test fun acceptsTheRealDomain() {
        assertTrue(isTrustedIwaraHost("iwara.tv"))
        assertTrue(isTrustedIwaraHost("api.iwara.tv"))
    }
    @Test fun rejectsLookalikeDomains() {
        assertFalse(isTrustedIwaraHost("api.iwara.tv.attacker.example"))
        assertFalse(isTrustedIwaraHost("notiwara.tv"))
        assertFalse(isTrustedIwaraHost("iwara.tv.evil"))
    }
}
