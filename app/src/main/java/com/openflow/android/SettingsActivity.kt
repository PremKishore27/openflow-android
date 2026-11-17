package com.openflow.android

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Setup + settings screen (the launcher icon). Built in code so the project needs no extra libraries. */
class SettingsActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var statusView: TextView
    private lateinit var polish: CheckBox
    private lateinit var sendContext: CheckBox
    private lateinit var spoken: CheckBox
    private lateinit var apiKey: EditText
    private lateinit var model: EditText
    private lateinit var dictionary: EditText
    private lateinit var snippets: EditText
    private lateinit var appStyles: EditText

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        title = "OpenFlow"
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

    private fun heading(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setPadding(0, dp(20), 0, dp(6))
        setTextColor(0xFF6E6E73.toInt())
    }

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    private fun field(hint: String, value: String, lines: Int = 1, secret: Boolean = false) = EditText(this).apply {
        this.hint = hint
        setText(value)
        if (secret) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        else if (lines > 1) { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; minLines = lines; gravity = android.view.Gravity.TOP }
        textSize = 14f
    }

    private fun build(): ScrollView {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(40))
        }
        col.addView(TextView(this).apply { text = "OpenFlow"; textSize = 26f })
        col.addView(TextView(this).apply { text = "A floating voice pill over your normal keyboard: hold it, talk, get clean text in any app." })

        col.addView(heading("SET UP"))
        statusView = TextView(this).apply { textSize = 14f }
        col.addView(statusView)
        col.addView(button("1. Allow the microphone") {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        })
        col.addView(button("2. Turn on the OpenFlow overlay") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        col.addView(TextView(this).apply {
            text = "In Accessibility, open Installed apps → OpenFlow and switch it on. If Android says " +
                "\"restricted setting\", go to Settings → Apps → OpenFlow → ⋮ → Allow restricted settings, then retry."
            textSize = 12f
        })
        col.addView(field("Try it here: tap this box, the pill appears above your keyboard", "", 2))

        col.addView(heading("BEHAVIOUR"))
        spoken = CheckBox(this).apply { text = "Spoken punctuation (\"comma\", \"new line\")"; isChecked = prefs.spoken }
        col.addView(spoken)

        col.addView(heading("AI (OPTIONAL)"))
        polish = CheckBox(this).apply { text = "Polish every dictation with Claude"; isChecked = prefs.polish }
        sendContext = CheckBox(this).apply { text = "Also send the text before the cursor to Claude"; isChecked = prefs.sendContext }
        col.addView(polish)
        col.addView(sendContext)
        apiKey = field("Anthropic API key (sk-ant-...)", prefs.apiKey, secret = true)
        model = field("Model", prefs.model)
        col.addView(apiKey)
        col.addView(model)
        col.addView(TextView(this).apply {
            text = "The key is stored only in this app's private storage. The ✎ edit-selection chip needs it too."
            textSize = 12f
        })

        col.addView(heading("DICTIONARY (heard = written, one per line)"))
        dictionary = field("open flow = OpenFlow", prefs.dictionaryText, 4)
        col.addView(dictionary)
        col.addView(heading("SNIPPETS (say trigger = inserted text)"))
        snippets = field("my email = you@example.com", prefs.snippetsText, 3)
        col.addView(snippets)
        col.addView(heading("TONE PER APP (package name = casual | formal | default)"))
        appStyles = field("com.whatsapp = casual", prefs.appStylesText, 4)
        col.addView(appStyles)

        col.addView(button("Save") { save() }.apply { setPadding(0, dp(12), 0, dp(12)) })
        return ScrollView(this).apply { addView(col) }
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
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val overlay = enabledServices.contains("$packageName/")
        val mic = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        fun mark(ok: Boolean) = if (ok) "✅" else "⬜"
        statusView.text = "${mark(mic)} Microphone allowed\n${mark(overlay)} Overlay turned on"
    }
}
