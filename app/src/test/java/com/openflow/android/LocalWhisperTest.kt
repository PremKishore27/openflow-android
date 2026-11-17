package com.openflow.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalWhisperTest {
    private val rate = 16_000

    @Test fun shortAudioIsNotSplit() {
        val a = FloatArray(rate * 20) { 0.3f }
        assertEquals(1, LocalWhisper.split(a).size)
    }

    @Test fun longAudioIsSplitWithinLimitAndKeepsEverySample() {
        val a = FloatArray(rate * 70) { 0.3f }
        val chunks = LocalWhisper.split(a)
        assertTrue(chunks.size >= 3)
        assertTrue(chunks.all { it.size <= 28 * rate })
        assertEquals(a.size, chunks.sumOf { it.size })
    }

    @Test fun cutsAtTheQuietestMomentNearTheLimit() {
        val a = FloatArray(rate * 60) { 0.3f }
        // a pause from 25.0 s to 25.5 s, inside the last 4 s before the 28 s limit
        for (i in (25 * rate) until (25 * rate + rate / 2)) a[i] = 0f
        val first = LocalWhisper.split(a).first().size
        assertTrue("cut at $first", first in (25 * rate)..(25 * rate + rate / 2))
    }
}
