package com.openflow.android

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Push-to-talk wrapper around Android's SpeechRecognizer.
 *
 * SpeechRecognizer stops by itself after a pause, so while the user is still holding / latched we
 * restart it and join the pieces. All callbacks arrive on the main thread.
 */
class Dictation(private val ctx: Context, private val cb: Callback) : SpeechEngine {
    interface Callback {
        fun onLevel(level: Float)
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(message: String)
    }

    private val main = Handler(Looper.getMainLooper())
    private var rec: SpeechRecognizer? = null
    private val segments = mutableListOf<String>()
    private var active = false      // the user still wants to record
    private var finishing = false   // the user let go; waiting for the last result
    private val deliverRunnable = Runnable { deliver() }

    // The recognizer plays a beep every time it starts or stops. We can't turn that off, so we mute the
    // streams it uses for the length of the dictation and put them back afterwards.
    private val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mutedStreams = mutableListOf<Int>()
    private val unmuteRunnable = Runnable { unmuteSounds() }

    private fun muteSounds() {
        main.removeCallbacks(unmuteRunnable)
        if (!Prefs(ctx).muteSounds || mutedStreams.isNotEmpty()) return
        for (stream in intArrayOf(AudioManager.STREAM_MUSIC, AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_SYSTEM)) {
            try {
                if (!audio.isStreamMute(stream)) {   // never touch something the user had already muted
                    audio.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0)
                    mutedStreams.add(stream)
                }
            } catch (_: Exception) {}                 // some phones refuse (for example in Do Not Disturb)
        }
    }

    private fun unmuteSounds() {
        for (stream in mutedStreams) try { audio.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0) } catch (_: Exception) {}
        mutedStreams.clear()
    }

    /** A short delay so the final "done" beep is still covered. */
    private fun scheduleUnmute() {
        main.removeCallbacks(unmuteRunnable)
        main.postDelayed(unmuteRunnable, 400)
    }

    override fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            cb.onError("No speech recognition service on this phone")
            return
        }
        destroy()
        muteSounds()
        segments.clear()
        active = true
        finishing = false
        // Online recognition (Google) is more accurate than the offline one, so it is the default.
        val offline = !Prefs(ctx).onlineSpeech
        rec = if (offline && Build.VERSION.SDK_INT >= 33 && SpeechRecognizer.isOnDeviceRecognitionAvailable(ctx)) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(ctx)
        } else {
            SpeechRecognizer.createSpeechRecognizer(ctx)
        }
        rec?.setRecognitionListener(listener)
        listen()
    }

    /** The user let go: finish and deliver what we have. */
    override fun stop() {
        if (!active) return
        active = false
        finishing = true
        rec?.stopListening()
        main.postDelayed(deliverRunnable, 2500) // safety net if no final result arrives
    }

    override fun cancel() {
        active = false
        finishing = false
        main.removeCallbacks(deliverRunnable)
        destroy()
        scheduleUnmute()
    }

    private fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, !Prefs(ctx).onlineSpeech)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.packageName)
        rec?.startListening(intent)
    }

    private fun deliver() {
        if (!finishing) return
        finishing = false
        main.removeCallbacks(deliverRunnable)
        val text = segments.joinToString(" ").trim()
        destroy()
        scheduleUnmute()
        cb.onFinal(text)
    }

    private fun destroy() {
        rec?.setRecognitionListener(null)
        rec?.destroy()
        rec = null
    }

    private fun best(b: Bundle?): String? =
        b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onRmsChanged(rmsdB: Float) {
            cb.onLevel(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val p = best(partialResults) ?: return
            cb.onPartial((segments + p).joinToString(" "))
        }

        override fun onResults(results: Bundle?) {
            val r = best(results)
            if (!r.isNullOrBlank()) segments.add(r)
            if (active) main.postDelayed({ if (active) listen() }, 80) else deliver()
        }

        override fun onError(error: Int) {
            if (active) {
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                        main.postDelayed({ if (active) listen() }, 150)
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        active = false
                        scheduleUnmute()
                        cb.onError("Microphone permission missing. Open the OpenFlow app.")
                    }
                    else -> {
                        active = false
                        scheduleUnmute()
                        cb.onError("Speech recognition error ($error)")
                    }
                }
            } else {
                deliver()
            }
        }
    }
}
