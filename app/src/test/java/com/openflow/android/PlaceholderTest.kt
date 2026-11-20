package com.openflow.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceholderTest {
    private val known = Placeholder.parse(Placeholder.DEFAULTS)

    @Test fun flaggedHintIsAHint() =
        assertTrue(Placeholder.isHint("Anything", null, true, -1, -1, known))

    @Test fun whatsAppMessageHintIsRecognisedWhateverTheCursorSays() {
        assertTrue(Placeholder.isHint("Message", null, false, -1, -1, known))   // cursor not reported
        assertTrue(Placeholder.isHint("Message", null, false, 0, 0, known))     // cursor at start
        assertTrue(Placeholder.isHint("Message", null, false, 7, 7, known))     // cursor reported at the end
    }

    @Test fun textEqualToTheBoxsOwnHintIsAHint() =
        assertTrue(Placeholder.isHint("Ask me anything", "Ask me anything", false, -1, -1, known))

    @Test fun trailingDotsAreIgnored() {
        assertTrue(Placeholder.isHint("Add a comment…", null, false, 0, 0, known))
        assertTrue(Placeholder.isHint("Add a comment...", null, false, -1, -1, known))
    }

    @Test fun realTextIsNeverAHint() {
        assertFalse(Placeholder.isHint("Hello there", null, false, 11, 11, known))
        assertFalse(Placeholder.isHint("message", null, false, 7, 7, known))    // typed in lower case
        assertFalse(Placeholder.isHint("Message", null, false, 3, 3, known))    // cursor in the middle: typed
        assertFalse(Placeholder.isHint("", "Message", true, 0, 0, known))
    }

    @Test fun yourOwnPlaceholdersCanBeAdded() {
        val mine = Placeholder.parse("Message\nWhat's on your mind?\n\n  ")
        assertTrue(Placeholder.isHint("What's on your mind?", null, false, -1, -1, mine))
        assertEquals(2, mine.size)
    }
}
