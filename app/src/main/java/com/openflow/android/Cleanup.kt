package com.openflow.android

/** Rule-based "auto-edit": a port of openflow/cleanup.py from the Mac app. */
object Cleanup {
    private val I = RegexOption.IGNORE_CASE

    private val FILLER = Regex("""\b(?:um+|uh+|uhm+|erm+|er|ah+|hmm+|mmm+)\b[,.]?\s*""", I)

    private val SPOKEN = listOf(
        "new paragraph" to "\n\n", "new line" to "\n", "newline" to "\n",
        "question mark" to "?", "exclamation mark" to "!", "exclamation point" to "!",
        "full stop" to ".", "period" to ".", "comma" to ",", "colon" to ":", "semicolon" to ";"
    )
    private val SPOKEN_RES = SPOKEN.map { (w, rep) ->
        Regex("""([,.?!]?)\s*\b""" + Regex.escape(w) + """\b[,.?!]?""", I) to rep
    }

    private val SCRATCH = Regex("""[,.]?\s*\b(?:scratch that|delete that|forget that)\b[,.]?\s*""", I)
    private val CORRECT = Regex("""[,;]?\s*\b(?:no,? wait|i mean|actually no)\b[,.]?\s*""", I)
    private val STUTTER = Regex("""\b(\w+)(?:\s+\1\b)+""", I)
    private val STUTTER_EXEMPT = setOf("had", "that")

    private val WS = Regex("[ \\t]+")
    private val NL_WS = Regex("[ \\t]*\\n[ \\t]*")
    private val SPACE_BEFORE_PUNCT = Regex("[ \\t]+([,.!?;:])")
    private val DUP_PUNCT = Regex("([,.!?;:])\\1+")
    private val LEADING_PUNCT = Regex("^[,.;:\\s]+")
    private val LONE_I = Regex("\\bi\\b(?!\\.)")
    private val SENTENCE_START = Regex("""(^|[.!?]\s+|\n)([a-z][\w']*)""")
    private val FIRST_WORD_BREAK = Regex("""^[^,.!?;\n]+""")
    private val WHITESPACE = Regex("\\s+")

    fun spokenPunctuation(text: String): String {
        var t = text
        for ((rx, rep) in SPOKEN_RES) {
            t = rx.replace(t) { m ->
                val lead = m.groupValues[1]
                if (rep.startsWith("\n")) (if (lead == "." || lead == "?" || lead == "!") lead else "") + rep else rep
            }
        }
        return t
    }

    /** "Meet at 3. Scratch that. Meet at 4." -> "Meet at 4." */
    fun scratchThat(text: String): String {
        var t = text
        while (true) {
            val m = SCRATCH.find(t) ?: return t
            val before = t.substring(0, m.range.first)
            val after = t.substring(m.range.last + 1)
            val idx = maxOf(before.lastIndexOf('.'), before.lastIndexOf('!'), before.lastIndexOf('?'), before.lastIndexOf('\n'))
            val kept = if (idx >= 0) before.substring(0, idx + 1) else ""
            t = (kept + " " + after).trim()
        }
    }

    /** "Send it to John, no wait, to Mike." -> "Send it to Mike." (the correction replaces as many words as it has) */
    fun selfCorrect(text: String): String {
        var t = text
        while (true) {
            val m = CORRECT.find(t) ?: return t
            val before = t.substring(0, m.range.first).trimEnd()
            val after = t.substring(m.range.last + 1)
            val mm = FIRST_WORD_BREAK.find(after)
            if (mm == null) {
                t = before + " " + after
                continue
            }
            val n = mm.value.trim().split(WHITESPACE).filter { it.isNotEmpty() }.size
            val words = before.split(" ").toMutableList()
            words[words.size - 1] = words.last().trimEnd(',', ';')
            val kept = if (n < words.size) words.subList(0, words.size - n) else emptyList()
            t = (kept.joinToString(" ") + " " + after).trim()
        }
    }

    fun removeFillers(text: String) = FILLER.replace(text, "")

    fun removeStutters(text: String) = STUTTER.replace(text) { m ->
        val w = m.groupValues[1]
        if (w.lowercase() in STUTTER_EXEMPT || w.all { it.isDigit() }) m.value else w
    }

    fun tidy(text: String, protectedWords: Set<String> = emptySet()): String {
        var t = text
        t = WS.replace(t, " ")
        t = NL_WS.replace(t, "\n")
        t = SPACE_BEFORE_PUNCT.replace(t, "\$1")
        t = DUP_PUNCT.replace(t, "\$1")
        t = LEADING_PUNCT.replace(t, "")
        t = LONE_I.replace(t, "I")
        t = SENTENCE_START.replace(t) { m ->
            val w = m.groupValues[2]
            if (w in protectedWords) m.value else m.groupValues[1] + w.substring(0, 1).uppercase() + w.substring(1)
        }
        return t.trim()
    }

    private fun replacePhrases(text: String, mapping: Map<String, String>): String {
        var t = text
        for (src in mapping.keys.sortedByDescending { it.length }) {
            val dst = mapping.getValue(src)
            t = Regex("""\b""" + Regex.escape(src) + """\b""", I).replace(t) { dst }
        }
        return t
    }

    fun applyStyle(text: String, style: String): String {
        if (style == "casual" && !text.contains('\n') && Regex("[.!?]").findAll(text).count() <= 1) {
            return text.replace(Regex("""\.$"""), "")
        }
        return text
    }

    fun clean(
        text: String,
        dictionary: Map<String, String> = emptyMap(),
        snippets: Map<String, String> = emptyMap(),
        style: String = "default",
        spoken: Boolean = true
    ): String {
        var t = text
        if (spoken) t = spokenPunctuation(t)
        t = scratchThat(t)
        t = selfCorrect(t)
        t = removeFillers(t)
        t = removeStutters(t)
        val protectedWords = (dictionary.values + snippets.values)
            .filter { it.isNotBlank() }
            .map { it.trim().split(WHITESPACE).first() }
            .toSet()
        t = tidy(t, protectedWords)
        t = replacePhrases(t, dictionary)
        t = replacePhrases(t, snippets)
        return applyStyle(t, style)
    }
}
