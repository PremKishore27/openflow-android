package com.openflow.android

import android.content.Context
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** One downloadable file of a speech model. [sha256] is null for small text files. */
data class ModelFile(val name: String, val bytes: Long, val sha256: String?)

/** An English-only Whisper model in sherpa-onnx's int8 format (MIT licence, hosted on Hugging Face). */
data class ModelSpec(
    val id: String,
    val title: String,
    val blurb: String,
    val repo: String,
    val encoder: ModelFile,
    val decoder: ModelFile,
    val tokens: ModelFile
) {
    val files get() = listOf(encoder, decoder, tokens)
    val totalBytes get() = files.sumOf { it.bytes }
    val totalMb get() = (totalBytes / 1_000_000L).toInt()
}

object Models {
    val SMALL = ModelSpec(
        id = "small.en",
        title = "Whisper Small",
        blurb = "Best accuracy",
        repo = "csukuangfj/sherpa-onnx-whisper-small.en",
        encoder = ModelFile("small.en-encoder.int8.onnx", 112_442_483L, "8bdac288f369aa94ee2194059238c465ed82ea9d47ee8fa4a8c0a891873e462f"),
        decoder = ModelFile("small.en-decoder.int8.onnx", 262_223_042L, "710ccf890e10f3faa15f51ec346081a2723c9f3adb6e4da81c6573a5a6f877fb"),
        tokens = ModelFile("small.en-tokens.txt", 835_554L, null)
    )

    val BASE = ModelSpec(
        id = "base.en",
        title = "Whisper Base",
        blurb = "Faster, a little less accurate",
        repo = "csukuangfj/sherpa-onnx-whisper-base.en",
        encoder = ModelFile("base.en-encoder.int8.onnx", 29_120_534L, "ef6b936f4c9b1d90a3b68634b60c4ed8576b26172b33c2535ec0e933c9edb823"),
        decoder = ModelFile("base.en-decoder.int8.onnx", 130_669_978L, "f7162ad6db2dbef16cfaeaa7f945b9d7dd9c1b8d472f6aca82f2273d185e4d41"),
        tokens = ModelFile("base.en-tokens.txt", 835_554L, null)
    )

    val all = listOf(SMALL, BASE)

    fun byId(id: String) = all.firstOrNull { it.id == id } ?: SMALL

    fun dir(ctx: Context, spec: ModelSpec) = File(ctx.filesDir, "models/${spec.id}")

    fun file(ctx: Context, spec: ModelSpec, f: ModelFile) = File(dir(ctx, spec), f.name)

    /** True when every file is present at its exact expected size (checksums are verified when downloaded). */
    fun isReady(ctx: Context, spec: ModelSpec) =
        spec.files.all { f -> file(ctx, spec, f).let { it.isFile && it.length() == f.bytes } }

    fun delete(ctx: Context, spec: ModelSpec) {
        LocalWhisper.release()
        dir(ctx, spec).deleteRecursively()
    }
}

/**
 * Downloads a model in the background, with resume and SHA-256 checks. One download at a time.
 * Progress is read by the settings screen. It keeps running after the screen is closed.
 */
object ModelDownloader {
    @Volatile var running = false; private set
    @Volatile var activeId: String? = null; private set
    @Volatile var doneBytes = 0L; private set
    @Volatile var totalBytes = 0L; private set
    @Volatile var error: String? = null; private set
    @Volatile private var cancelRequested = false

    fun cancel() { cancelRequested = true }

    @Synchronized
    fun start(ctx: Context, spec: ModelSpec) {
        if (running) return
        val app = ctx.applicationContext
        running = true
        cancelRequested = false
        error = null
        activeId = spec.id
        totalBytes = spec.totalBytes
        doneBytes = 0L
        Thread {
            try {
                val dir = Models.dir(app, spec).also { it.mkdirs() }
                val needed = spec.totalBytes * 11 / 10
                if (dir.usableSpace < needed) {
                    error = "Not enough free storage (need about ${needed / 1_000_000} MB)."
                    return@Thread
                }
                for (f in spec.files) {
                    if (!fetch(spec, f, dir)) return@Thread
                }
            } catch (e: Exception) {
                error = "Download failed: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                running = false
            }
        }.start()
    }

    /** Returns false on failure or cancel (and sets [error] when it was a failure). */
    private fun fetch(spec: ModelSpec, f: ModelFile, dir: File): Boolean {
        val dest = File(dir, f.name)
        if (dest.isFile && dest.length() == f.bytes) { doneBytes += f.bytes; return true }
        val part = File(dir, f.name + ".part")
        var have = if (part.isFile) part.length() else 0L
        if (have > f.bytes) { part.delete(); have = 0L }

        val url = "https://huggingface.co/${spec.repo}/resolve/main/${f.name}"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            if (have > 0) setRequestProperty("Range", "bytes=$have-")
        }
        try {
            val code = conn.responseCode
            if (code != 200 && code != 206) { error = "Download failed (HTTP $code)."; return false }
            if (code == 200) have = 0L // the server ignored the range: start over
            RandomAccessFile(part, "rw").use { raf ->
                if (have == 0L) raf.setLength(0)
                raf.seek(have)
                val buf = ByteArray(256 * 1024)
                var written = have
                conn.inputStream.use { input ->
                    while (true) {
                        if (cancelRequested) { error = null; return false }
                        val n = input.read(buf)
                        if (n < 0) break
                        raf.write(buf, 0, n)
                        written += n
                        doneBytes = doneBeforeThisFile(spec, f) + written
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        if (part.length() != f.bytes) { error = "Download was cut short. Tap to resume."; return false }
        if (f.sha256 != null && sha256Of(part) != f.sha256) {
            part.delete()
            error = "Checksum mismatch. The download was discarded; try again."
            return false
        }
        if (!part.renameTo(dest)) { error = "Couldn't save the model file."; return false }
        return true
    }

    private fun doneBeforeThisFile(spec: ModelSpec, f: ModelFile): Long {
        var sum = 0L
        for (x in spec.files) { if (x == f) break; sum += x.bytes }
        return sum
    }

    private fun sha256Of(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(1024 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
