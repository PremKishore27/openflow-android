package com.openflow.android

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * OpenFlow as ONE floating icon, in the style of Wispr Flow.
 *
 * The icon is the OpenFlow logo itself. Tap it: it starts listening and grows into a small bar  [ ✕ | waveform | ✓ ].
 * Tap ✓ and your words are typed into the focused text field, then the pill shrinks back to the icon.
 * Everything lives in a single small window above the keyboard.
 */
class OpenFlowService : AccessibilityService(), Dictation.Callback {
    private enum class State { IDLE, LISTENING, WORKING }

    private lateinit var prefs: Prefs
    private lateinit var dictation: Dictation
    private lateinit var wm: WindowManager
    private val main = Handler(Looper.getMainLooper())

    private lateinit var root: LinearLayout
    private lateinit var bubble: TextView
    private lateinit var cancel: TextView
    private lateinit var done: TextView
    private lateinit var center: FrameLayout
    private lateinit var logo: LogoView
    private lateinit var pill: LinearLayout
    private lateinit var wave: WaveformView
    private lateinit var params: WindowManager.LayoutParams
    private var shown = false

    private var state = State.IDLE
    private var mode = "dictate"
    private var pendingSelection = ""
    private var target: AccessibilityNodeInfo? = null
    private var pkg = ""
    private var undoStart = -1
    private var undoText: String? = null

    private val hideRunnable = Runnable { if (state == State.IDLE) hide() }
    private val refreshRunnable = Runnable { refresh() }
    private val clearBubble = Runnable { setBubble("") }
    private val expireUndo = Runnable { undoText = null; setBubble("") }

    private val ICON = 48

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

    override fun onUnbind(intent: Intent?): Boolean {
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

    // ---------- overlay UI (one window, one icon) ----------
    private fun capsule(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), 0x33FFFFFF)
    }

    private fun chip(label: String, bg: Int, onTap: () -> Unit): TextView = TextView(this).apply {
        text = label
        textSize = 17f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(bg) }
        layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        visibility = View.GONE
        setOnClickListener { onTap() }
    }

    private fun buildOverlay() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        bubble = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            maxLines = 3
            maxWidth = dp(260)
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = capsule(0xF21C1C1E.toInt(), 14)
            visibility = View.GONE
            setOnClickListener { if (undoText != null) undo() }
        }
        root.addView(bubble, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(8) })

        cancel = chip("✕", 0xFF3A3A3C.toInt()) { abort() }
        done = chip("✓", 0xFF30D158.toInt()) { finish() }
        logo = LogoView(this)
        wave = WaveformView(this).apply { visibility = View.GONE }
        center = FrameLayout(this).apply {
            contentDescription = "OpenFlow: tap to dictate"
            addView(logo, FrameLayout.LayoutParams(-1, -1))
            addView(wave, FrameLayout.LayoutParams(-1, dp(26), Gravity.CENTER))
            layoutParams = LinearLayout.LayoutParams(dp(ICON), dp(ICON))
            setOnClickListener { onTapIcon() }
        }

        pill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        pill.addView(cancel)
        pill.addView(center)
        pill.addView(done)
        root.addView(pill, LinearLayout.LayoutParams(-2, -2))

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = dp(12)
            y = dp(96)
        }
        applyState()
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

    private fun relayout() {
        if (shown) try { wm.updateViewLayout(root, params) } catch (_: Exception) {}
    }

    /** Distance from the screen bottom so the icon sits just above the keyboard. */
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
        relayout()
    }

    /** Idle: just the OpenFlow logo. Listening: it grows into  [ ✕ | waveform | ✓ ]. */
    private fun applyState() {
        val listening = state == State.LISTENING
        cancel.visibility = if (listening) View.VISIBLE else View.GONE
        done.visibility = if (listening) View.VISIBLE else View.GONE
        logo.visibility = if (listening) View.GONE else View.VISIBLE
        logo.alpha = if (state == State.WORKING) 0.55f else 1f
        wave.visibility = if (listening) View.VISIBLE else View.GONE
        pill.background = if (listening) capsule(0xF2151517.toInt(), 28) else null
        val pad = if (listening) dp(4) else 0
        pill.setPadding(pad, pad, pad, pad)
        (center.layoutParams as LinearLayout.LayoutParams).width = if (listening) dp(112) else dp(ICON)
        center.requestLayout()
        relayout()
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

    private fun hasSelection(n: AccessibilityNodeInfo) =
        n.textSelectionStart >= 0 && n.textSelectionStart != n.textSelectionEnd

    private fun refresh() {
        if (state != State.IDLE) { if (shown) show(); return }
        val node = findEditable()
        if (node != null) {
            target = node
            show()
        } else {
            main.removeCallbacks(hideRunnable)
            main.postDelayed(hideRunnable, 400)
        }
    }

    // ---------- tap flow ----------
    /** Tap the icon to start; tap ✓ (or the icon again) to finish. */
    private fun onTapIcon() {
        when (state) {
            State.IDLE -> begin()
            State.LISTENING -> finish()
            State.WORKING -> {}
        }
    }

    private fun begin() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            setBubble("Microphone not allowed. Opening OpenFlow…", 3000)
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        val node = findEditable() ?: target
        if (node == null) { setBubble("Tap a text field first", 2000); return }
        target = node
        if (node.isPassword) { setBubble("OpenFlow doesn't type into password fields", 3000); return }
        pkg = node.packageName?.toString() ?: ""
        // With text selected and an API key set, speaking is an instruction ("make this more formal").
        val editing = hasSelection(node) && prefs.apiKey.isNotBlank()
        mode = if (editing) "edit" else "dictate"
        pendingSelection = if (editing) selectedText(node) else ""
        undoText = null
        main.removeCallbacks(expireUndo)
        state = State.LISTENING
        wave.clear()
        setBubble(if (editing) "Say how to change the selected text" else "Listening…")
        applyState()
        dictation.start()
    }

    private fun finish() {
        if (state != State.LISTENING) return
        state = State.WORKING
        wave.clear()
        setBubble("Working…")
        applyState()
        dictation.stop()
    }

    private fun abort() {
        if (::dictation.isInitialized) dictation.cancel()
        if (::wave.isInitialized) idle()
    }

    private fun idle(message: String = "", clearMs: Long = 0) {
        state = State.IDLE
        wave.clear()
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
                when {
                    result == null -> idle("AI request failed", 3000)
                    !replaceSelection(result) -> idle("Couldn't write to the field", 3000)
                    else -> idle()
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
        if (node == null) { idle("Couldn't write to the field", 3000); return }
        val start = selection(node).first
        if (!replaceSelection(text)) { idle("Couldn't write to the field", 3000); return }
        undoStart = start
        undoText = text
        main.removeCallbacks(expireUndo)
        main.postDelayed(expireUndo, 6000)
        idle("↶ Tap to undo")
    }

    /** Removes the last dictation if it is still exactly where we put it. */
    private fun undo() {
        val s = undoText ?: return
        val node = findEditable() ?: target ?: return
        val cur = fieldText(node)
        if (undoStart >= 0 && cur.startsWith(s, undoStart)) {
            val out = cur.removeRange(undoStart, undoStart + s.length)
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, out)
            })
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, undoStart)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, undoStart)
            })
            setBubble("Removed last dictation", 1500)
        } else {
            setBubble("Can't undo: you've typed since", 1500)
        }
        undoText = null
        main.removeCallbacks(expireUndo)
    }
}
