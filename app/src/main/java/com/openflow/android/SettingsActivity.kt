package com.openflow.android

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private lateinit var muteSounds: Switch
    private lateinit var updateRow: Row
    private lateinit var apiKey: EditText
    private lateinit var model: EditText
    private lateinit var dictionary: EditText
    private lateinit var snippets: EditText
    private lateinit var appStyles: EditText
    private lateinit var tryField: EditText
    private lateinit var useLocal: Switch
    private lateinit var onlineSpeech: Switch
    private lateinit var smallRow: Row
    private lateinit var baseRow: Row
    private lateinit var downloadRow: Row
    private val ui = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() { refreshSpeech(); ui.postDelayed(this, 500) }
    }

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
        ui.post(tick)
    }

    override fun onPause() {
        ui.removeCallbacks(tick)
        super.onPause()
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

    /** A settings row plus handles to its text, so progress and status can change live. */
    private class Row(val view: LinearLayout, val title: TextView, val subtitle: TextView, val trailing: TextView?)

    private fun buildRow(title: String, subtitle: String?, trailing: TextView?, onClick: (() -> Unit)? = null): Row {
        val titleView = text(title, 17f)
        val subView = text(subtitle ?: "", 13f, cSub).apply { visibility = if (subtitle == null) View.GONE else View.VISIBLE }
        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(titleView)
            addView(subView)
        }
        val view = LinearLayout(this).apply {
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
        return Row(view, titleView, subView, trailing)
    }

    private fun row(title: String, subtitle: String?, trailing: View?, onClick: (() -> Unit)? = null): LinearLayout =
        buildRow(title, subtitle, trailing as? TextView, onClick).view.also { v ->
            if (trailing != null && trailing !is TextView) {
                v.addView(trailing, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(12) })
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
        divider(behaviour)
        val (muteRow, muteSw) = switchRow("Silence microphone beeps", "Mutes the phone's start and stop beeps while you dictate, then restores your volume.", prefs.muteSounds)
        muteSounds = muteSw
        muteSounds.setOnCheckedChangeListener { _, on -> prefs.muteSounds = on }
        behaviour.addView(muteRow)
        divider(behaviour)
        behaviour.addView(row("Reset icon position", "Drag the icon anywhere. This puts it back by the keyboard.", null) {
            prefs.resetOverlayPos()
            Toast.makeText(this, "Icon position reset", Toast.LENGTH_SHORT).show()
        })
        col.addView(behaviour)

        col.addView(section("Speech recognition"))
        val speech = card()
        val (localRow, localSw) = switchRow("On-device Whisper (English)", "Most accurate. Private and free. Works offline.", prefs.useLocal)
        useLocal = localSw
        useLocal.setOnCheckedChangeListener { _, on -> prefs.useLocal = on; refreshSpeech() }
        speech.addView(localRow)
        divider(speech)
        smallRow = buildRow(Models.SMALL.title, "${Models.SMALL.blurb} · ${Models.SMALL.totalMb} MB", text("", 17f, accent, bold = true)) {
            prefs.localModel = Models.SMALL.id; refreshSpeech()
        }
        speech.addView(smallRow.view)
        divider(speech)
        baseRow = buildRow(Models.BASE.title, "${Models.BASE.blurb} · ${Models.BASE.totalMb} MB", text("", 17f, accent, bold = true)) {
            prefs.localModel = Models.BASE.id; refreshSpeech()
        }
        speech.addView(baseRow.view)
        divider(speech)
        downloadRow = buildRow("", "", null) { onDownloadTapped() }
        speech.addView(downloadRow.view)
        divider(speech)
        val (onlineRow, onlineSw) = switchRow("Use Google online recognition", "Only when Whisper isn't downloaded. Sends audio to Google.", prefs.onlineSpeech)
        onlineSpeech = onlineSw
        onlineSpeech.setOnCheckedChangeListener { _, on -> prefs.onlineSpeech = on }
        speech.addView(onlineRow)
        col.addView(speech)
        col.addView(footnote("Download over Wi-Fi: the model is one file set, downloaded once. After that, dictation needs no internet."))

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

        col.addView(section("About"))
        val about = card()
        val version = installedVersion()
        updateRow = buildRow("Check for updates", "Version $version", null) { checkForUpdates(version) }
        about.addView(updateRow.view)
        divider(about)
        about.addView(row("Source code and releases", "github.com/${Updates.REPO}", null) { openUrl(Updates.RELEASES_PAGE) })
        col.addView(about)

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

    private fun onDownloadTapped() {
        val spec = Models.byId(prefs.localModel)
        when {
            ModelDownloader.running && ModelDownloader.activeId == spec.id -> ModelDownloader.cancel()
            Models.isReady(this, spec) -> AlertDialog.Builder(this)
                .setTitle("Delete ${spec.title}?")
                .setMessage("This frees ${spec.totalMb} MB. You can download it again later.")
                .setPositiveButton("Delete") { _, _ -> Models.delete(this, spec); refreshSpeech() }
                .setNegativeButton("Keep", null)
                .show()
            else -> {
                ModelDownloader.start(this, spec)
                Toast.makeText(this, "Downloading ${spec.title}", Toast.LENGTH_SHORT).show()
            }
        }
        refreshSpeech()
    }

    private fun refreshSpeech() {
        if (!::downloadRow.isInitialized) return
        val spec = Models.byId(prefs.localModel)
        smallRow.trailing?.text = if (spec.id == Models.SMALL.id) "✓" else ""
        baseRow.trailing?.text = if (spec.id == Models.BASE.id) "✓" else ""
        val ready = Models.isReady(this, spec)
        val busy = ModelDownloader.running && ModelDownloader.activeId == spec.id
        when {
            busy -> {
                val done = ModelDownloader.doneBytes
                val total = ModelDownloader.totalBytes.coerceAtLeast(1)
                downloadRow.title.text = "Downloading ${spec.title}…  ${(done * 100 / total).toInt()}%"
                downloadRow.subtitle.text = "${done / 1_000_000} of ${total / 1_000_000} MB. Tap to cancel."
                downloadRow.subtitle.setTextColor(cSub)
            }
            ready -> {
                downloadRow.title.text = "${spec.title} is ready ✓"
                downloadRow.subtitle.text = if (prefs.useLocal) "Dictation uses it now, offline. Tap to delete." else "Turn on On-device Whisper above to use it. Tap to delete."
                downloadRow.subtitle.setTextColor(cSub)
            }
            else -> {
                downloadRow.title.text = "Download ${spec.title}"
                val err = ModelDownloader.error
                downloadRow.subtitle.text = err ?: "${spec.totalMb} MB, one time. Use Wi-Fi."
                downloadRow.subtitle.setTextColor(if (err != null) attention else cSub)
            }
        }
    }

    private fun installedVersion(): String =
        try { packageManager.getPackageInfo(packageName, 0).versionName ?: "?" } catch (_: Exception) { "?" }

    private fun openUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {
            Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkForUpdates(current: String) {
        updateRow.subtitle.text = "Checking GitHub…"
        updateRow.subtitle.setTextColor(cSub)
        Thread {
            val result = Updates.check(current)
            runOnUiThread {
                when (result) {
                    is Updates.Result.UpToDate -> {
                        updateRow.subtitle.text = "Version $current · you're up to date ✓"
                        updateRow.subtitle.setTextColor(good)
                    }
                    is Updates.Result.Failed -> {
                        updateRow.subtitle.text = "Couldn't check: ${result.reason}"
                        updateRow.subtitle.setTextColor(attention)
                    }
                    is Updates.Result.Newer -> {
                        updateRow.subtitle.text = "Version ${result.info.tag.removePrefix("v")} is available"
                        updateRow.subtitle.setTextColor(accent)
                        showUpdateDialog(current, result.info)
                    }
                }
            }
        }.start()
    }

    private fun showUpdateDialog(current: String, info: Updates.Info) {
        val notes = info.notes.trim().take(600).ifEmpty { "No release notes." }
        AlertDialog.Builder(this)
            .setTitle("Update available: ${info.tag.removePrefix("v")}")
            .setMessage(
                "You have $current.\n\n$notes\n\n" +
                    "Tap Download, then open the file to install. If Android says \"App not installed\", uninstall " +
                    "OpenFlow first (your settings and the Whisper model are removed), then install the new file."
            )
            .setPositiveButton("Download") { _, _ -> openUrl(info.apkUrl ?: info.pageUrl) }
            .setNegativeButton("Later", null)
            .show()
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
