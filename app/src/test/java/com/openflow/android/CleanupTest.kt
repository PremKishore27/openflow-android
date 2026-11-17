package com.openflow.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanupTest {
    @Test fun fillers() = assertEquals("So, I think we should go", Cleanup.clean("So, um, I think uh we should go"))

    @Test fun stutters() {
        assertEquals("I want to go", Cleanup.clean("i want want to go"))
        assertEquals("He had had enough", Cleanup.clean("he had had enough"))
    }

    @Test fun spokenPunctuation() {
        assertEquals("Hello, how are you?", Cleanup.clean("hello comma how are you question mark"))
        assertEquals("First line\nSecond line", Cleanup.clean("first line new line second line"))
        assertEquals("Done.", Cleanup.clean("done period"))
        assertTrue(Cleanup.clean("a period of time", spoken = false).contains("period"))
    }

    @Test fun scratchThat() {
        assertEquals("I'm home. Meet at 4.", Cleanup.clean("I'm home. Meet at 3. Scratch that. Meet at 4."))
        assertEquals("Buy eggs.", Cleanup.clean("Buy milk, scratch that, buy eggs."))
    }

    @Test fun selfCorrection() {
        assertEquals("Meet at 4.", Cleanup.clean("Meet at 3, no wait, 4."))
        assertEquals("Send it to Mike.", Cleanup.clean("Send it to John, no wait, to Mike."))
    }

    @Test fun dictionaryAndSnippets() {
        assertEquals("iPhone sales are up", Cleanup.clean("iphone sales are up", mapOf("iphone" to "iPhone")))
        assertEquals("Send it to a@b.de", Cleanup.clean("Send it to my email", snippets = mapOf("my email" to "a@b.de")))
    }

    @Test fun casualStyle() {
        assertEquals("Sounds good", Cleanup.clean("sounds good.", style = "casual"))
        assertEquals("Sounds good. See you.", Cleanup.clean("Sounds good. See you.", style = "casual"))
    }
}
