package com.openflow.android

/**
 * Many apps show grey hint text in an empty box ("Message" in WhatsApp). Android sometimes reports that
 * hint as if it were real text, so OpenFlow must recognise it and treat the box as empty.
 */
object Placeholder {
    /** Common hints. The Settings screen lets you add your own. Matched exactly, with the capitals shown. */
    const val DEFAULTS = "Message\nType a message\nWrite a message\nWrite a reply\nReply\nAdd a comment\nSay something\nSearch"

    fun parse(text: String): Set<String> =
        text.lines().map { normalise(it) }.filter { it.isNotEmpty() }.toSet()

    /** "Add a comment..." and "Add a comment…" count the same as "Add a comment". */
    private fun normalise(s: String) = s.trim().trimEnd('.', '…').trim()

    /**
     * True when [text] is really the box's hint and the box is empty.
     * [selStart]/[selEnd] are the reported cursor (-1 when the app doesn't say).
     */
    fun isHint(
        text: String,
        hint: String?,
        showingHint: Boolean,
        selStart: Int,
        selEnd: Int,
        known: Set<String>
    ): Boolean {
        if (text.isEmpty()) return false
        if (showingHint) return true
        // A cursor strictly inside the text means somebody typed it: never a hint.
        if (selStart > 0 && selStart == selEnd && selStart < text.length) return false
        if (hint != null && normalise(hint) == normalise(text)) return true
        return normalise(text) in known
    }
}
