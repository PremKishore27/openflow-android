package com.openflow.android

import org.junit.Assert.assertEquals
import org.junit.Test

class ContextFitTest {
    @Test fun unknownOrEmpty() {
        assertEquals("Hello there", ContextFit.fit("Hello there", null))
        assertEquals("Hello there", ContextFit.fit("Hello there", ""))
    }

    @Test fun midSentence() {
        assertEquals(" the report is ready.", ContextFit.fit("The report is ready.", "I think that"))
        assertEquals("the report", ContextFit.fit("The report", "I think that "))
    }

    @Test fun newSentence() {
        assertEquals("The report", ContextFit.fit("The report", "Done. "))
        assertEquals(" The report", ContextFit.fit("The report", "Done."))
        assertEquals("The report", ContextFit.fit("The report", "Line one\n"))
    }

    @Test fun keepsProperWords() {
        assertEquals(" I am here", ContextFit.fit("I am here", "so"))
        assertEquals(" NASA called", ContextFit.fit("NASA called", "so"))
        assertEquals(" iPhone sales", ContextFit.fit("iPhone sales", "so", setOf("iPhone")))
    }

    @Test fun punctuation() {
        assertEquals(", and then", ContextFit.fit(", and then", "first"))
        assertEquals("word", ContextFit.fit("Word", "see ("))
    }
}
