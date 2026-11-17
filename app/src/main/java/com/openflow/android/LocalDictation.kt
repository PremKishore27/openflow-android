package com.openflow.android

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import kotlin.math.sqrt

/**
 * Records from the microphone while you speak, then transcribes it with on-device Whisper when you tap ✓.
 * Whisper works on a finished recording, so there is no live text; the waveform shows you're being heard.
 */
class LocalDictation(
    private val ctx: Context,
    private val cb: Dictation.Callback,
    private val spec: ModelSpec
) : SpeechEngine {
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var recording = false
    @Volatile private var cancelled = false
    private var recorder: Thread? = null
    private var samples = FloatArray(16_000 * 30)
    private var count = 0

    override fun start() {
        cancelled = false
        recording = true
        count = 0
        // Load the model while the user is still talking, so transcription starts right away.
        Thread { try { LocalWhisper.get(ctx, spec) } catch (_: Throwable) {} }.start()
        recorder = Thread { record() }.also { it.start() }
    }

    private fun record() {
        val rate = 16_000
        val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bufSize = maxOf(minBuf, rate / 2)
        val rec = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufSize)
        } catch (e: Exception) {
            main.post { if (!cancelled) cb.onError("Couldn't open the microphone") }
            return
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            main.post { if (!cancelled) cb.onError("Couldn't open the microphone") }
            return
        }
        val buf = ShortArray(rate / 10)   // 100 ms
        val limit = rate * 60 * 10        // keep at most 10 minutes
        try {
            rec.startRecording()
            while (recording) {
                val n = rec.read(buf, 0, buf.size)
                if (n <= 0) continue
                var sum = 0.0
                for (i in 0 until n) {
                    val v = buf[i] / 32768f
                    sum += v * v
                    if (count < limit) {
                        if (count == samples.size) samples = samples.copyOf(samples.size * 2)
                        samples[count++] = v
                    }
                }
                val level = (sqrt(sum / n).toFloat() * 8f).coerceIn(0f, 1f)
                main.post { if (recording && !cancelled) cb.onLevel(level) }
            }
        } finally {
            try { rec.stop() } catch (_: Exception) {}
            rec.release()
        }
    }

    override fun stop() {
        recording = false
        Thread {
            recorder?.join(2000)
            if (cancelled) return@Thread
            try {
                val text = LocalWhisper.transcribe(ctx, spec, samples.copyOf(count))
                if (!cancelled) main.post { cb.onFinal(text) }
            } catch (e: Throwable) {
                if (!cancelled) main.post { cb.onError("Whisper failed: ${(e.message ?: e.javaClass.simpleName).take(60)}") }
            }
        }.start()
    }

    override fun cancel() {
        cancelled = true
        recording = false
    }
}
