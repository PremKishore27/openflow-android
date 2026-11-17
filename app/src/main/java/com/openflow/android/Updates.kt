package com.openflow.android

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Checks GitHub for a newer release. Sends nothing about you: it is one public, anonymous GET request. */
object Updates {
    const val REPO = "PremKishore27/openflow-android"
    const val RELEASES_PAGE = "https://github.com/$REPO/releases/latest"

    data class Info(val tag: String, val notes: String, val apkUrl: String?, val pageUrl: String)

    sealed class Result {
        data class Newer(val info: Info) : Result()
        object UpToDate : Result()
        data class Failed(val reason: String) : Result()
    }

    /** "v0.10.0" -> [0, 10, 0]. Anything after the digits of a part (like "-beta") is ignored. */
    fun parse(version: String): List<Int> =
        version.trim().removePrefix("v").split('.').map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

    fun isNewer(latest: String, current: String): Boolean {
        val a = parse(latest)
        val b = parse(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** Builds before 0.11 were signed with throwaway keys, so updating from them needs one uninstall first. */
    fun needsReinstall(installed: String) = isNewer("0.11.0", installed)

    /** Blocking. Run it on a background thread. */
    fun check(currentVersion: String): Result {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "OpenFlow-update-check")
            }
            if (conn.responseCode != 200) return Result.Failed("GitHub answered ${conn.responseCode}")
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val tag = json.optString("tag_name")
            if (tag.isEmpty()) return Result.Failed("No release found")
            if (!isNewer(tag, currentVersion)) return Result.UpToDate
            var apk: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name") == "OpenFlow-Android.apk") apk = a.optString("browser_download_url")
                }
            }
            Result.Newer(Info(tag, json.optString("body"), apk, json.optString("html_url", RELEASES_PAGE)))
        } catch (e: Exception) {
            Result.Failed(e.message ?: "No connection")
        } finally {
            conn?.disconnect()
        }
    }
}
