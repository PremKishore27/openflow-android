package com.openflow.android

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.LinearLayout
import android.widget.TextView

/**
 * OpenFlow as a floating overlay (Wispr Flow style) instead of a keyboard.
 *
 * A small dark pill floats above whatever keyboard is open whenever a text field is focused.
 * Hold the pill to talk, or tap it to go hands-free (then tap the check mark). The cleaned text is
 * written straight into the focused field, so your normal keyboard stays in place.
 */
class OpenFlowService : AccessibilityService(), Dictation.Callback {
    private enum class State { IDLE, LISTENING, WORKING }

    private lateinit var prefs: Prefs
    private lateinit var dictation: Dictation
    private lateinit var wm: WindowManager
    private val main = Handler(Looper.getMainLooper())

    private lateinit var root: LinearLayout
    private lateinit var bubble: TextView
    private lateinit var wave: WaveformView
    private lateinit var cancel: TextView
    private lateinit var done: TextView
    private lateinit var editChip: TextView
    private lateinit var undoChip: TextView
    private lateinit var params: WindowManager.LayoutParams
    private var shown = false

    private var state = State.IDLE
    private var mode = "dictate"
    private var latched = false
    private var ignoreUp = false
    private var downAt = 0L
    private var pendingSelection = ""
    private var target: AccessibilityNodeInfo? = null
    private var pkg = ""
    private var lastStart = -1
    private var lastInserted: String? = null

