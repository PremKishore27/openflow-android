package com.openflow.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatesTest {
    @Test fun newerVersionsAreDetected() {
        assertTrue(Updates.isNewer("v0.10.0", "0.9"))
        assertTrue(Updates.isNewer("v1.0.0", "0.9.5"))
        assertTrue(Updates.isNewer("v0.9.1", "0.9"))
    }

    @Test fun sameOrOlderIsNotAnUpdate() {
        assertFalse(Updates.isNewer("v0.9.0", "0.9"))
        assertFalse(Updates.isNewer("v0.8.0", "0.9"))
        assertFalse(Updates.isNewer("v0.9", "0.9.0"))
    }

    @Test fun parsingIgnoresPrefixAndSuffix() {
        assertEquals(listOf(0, 10, 0), Updates.parse("v0.10.0"))
        assertEquals(listOf(1, 2), Updates.parse("1.2-beta"))
    }
}
