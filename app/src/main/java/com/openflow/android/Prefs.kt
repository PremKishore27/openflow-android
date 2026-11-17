package com.openflow.android

import android.content.Context
import android.content.SharedPreferences

/** Settings, stored privately in this app's own storage. */
class Prefs(ctx: Context) {
    private val sp: SharedPreferences =
        ctx.applicationContext.getSharedPreferences("openflow", Context.MODE_PRIVATE)

    var apiKey: String
        get() = sp.getString("apiKey", "") ?: ""
        set(v) { sp.edit().putString("apiKey", v.trim()).apply() }

    var model: String
        get() = sp.getString("model", "claude-haiku-5-5") ?: "claude-haiku-5-5"
        set(v) { sp.edit().putString("model", v.trim()).apply() }

    var polish: Boolean
        get() = sp.getBoolean("polish", false)
        set(v) { sp.edit().putBoolean("polish", v).apply() }

    var sendContext: Boolean
        get() = sp.getBoolean("sendContext", false)
        set(v) { sp.edit().putBoolean("sendContext", v).apply() }

    /** Use the on-device Whisper model when it has been downloaded. */
    var useLocal: Boolean
        get() = sp.getBoolean("useLocal", true)
        set(v) { sp.edit().putBoolean("useLocal", v).apply() }

    var localModel: String
        get() = sp.getString("localModel", Models.SMALL.id) ?: Models.SMALL.id
        set(v) { sp.edit().putString("localModel", v).apply() }

    /** The phone's built-in recognizer: allow Google's online engine (more accurate) instead of offline only. */
    var onlineSpeech: Boolean
        get() = sp.getBoolean("onlineSpeech", true)
        set(v) { sp.edit().putBoolean("onlineSpeech", v).apply() }

    /** Silence the beeps the phone's speech recognizer plays when listening starts and stops. */
    var muteSounds: Boolean
        get() = sp.getBoolean("muteSounds", true)
        set(v) { sp.edit().putBoolean("muteSounds", v).apply() }

    var spoken: Boolean
        get() = sp.getBoolean("spoken", true)
        set(v) { sp.edit().putBoolean("spoken", v).apply() }

    var dictionaryText: String
        get() = sp.getString("dictionary", "open flow = OpenFlow") ?: ""
        set(v) { sp.edit().putString("dictionary", v).apply() }

    var snippetsText: String
        get() = sp.getString("snippets", "my email = you@example.com") ?: ""
        set(v) { sp.edit().putString("snippets", v).apply() }

    var appStylesText: String
        get() = sp.getString(
            "appStyles",
            "com.whatsapp = casual\norg.telegram.messenger = casual\ncom.google.android.apps.messaging = casual\n" +
                "com.Slack = casual\ncom.google.android.gm = formal\ncom.microsoft.office.outlook = formal"
        ) ?: ""
        set(v) { sp.edit().putString("appStyles", v).apply() }

    /** Where you left the floating icon, as fractions of the screen (centre x, bottom y). One per keyboard state. */
    fun overlayPos(keyboardOpen: Boolean): Pair<Float, Float>? {
        val k = if (keyboardOpen) "ime" else "free"
        val x = sp.getFloat("ovx_$k", -1f)
        val y = sp.getFloat("ovy_$k", -1f)
        return if (x in 0f..1f && y in 0f..1f) x to y else null
    }

    fun saveOverlayPos(keyboardOpen: Boolean, fx: Float, fy: Float) {
        val k = if (keyboardOpen) "ime" else "free"
        sp.edit().putFloat("ovx_$k", fx.coerceIn(0f, 1f)).putFloat("ovy_$k", fy.coerceIn(0f, 1f)).apply()
    }

    fun resetOverlayPos() {
        sp.edit().remove("ovx_ime").remove("ovy_ime").remove("ovx_free").remove("ovy_free").apply()
    }

    fun dictionary() = parseMap(dictionaryText)
    fun snippets() = parseMap(snippetsText)
    fun appStyles() = parseMap(appStylesText)

    companion object {
        /** One "left = right" pair per line; blank or malformed lines are skipped. */
        fun parseMap(text: String): Map<String, String> {
            val out = LinkedHashMap<String, String>()
            for (line in text.lines()) {
                val i = line.indexOf('=')
                if (i <= 0) continue
                val k = line.substring(0, i).trim()
                val v = line.substring(i + 1).trim()
                if (k.isNotEmpty()) out[k] = v
            }
            return out
        }
    }
}
