package com.openflow.android

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import kotlin.math.abs
import kotlin.math.sqrt

/** On-device Whisper (English) through sherpa-onnx. Loaded once and kept for fast repeat dictations. */
object LocalWhisper {
    private const val SAMPLE_RATE = 16_000
    private const val MAX_CHUNK = 28 * SAMPLE_RATE      // Whisper works on up to 30 s at a time

    private var recognizer: OfflineRecognizer? = null
    private var loadedId: String? = null

    @Synchronized
    fun get(ctx: Context, spec: ModelSpec): OfflineRecognizer {
        recognizer?.let { if (loadedId == spec.id) return it }
        release()
        val whisper = OfflineWhisperModelConfig(
            encoder = Models.file(ctx, spec, spec.encoder).path,
            decoder = Models.file(ctx, spec, spec.decoder).path,
            language = "en",
            task = "transcribe"
        )
        val model = OfflineModelConfig(
            whisper = whisper,
            tokens = Models.file(ctx, spec, spec.tokens).path,
            numThreads = Runtime.getRuntime().availableProcessors().coerceIn(2, 4),
            debug = false,
            provider = "cpu",
            modelType = "whisper"
        )
        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
            modelConfig = model,
            decodingMethod = "greedy_search"
        )
        return OfflineRecognizer(config = config).also {
            recognizer = it
            loadedId = spec.id
        }
    }

    @Synchronized
    fun release() {
        recognizer?.release()
        recognizer = null
        loadedId = null
    }

    /** Blocking. Call from a background thread. Returns "" for silence. */
    @Synchronized
    fun transcribe(ctx: Context, spec: ModelSpec, samples: FloatArray): String {
        if (samples.size < SAMPLE_RATE / 3) return ""          // shorter than 0.33 s
        if (peak(samples) < 0.008f) return ""                  // silence: Whisper would invent words
        val rec = get(ctx, spec)
        val parts = ArrayList<String>()
        for (chunk in split(samples)) {
            val stream = rec.createStream()
            try {
                stream.acceptWaveform(chunk, SAMPLE_RATE)
                rec.decode(stream)
                val text = rec.getResult(stream).text.trim()
                if (text.isNotEmpty()) parts.add(text)
            } finally {
                stream.release()
            }
        }
        return parts.joinToString(" ").trim()
    }

    private fun peak(a: FloatArray): Float {
        var p = 0f
        for (v in a) { val x = abs(v); if (x > p) p = x }
        return p
    }

    /** Splits long audio into chunks of at most 28 s, cutting at the quietest moment near the end of each. */
    fun split(samples: FloatArray): List<FloatArray> {
        if (samples.size <= MAX_CHUNK) return listOf(samples)
        val out = ArrayList<FloatArray>()
        var start = 0
        while (start < samples.size) {
            var end = minOf(start + MAX_CHUNK, samples.size)
            if (end < samples.size) end = quietestCut(samples, end - 4 * SAMPLE_RATE, end)
            out.add(samples.copyOfRange(start, end))
            start = end
        }
        return out
    }

    private fun quietestCut(a: FloatArray, from: Int, to: Int): Int {
        val win = SAMPLE_RATE / 10   // 100 ms
        var bestPos = to
        var bestRms = Float.MAX_VALUE
        var pos = maxOf(from, 0)
        while (pos + win <= to) {
            var sum = 0f
            for (i in pos until pos + win) sum += a[i] * a[i]
            val rms = sqrt(sum / win)
            if (rms < bestRms) { bestRms = rms; bestPos = pos + win / 2 }
            pos += win
        }
        return bestPos
    }
}
