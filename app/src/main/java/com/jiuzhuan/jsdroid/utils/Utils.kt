package com.jiuzhuan.jsdroid.utils

import android.graphics.Bitmap
import android.media.Image
import android.util.Log
import androidx.core.graphics.createBitmap
import com.jiuzhuan.jsdroid.BuildConfig
import com.jiuzhuan.jsdroid.events.MessageEvent
import com.jiuzhuan.jsdroid.room.AppDatabase
import com.jiuzhuan.jsdroid.room.LogEntity
import com.jiuzhuan.jsdroid.room.level_nomal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.greenrobot.eventbus.EventBus
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

fun addLogs(message: String, showFloatToast: Boolean = false, level: String = level_nomal, log: Boolean = true) {
    if (log) {
        CoroutineScope(Dispatchers.IO).launch {
            val log = LogEntity(
                timestamp = System.currentTimeMillis(), level = level, message = message
            )
            AppDatabase.getInstance().logDao().insert(log)
        }
    }
    if (showFloatToast) {
        EventBus.getDefault().post(MessageEvent(message))
    }
}


val LINE_SEP: String = System.getProperty("line.separator")
val okHttpClient =
    OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS).build()

fun fetchJsContent(url: String): String {
    try {
        val request: Request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        val body = response.body
        return body?.string() ?: ""
    } catch (e: IOException) {
        showException(e)
        return ""
    }
}

fun logd(vararg msg: String) {
    if (BuildConfig.DEBUG) {
        Log.e("Utils", msg.toString())
    }
}

fun showException(e: Exception) {
    if (BuildConfig.DEBUG) {
        e.printStackTrace()
    }
}

fun execCmd(
    vararg commands: String
): String {
    var result = -1
    if (commands.isEmpty()) {
        return ""
    }
    var process: Process? = null
    var successResult: BufferedReader? = null
    var successMsg: StringBuilder? = null

    var os: DataOutputStream? = null
    try {
        val isRooted = false
        process = Runtime.getRuntime().exec(if (isRooted) "su" else "sh", null, null)
        os = DataOutputStream(process.outputStream)
        for (command in commands) {
            os.write(command.toByteArray())
            os.writeBytes(LINE_SEP)
            os.flush()
        }
        os.writeBytes("exit$LINE_SEP")
        os.flush()
        result = process.waitFor()
        if (result != 0) {
            return ""
        }
        successMsg = StringBuilder()
        successResult = BufferedReader(
            InputStreamReader(process.inputStream, "UTF-8")
        )

        var line: String?
        if ((successResult.readLine().also { line = it }) != null) {
            successMsg.append(line)
            while ((successResult.readLine().also { line = it }) != null) {
                successMsg.append("@@").append(line)
            }
        }
    } catch (e: Exception) {
        showException(e)
        return ""
    } finally {
        try {
            os?.close()
            successResult?.close()
            process?.destroy()
        } catch (e: IOException) {
            showException(e)
        }
    }
//    logd(*commands, successMsg.toString())
    return successMsg.toString()
}

fun handlePackage(): String {
    return execCmd("dumpsys window | grep -E 'mFocusedApp='")
}

internal data class PackageClassCandidate(
    val packageName: String,
    val className: String,
    val rawLine: String,
    val sourceHint: String = ""
)

private val packageClassPattern = """([A-Za-z0-9_.$]+)/([A-Za-z0-9_.$]+)""".toRegex()
private val likelyForegroundNoisePackages = setOf(
    "android",
    "com.android.systemui",
    "com.android.launcher3",
    "com.google.android.permissioncontroller",
    "com.android.permissioncontroller",
    "com.miui.home",
    "com.miui.securitycenter",
    "com.miui.systemui"
)

internal fun extractPackageAndClassCandidates(
    input: String,
    sourceHint: String = ""
): List<PackageClassCandidate> {
    if (input.isBlank()) {
        return emptyList()
    }
    return input.replace("@@", "\n")
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { line ->
            packageClassPattern.find(line)?.let { matchResult ->
                PackageClassCandidate(
                    packageName = matchResult.groupValues[1],
                    className = matchResult.groupValues[2],
                    rawLine = line,
                    sourceHint = sourceHint
                )
            }
        }
        .toList()
}

private fun scorePackageClassCandidate(candidate: PackageClassCandidate): Int {
    val raw = "${candidate.sourceHint} ${candidate.rawLine}".lowercase()
    var score = 0
    when {
        raw.contains("topresumedactivity") -> score += 120
        raw.contains("mresumedactivity") -> score += 110
        raw.contains("top-activity") -> score += 100
        raw.contains("mcurrentfocus=window") -> score += 90
        raw.contains("mfocusedwindow=window") -> score += 80
        raw.contains("mfocusedapp=activityrecord") -> score += 70
    }
    if (candidate.packageName in likelyForegroundNoisePackages) {
        score -= 40
    }
    if (candidate.rawLine.contains("Splash Screen", ignoreCase = true)) {
        score -= 5
    }
    if (candidate.className.startsWith(".")) {
        score += 10
    }
    return score
}

internal fun selectBestPackageAndClassCandidate(
    candidates: Collection<PackageClassCandidate>
): PackageClassCandidate? {
    return candidates.maxByOrNull(::scorePackageClassCandidate)
}

fun extractPackageAndClass(input: String): Pair<String, String>? {
    val bestCandidate = selectBestPackageAndClassCandidate(
        extractPackageAndClassCandidates(input)
    ) ?: return null
    return bestCandidate.packageName to bestCandidate.className
}

fun getFocusedAppPackageAndClass(): Pair<String, String>? {
    return extractPackageAndClass(handlePackage())
}

fun Image.toBitmap(): Bitmap {
    val planes = this.planes
    val buffer = planes[0].buffer
    val pixelStride = planes[0].pixelStride
    val rowStride = planes[0].rowStride
    val rowPadding = rowStride - pixelStride * width

    val bitmap = createBitmap(width + rowPadding / pixelStride, height)
    bitmap.copyPixelsFromBuffer(buffer)
    return bitmap
}