    private val hideRunnable = Runnable { if (state == State.IDLE) hide() }
    private val refreshRunnable = Runnable { refresh() }
    private val clearBubble = Runnable { setBubble("") }
    private val hideUndo = Runnable { undoChip.visibility = View.GONE }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    // ---------- lifecycle ----------
    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = Prefs(this)
        dictation = Dictation(this, this)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        buildOverlay()
    }

    override fun onInterrupt() { abort() }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        abort()
        hide()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (::dictation.isInitialized) dictation.cancel()
        main.removeCallbacksAndMessages(null)
        if (::wm.isInitialized) hide()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!::root.isInitialized) return
        main.removeCallbacks(refreshRunnable)
        main.postDelayed(refreshRunnable, 80)
    }

    // ---------- overlay UI ----------
    private fun capsule(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), 0x33FFFFFF)
    }

    private fun circle(label: String, bg: Int) = TextView(this).apply {
        text = label
        textSize = 15f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(bg); setStroke(dp(1), 0x33FFFFFF) }
        layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply { setMargins(dp(3), 0, dp(3), 0) }
    }

    private fun buildOverlay() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        bubble = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            maxLines = 3
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = capsule(0xEE111111.toInt(), 16)
            visibility = View.GONE
            maxWidth = dp(280)
        }
        root.addView(bubble, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(8) })

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        editChip = circle("✎", 0xFF111111.toInt()).apply {
            visibility = View.GONE
            setOnClickListener { begin("edit", latch = true) }
        }
        undoChip = circle("↶", 0xFF111111.toInt()).apply {
            visibility = View.GONE
            setOnClickListener { undo() }
        }
        cancel = circle("✕", 0xFF3A3A3C.toInt()).apply {
            visibility = View.GONE
            setOnClickListener { abort(); setBubble("") }
        }
        done = circle("✓", 0xFF30D158.toInt()).apply {
            visibility = View.GONE
            setOnClickListener { finish() }
        }
        wave = WaveformView(this)
        val pill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = capsule(0xFF111111.toInt(), 22)
            setPadding(dp(5), dp(5), dp(5), dp(5))
        }
        pill.addView(cancel)
        pill.addView(wave, LinearLayout.LayoutParams(dp(44), dp(24)).apply { setMargins(dp(8), 0, dp(8), 0) })
        pill.addView(done)
        bindTalk(wave)

        row.addView(editChip)
        row.addView(pill, LinearLayout.LayoutParams(-2, -2).apply { setMargins(dp(4), 0, dp(4), 0) })
        row.addView(undoChip)
        root.addView(row)

        params = WindowManager.LayoutParams(
            -2, -2,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(96)
        }
    }

    private fun show() {
        main.removeCallbacks(hideRunnable)
        params.y = bottomOffset()
        if (!shown) {
            try { wm.addView(root, params); shown = true } catch (_: Exception) {}
        } else {
            try { wm.updateViewLayout(root, params) } catch (_: Exception) {}
        }
    }

    private fun hide() {
        if (shown) {
            try { wm.removeView(root) } catch (_: Exception) {}
            shown = false
        }
    }

    /** Distance from the screen bottom so the pill sits just above the keyboard. */
    private fun bottomOffset(): Int {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        val r = Rect()
        val ime = windows.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        return if (ime != null) {
            ime.getBoundsInScreen(r)
            (dm.heightPixels - r.top + dp(8)).coerceAtLeast(dp(24))
        } else dp(96)
    }

    private fun setBubble(text: String, autoClearMs: Long = 0) {
        main.removeCallbacks(clearBubble)
        bubble.text = text
        bubble.visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
        if (autoClearMs > 0) main.postDelayed(clearBubble, autoClearMs)
        if (shown) try { wm.updateViewLayout(root, params) } catch (_: Exception) {}
    }

    private fun applyState() {
        val listening = state == State.LISTENING
        val w = if (listening) dp(120) else dp(44)
        (wave.layoutParams as LinearLayout.LayoutParams).width = w
        wave.requestLayout()
        cancel.visibility = if (listening && latched) View.VISIBLE else View.GONE
        done.visibility = if (listening && latched) View.VISIBLE else View.GONE
        val idle = state == State.IDLE
        editChip.visibility = if (idle && hasSelection()) View.VISIBLE else View.GONE
        if (!idle) undoChip.visibility = View.GONE
        if (shown) try { wm.updateViewLayout(root, params) } catch (_: Exception) {}
    }

    // ---------- focus tracking ----------
    private fun findEditable(): AccessibilityNodeInfo? {
        return try {
            for (w in windows) {
                if (w.type != AccessibilityWindowInfo.TYPE_APPLICATION) continue
                val f = w.root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (f != null && f.isEditable) return f
            }
            rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.takeIf { it.isEditable }
        } catch (_: Exception) { null }
    }

    private fun hasSelection(): Boolean {
        val n = target ?: return false
        return n.textSelectionStart >= 0 && n.textSelectionStart != n.textSelectionEnd
    }

    private fun refresh() {
        if (state != State.IDLE) { if (shown) show(); return }
        val node = findEditable()
        if (node != null) {
            target = node
            show()
            applyState()
        } else {
            main.removeCallbacks(hideRunnable)
            main.postDelayed(hideRunnable, 400)
        }
    }

    // ---------- gestures ----------
    private fun bindTalk(v: View) {
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (state == State.LISTENING && latched) {
                        ignoreUp = true
                        finish()
                    } else if (state == State.IDLE) {
                        ignoreUp = false
                        downAt = SystemClock.uptimeMillis()
                        begin("dictate", latch = false)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!ignoreUp && state == State.LISTENING) {
                        val held = SystemClock.uptimeMillis() - downAt
                        if (held < 250 && e.action == MotionEvent.ACTION_UP) {
                            latched = true
                            applyState()
                            setBubble("Hands-free: tap ✓ when done")
                        } else finish()
                    }
                }
            }
            true
        }
    }

    // ---------- recording ----------
    private fun begin(m: String, latch: Boolean) {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            setBubble("Microphone not allowed. Opening OpenFlow…", 3000)
            startActivity(android.content.Intent(this, SettingsActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        val node = findEditable() ?: target
        if (node == null) { setBubble("Tap a text field first", 2000); return }
        target = node
        if (node.isPassword) { setBubble("OpenFlow doesn't type into password fields", 3000); return }
        pkg = node.packageName?.toString() ?: ""
        mode = m
        pendingSelection = if (m == "edit") selectedText(node) else ""
        state = State.LISTENING
        latched = latch
        wave.clear()
        applyState()
        setBubble(if (latch) "Listening… tap ✓ when done" else "Listening…")
        dictation.start()
    }

    private fun finish() {
        if (state != State.LISTENING) return
        state = State.WORKING
        latched = false
        wave.clear()
        applyState()
        setBubble("Working…")
        dictation.stop()
    }

    private fun abort() {
        if (::dictation.isInitialized) dictation.cancel()
        state = State.IDLE
        latched = false
        if (::wave.isInitialized) { wave.clear(); applyState() }
    }

    private fun idle(message: String = "", clearMs: Long = 0) {
        state = State.IDLE
        latched = false
        applyState()
        setBubble(message, clearMs)
    }

    // ---------- Dictation.Callback ----------
    override fun onLevel(level: Float) { wave.push(level) }

    override fun onPartial(text: String) { if (state == State.LISTENING) setBubble(text.takeLast(120)) }

    override fun onError(message: String) { idle(message, 3500) }

    override fun onFinal(text: String) {
        if (text.isBlank()) { idle("Didn't catch that", 2000); return }
        if (mode == "edit") handleEdit(text) else handleDictation(text)
    }

    // ---------- text handling ----------
    private fun fieldText(n: AccessibilityNodeInfo): String =
        if (n.isShowingHintText) "" else n.text?.toString() ?: ""

    private fun selection(n: AccessibilityNodeInfo): Pair<Int, Int> {
        val len = fieldText(n).length
        var s = n.textSelectionStart
        var e = n.textSelectionEnd
        if (s < 0 || e < 0) { s = len; e = len }
        if (s > e) { val t = s; s = e; e = t }
        return s.coerceIn(0, len) to e.coerceIn(0, len)
    }

    private fun selectedText(n: AccessibilityNodeInfo): String {
        val (s, e) = selection(n)
        return fieldText(n).substring(s, e)
    }

    private fun handleDictation(text: String) {
        val node = findEditable() ?: target
        if (node == null) { idle("No text field to type into", 3000); return }
        val (s, _) = selection(node)
        val before = fieldText(node).substring(0, s).takeLast(300)
        val style = prefs.appStyles()[pkg] ?: "default"
        val dict = prefs.dictionary()
        val cleaned = Cleanup.clean(text, dict, prefs.snippets(), style, prefs.spoken)
        val protectedWords = dict.values.filter { it.isNotBlank() }
            .map { it.trim().split(Regex("\\s+")).first() }.toSet()

        if (prefs.polish && prefs.apiKey.isNotBlank()) {
            setBubble("Polishing…")
            val ctx = if (prefs.sendContext) before else null
            Thread {
                val polished = Llm.polish(cleaned, style, ctx, prefs) ?: cleaned
                main.post { insert(ContextFit.fit(polished, before, protectedWords)) }
            }.start()
        } else {
            insert(ContextFit.fit(cleaned, before, protectedWords))
        }
    }

    private fun handleEdit(instruction: String) {
        if (prefs.apiKey.isBlank()) { idle("Add your Anthropic API key in the OpenFlow app first", 4000); return }
        setBubble("Editing…")
        val selection = pendingSelection
        Thread {
            val result = Llm.edit(selection, instruction, prefs)
            main.post {
                if (result == null) idle("AI request failed", 3000)
                else {
                    if (!replaceSelection(result)) idle("Couldn't write to the field", 3000) else idle()
                }
            }
        }.start()
    }

    private fun replaceSelection(text: String): Boolean {
        val node = findEditable() ?: target ?: return false
        val cur = fieldText(node)
        val (s, e) = selection(node)
        val out = cur.substring(0, s) + text + cur.substring(e)
        val ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, out)
        })
        if (ok) node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, s + text.length)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, s + text.length)
        })
        return ok
    }

    private fun insert(text: String) {
        val node = findEditable() ?: target
        val start = node?.let { selection(it).first } ?: -1
        if (!replaceSelection(text)) { idle("Couldn't write to the field", 3000); return }
        lastStart = start
        lastInserted = text
        idle()
        undoChip.visibility = View.VISIBLE
        main.removeCallbacks(hideUndo)
        main.postDelayed(hideUndo, 8000)
        if (shown) try { wm.updateViewLayout(root, params) } catch (_: Exception) {}
    }

    /** Removes the last dictation if it is still exactly where we put it. */
    private fun undo() {
        val node = findEditable() ?: target
        val s = lastInserted
        if (node == null || s == null || lastStart < 0) return
        val cur = fieldText(node)
        if (cur.startsWith(s, lastStart)) {
            val out = cur.removeRange(lastStart, lastStart + s.length)
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, out)
            })
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, lastStart)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, lastStart)
            })
            setBubble("Removed last dictation", 1500)
        }
        lastInserted = null
        undoChip.visibility = View.GONE
    }
}
