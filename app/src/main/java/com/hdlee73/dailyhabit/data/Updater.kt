package com.hdlee73.dailyhabit.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.hdlee73.dailyhabit.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
    val pageUrl: String,
)

/** GitHub 릴리스에서 내 판본(천주교인용/일반용)의 최신 APK를 찾아 내려받고 설치한다 */
object Updater {
    const val REPO = "hdlee73/DAilyhabit"
    const val RELEASES_PAGE = "https://github.com/$REPO/releases"

    /** 새 버전을 찾았을 때 메뉴의 NEW 표시에 쓰는 마지막 확인 결과 */
    var available by mutableStateOf<UpdateInfo?>(null)

    val hasUpdate: Boolean get() = available?.let { isNewer(it.version, BuildConfig.VERSION_NAME) } == true

    /** "1.3.0", "1.3.0-dev.5" → 숫자 부분과 개발 빌드 여부 */
    private fun parse(v: String): Pair<List<Int>, Boolean> {
        val base = v.removePrefix("v").substringBefore('-')
        val nums = base.split('.').map { it.toIntOrNull() ?: 0 }
        return nums to v.contains("-dev")
    }

    fun isNewer(latest: String, current: String): Boolean {
        val (a, _) = parse(latest)
        val (b, currentIsDev) = parse(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        // 같은 숫자면 개발 빌드보다 정식 릴리스가 새 것
        return currentIsDev
    }

    /** 내 판본의 가장 높은 정식 릴리스. 네트워크 오류는 예외로 던진다 */
    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        val conn = URL("https://api.github.com/repos/$REPO/releases?per_page=30").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "DailyHabit-Android")
            if (conn.responseCode != 200) throw java.io.IOException("서버 응답 ${conn.responseCode}")
            val arr = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
            var best: UpdateInfo? = null
            for (i in 0 until arr.length()) {
                val r = arr.getJSONObject(i)
                if (r.optBoolean("draft") || r.optBoolean("prerelease")) continue
                val tag = r.optString("tag_name")
                if (tag.endsWith("-catholic") != BuildConfig.CATHOLIC) continue
                val version = tag.removePrefix("v").removeSuffix("-catholic")
                val assets = r.optJSONArray("assets") ?: continue
                var apk: org.json.JSONObject? = null
                for (j in 0 until assets.length()) {
                    val a = assets.getJSONObject(j)
                    if (a.optString("name").endsWith(".apk")) { apk = a; break }
                }
                if (apk == null) continue
                if (best == null || isNewer(version, best.version)) {
                    best = UpdateInfo(
                        version = version,
                        notes = r.optString("body").trim(),
                        apkUrl = apk.getString("browser_download_url"),
                        apkSize = apk.optLong("size"),
                        pageUrl = r.optString("html_url", RELEASES_PAGE),
                    )
                }
            }
            best
        } finally {
            conn.disconnect()
        }
    }

    /** 앱을 열 때마다 확인한다. 설치할 때까지 시작할 때마다 새 버전이 보이게 하기 위해 간격을 두지 않는다 */
    suspend fun autoCheck(context: Context) {
        val settings = AppSettings(context)
        if (!settings.autoUpdateCheck) return
        runCatching { fetchLatest() }.onSuccess {
            settings.lastUpdateCheck = System.currentTimeMillis()
            available = it
        }
    }

    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val out = File(dir, "DailyHabit-${info.version}.apk")
        val conn = URL(info.apkUrl).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("User-Agent", "DailyHabit-Android")
            if (conn.responseCode != 200) throw java.io.IOException("다운로드 실패 (${conn.responseCode})")
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: info.apkSize
            conn.inputStream.use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        done += n
                        if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            onProgress(1f)
            out
        } finally {
            conn.disconnect()
        }
    }

    /** 설치 화면을 연다. '출처를 알 수 없는 앱' 허용이 필요하면 설정 화면을 열고 false를 돌려준다 */
    fun install(context: Context, apk: File): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }
}
