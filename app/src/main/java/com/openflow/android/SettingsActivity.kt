package com.openflow.android

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/**
 * Setup and settings. Styled after Apple's grouped-list look: light and dark, rounded cards,
 * hairline separators, system colours. Built in code so the project needs no extra libraries.
 */
class SettingsActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var micStatus: TextView
    private lateinit var overlayStatus: TextView
    private lateinit var polish: Switch
    private lateinit var sendContext: Switch
    private lateinit var spoken: Switch
    private lateinit var apiKey: EditText
    private lateinit var model: EditText
    private lateinit var dictionary: EditText
    private lateinit var snippets: EditText
    private lateinit var appStyles: EditText
    private lateinit var tryField: EditText

    private val night by lazy {
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
    private val cBg by lazy { if (night) 0xFF000000.toInt() else 0xFFF2F2F7.toInt() }
    private val cCard by lazy { if (night) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt() }
    private val cText by lazy { if (night) 0xFFFFFFFF.toInt() else 0xFF000000.toInt() }
    private val cSub by lazy { if (night) 0xFF8E8E93.toInt() else 0xFF6C6C70.toInt() }
    private val cLine by lazy { if (night) 0xFF38383A.toInt() else 0xFFD1D1D6.toInt() }
    private val accent = 0xFF0A84FF.toInt()
    private val good = 0xFF30D158.toInt()
    private val attention = 0xFFFF9F0A.toInt()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        setContentView(build())
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refreshStatus()
    }

    // ---------- building blocks ----------
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun text(s: String, size: Float, color: Int? = null, bold: Boolean = false) = TextView(this).apply {
        text = s
        textSize = size
        setTextColor(color ?: cText)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun section(title: String) = text(title.uppercase(), 13f, cSub).apply {
        setPadding(dp(32), dp(24), dp(20), dp(8))
    }

    private fun footnote(s: String) = text(s, 13f, cSub).apply { setPadding(dp(32), dp(8), dp(20), 0) }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(cCard, 12)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(16), 0, dp(16), 0) }
    }

    private fun divider(into: LinearLayout) {
        into.addView(
            View(this).apply { setBackgroundColor(cLine) },
            LinearLayout.LayoutParams(-1, 1).apply { setMargins(dp(16), 0, 0, 0) }
        )
    }

    private fun row(title: String, subtitle: String?, trailing: View?, onClick: (() -> Unit)? = null): LinearLayout {
        val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        left.addView(text(title, 17f))
        if (subtitle != null) left.addView(text(subtitle, 13f, cSub))
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            addView(left, LinearLayout.LayoutParams(0, -2, 1f))
            if (trailing != null) addView(trailing, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(12) })
            if (onClick != null) {
                val ta = obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
                background = ta.getDrawable(0)
                ta.recycle()
                isClickable = true
                setOnClickListener { onClick() }
            }
        }
    }

    private fun switchRow(title: String, subtitle: String?, checked: Boolean): Pair<LinearLayout, Switch> {
        val sw = Switch(this).apply { isChecked = checked }
        return row(title, subtitle, sw) to sw
    }

    private fun field(hint: String, value: String, lines: Int = 1, secret: Boolean = false) = EditText(this).apply {
        this.hint = hint
        setText(value)
        textSize = 16f
        setTextColor(cText)
        setHintTextColor(cSub)
        background = null
        setPadding(dp(16), dp(12), dp(16), dp(12))
        when {
            secret -> inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            lines > 1 -> {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines = lines
                gravity = Gravity.TOP
            }
        }
    }

    private fun primaryButton(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 17f
        setTextColor(0xFFFFFFFF.toInt())
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        background = rounded(accent, 12)
        layoutParams = LinearLayout.LayoutParams(-1, dp(52)).apply { setMargins(dp(16), dp(24), dp(16), 0) }
        setOnClickListener { onClick() }
    }

    // ---------- screen ----------
    private fun build(): ScrollView {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(56), 0, dp(40))
        }
        col.addView(text("OpenFlow", 34f, bold = true).apply { setPadding(dp(20), 0, dp(20), 0) })
        col.addView(text("Tap the icon, speak, tap ✓, and your words appear in any app.", 15f, cSub).apply {
            setPadding(dp(20), dp(4), dp(20), 0)
        })

        col.addView(section("Set up: 4 steps, in this order"))
        val setup = card()
        micStatus = text("", 15f, bold = true)
        setup.addView(row("1. Allow the microphone", "So OpenFlow can hear you", micStatus) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        })
        divider(setup)
        setup.addView(row("2. Open Accessibility", "Find OpenFlow. Android will say it's a restricted setting. That's expected. Come back for step 3.", null) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        divider(setup)
        setup.addView(row("3. Allow restricted settings", "Tap ⋮ in the top corner, then Allow restricted settings", null) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        })
        divider(setup)
        overlayStatus = text("", 15f, bold = true)
        setup.addView(row("4. Turn on the overlay", "Back in Accessibility, open OpenFlow and switch it on", overlayStatus) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        col.addView(setup)
        col.addView(footnote(
            "Why step 3? Android blocks apps installed outside the Play Store from using accessibility until you " +
                "allow restricted settings. If step 2 lets you switch OpenFlow on straight away, skip step 3."
        ))

        col.addView(section("Try it"))
        val tryCard = card()
        tryField = field("Tap here, then tap the OpenFlow icon and speak", "", 3)
        tryCard.addView(tryField)
        col.addView(tryCard)

        col.addView(section("Behaviour"))
        val behaviour = card()
        val (spokenRow, sw) = switchRow("Spoken punctuation", "Say “comma”, “full stop” or “new line”", prefs.spoken)
        spoken = sw
        behaviour.addView(spokenRow)
        col.addView(behaviour)

        col.addView(section("AI polish (optional)"))
        val ai = card()
        val (polishRow, p) = switchRow("Polish with Claude", "Smooths grammar and flow", prefs.polish)
        polish = p
        ai.addView(polishRow)
        divider(ai)
        val (ctxRow, c) = switchRow("Send text before cursor", "Helps match names and style", prefs.sendContext)
        sendContext = c
        ai.addView(ctxRow)
        divider(ai)
        apiKey = field("Anthropic API key (sk-ant-…)", prefs.apiKey, secret = true)
        ai.addView(apiKey)
        divider(ai)
        model = field("Model", prefs.model)
        ai.addView(model)
        col.addView(ai)
        col.addView(footnote("Stored only on this phone. OpenFlow works fully without a key."))

        col.addView(section("Dictionary: heard = written"))
        val dict = card()
        dictionary = field("open flow = OpenFlow", prefs.dictionaryText, 4)
        dict.addView(dictionary)
        col.addView(dict)

        col.addView(section("Snippets: say = inserted"))
        val snip = card()
        snippets = field("my email = you@example.com", prefs.snippetsText, 3)
        snip.addView(snippets)
        col.addView(snip)

        col.addView(section("Tone per app: package = casual | formal | default"))
        val tone = card()
        appStyles = field("com.whatsapp = casual", prefs.appStylesText, 4)
        tone.addView(appStyles)
        col.addView(tone)

        col.addView(primaryButton("Save") { save() })
        return ScrollView(this).apply {
            setBackgroundColor(cBg)
            isFillViewport = true
            addView(col)
        }
    }

    private fun save() {
        prefs.spoken = spoken.isChecked
        prefs.polish = polish.isChecked
        prefs.sendContext = sendContext.isChecked
        prefs.apiKey = apiKey.text.toString()
        prefs.model = model.text.toString().ifBlank { "claude-haiku-5-5" }
        prefs.dictionaryText = dictionary.text.toString()
        prefs.snippetsText = snippets.text.toString()
        prefs.appStylesText = appStyles.text.toString()
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
    }

    private fun refreshStatus() {
        val mic = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        micStatus.text = if (mic) "✓ Allowed" else "Allow"
        micStatus.setTextColor(if (mic) good else accent)

        val service = ComponentName(this, OpenFlowService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?.contains(service) == true
        overlayStatus.text = if (enabled) "✓ On" else "Off"
        overlayStatus.setTextColor(if (enabled) good else attention)
    }
}
