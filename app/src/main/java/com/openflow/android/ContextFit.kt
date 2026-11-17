package com.openflow.android

/** Uses the text before the cursor to match capitalisation and spacing (port of contextfit.py). */
object ContextFit {
    private const val SENTENCE_END = ".!?\n"
    private const val NO_SPACE_BEFORE = ".,!?;:)]}\"'”’%"
    private const val NO_SPACE_AFTER = "([{“‘\"'/-@#\n\t "
    private val FIRST_WORD = Regex("""^[\w'’]+""")

    private fun lowerFirst(text: String, protectedWords: Set<String>): String {
        val word = FIRST_WORD.find(text)?.value ?: return text
        if (word.split('\'')[0].split('’')[0] == "I") return text      // I, I'm, I'll
        if (word.drop(1).any { it.isUpperCase() } || word in protectedWords) return text  // NASA, iPhone
        return text.substring(0, 1).lowercase() + text.substring(1)
    }

    /** before == null means "unknown" (password field, no access): text is returned unchanged. */
    fun fit(text: String, before: String?, protectedWords: Set<String> = emptySet()): String {
        if (text.isEmpty() || before == null || before.isEmpty() || before.isBlank()) return text
        var t = text
        val last = before.trimEnd(' ', '\t').lastOrNull()
        if (last == null || SENTENCE_END.indexOf(last) < 0) t = lowerFirst(t, protectedWords)
        if (NO_SPACE_AFTER.indexOf(before.last()) < 0 && NO_SPACE_BEFORE.indexOf(t[0]) < 0) t = " $t"
        return t
    }
}
