package com.openflow.android

/** Anything that can listen and hand back text through [Dictation.Callback]. */
interface SpeechEngine {
    fun start()
    /** The user tapped ✓: finish and deliver the text. */
    fun stop()
    /** The user tapped ✕ or the service is going away: drop everything. */
    fun cancel()
}
