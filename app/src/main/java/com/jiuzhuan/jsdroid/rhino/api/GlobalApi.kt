@file:JvmName("GlobalApi")

package com.jiuzhuan.jsdroid.rhino.api

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.jiuzhuan.jsdroid.BuildConfig
import com.jiuzhuan.jsdroid.api.getApp
import com.jiuzhuan.jsdroid.api.getMmkv
import com.jiuzhuan.jsdroid.base.activity
import com.jiuzhuan.jsdroid.events.ChangeFloatEvent
import com.jiuzhuan.jsdroid.events.TaskEvent
import com.jiuzhuan.jsdroid.events.TaskEvent.Companion.NEXT_TASK
import com.jiuzhuan.jsdroid.events.TaskEvent.Companion.START_TASK
import com.jiuzhuan.jsdroid.events.TaskEvent.Companion.STOP_TASK
import com.jiuzhuan.jsdroid.rhino.helper.ImageHelper
import com.jiuzhuan.jsdroid.rhino.helper.Screencap
import com.jiuzhuan.jsdroid.utils.ShellUtils
import com.jiuzhuan.jsdroid.utils.execCmd
import com.jiuzhuan.jsdroid.utils.extractPackageAndClassCandidates
import com.jiuzhuan.jsdroid.utils.handlePackage
import com.jiuzhuan.jsdroid.utils.logd
import com.jiuzhuan.jsdroid.utils.selectBestPackageAndClassCandidate
import com.ss.android.ugc.aweme.live.livehostimpl.currentActivityName
import com.ss.android.ugc.aweme.live.livehostimpl.currentPackageName
import com.ss.android.ugc.aweme.live.livehostimpl.handleClick
import com.ss.android.ugc.aweme.live.livehostimpl.handleSwipe
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object GlobalApi {

    private const val JSAPI_TRACE_TAG = "GlobalApi"
    private const val JSAPI_DETECT_PREFIX = "jsapi-detect"
    private const val SCREENSHOT_MODE_SCREEN = "screenCapture"
    private const val SCREENSHOT_MODE_PROJECTION = "mediaProjection"
    private const val SOURCE_FOCUSED_APP = "mFocusedApp=ActivityRecord"
    private const val SOURCE_CURRENT_FOCUS = "mCurrentFocus=Window"
    private const val SOURCE_FOCUSED_WINDOW = "mFocusedWindow=Window"
    private const val SOURCE_TOP_ACTIVITY = "dumpsys activity top-activity"
    private const val SOURCE_RESUMED_ACTIVITY = "dumpsys activity resumed-activity"

    private data class CurrentComponentInfo(
        val source: String,
        val packageName: String,
        val activityName: String,
        val rawLine: String = ""
    )

    private data class ForegroundSourceSnapshot(
        val source: String,
        val raw: String,
        val resolved: CurrentComponentInfo? = null,
        val score: Int? = null
    )

    private fun logJsApiTouch(message: String) {
        val logMessage = "jsapi-click $message"
        Log.e("@@@", logMessage)
        LogApi.addLog(logMessage, false)
    }

    private fun logJsApiDetect(type: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(JSAPI_TRACE_TAG, "$JSAPI_DETECT_PREFIX type=$type $message")
        }
    }

    private fun normalizeActivityName(packageName: String, activityName: String): String {
        val trimmedActivity = activityName.trim()
        if (trimmedActivity.isEmpty()) {
            return ""
        }
        return when {
            trimmedActivity.startsWith(".") -> "$packageName$trimmedActivity"
            trimmedActivity.startsWith(packageName) -> trimmedActivity
            trimmedActivity.contains(".") -> trimmedActivity
            else -> "$packageName.$trimmedActivity"
        }
    }

    private fun getScreenshotModeForCurrentComponent(): String {
        return if (runCatching { Screencap.getImageReader() }.isSuccess) {
            SCREENSHOT_MODE_PROJECTION
        } else {
            SCREENSHOT_MODE_SCREEN
        }
    }

    private fun preferredSourcesForMode(screenshotMode: String): List<String> {
        return when (screenshotMode) {
            SCREENSHOT_MODE_PROJECTION,
            SCREENSHOT_MODE_SCREEN -> listOf(
                SOURCE_CURRENT_FOCUS,
                SOURCE_FOCUSED_WINDOW,
                SOURCE_TOP_ACTIVITY,
                SOURCE_FOCUSED_APP
            )

            else -> listOf(
                SOURCE_CURRENT_FOCUS,
                SOURCE_FOCUSED_WINDOW,
                SOURCE_TOP_ACTIVITY,
                SOURCE_FOCUSED_APP
            )
        }
    }

    private fun buildCurrentComponentInfo(
        source: String,
        packageName: String,
        activityName: String,
        rawLine: String = ""
    ): CurrentComponentInfo? {
        val trimmedPackage = packageName.trim()
        val trimmedActivity = activityName.trim()
        if (trimmedPackage.isEmpty() || trimmedActivity.isEmpty()) {
            return null
        }
        return CurrentComponentInfo(source, trimmedPackage, trimmedActivity, rawLine)
    }

    private fun parseComponentFromRaw(raw: String, source: String): CurrentComponentInfo? {
        if (raw.isBlank()) {
            return null
        }
        val matchResult = selectBestPackageAndClassCandidate(
            extractPackageAndClassCandidates(raw, source)
        ) ?: return null
        return buildCurrentComponentInfo(
            source = source,
            packageName = matchResult.packageName,
            activityName = matchResult.className,
            rawLine = matchResult.rawLine
        )
    }

    private fun resolveFromFocusedApp(): CurrentComponentInfo? {
        return parseComponentFromRaw(handlePackage(), SOURCE_FOCUSED_APP)
    }

    private fun resolveFromWindow(filterStr: String): CurrentComponentInfo? {
        val raw = execCmd("dumpsys window | grep \"$filterStr\"")
        return parseComponentFromRaw(raw, filterStr)
    }

    private fun queryForegroundSourceRaw(source: String): String {
        return when (source) {
            SOURCE_FOCUSED_APP -> handlePackage()
            SOURCE_CURRENT_FOCUS,
            SOURCE_FOCUSED_WINDOW -> execCmd("dumpsys window | grep \"$source\"")
            SOURCE_TOP_ACTIVITY -> execCmd("dumpsys activity | grep 'top-activity'")
            SOURCE_RESUMED_ACTIVITY -> listOf(
                execCmd("dumpsys activity activities | grep 'topResumedActivity'"),
                execCmd("dumpsys activity activities | grep 'mResumedActivity'")
            ).filter { it.isNotBlank() }.joinToString("@@")

            else -> ""
        }
    }

    private fun resolveFromTopActivity(): CurrentComponentInfo? {
        parseComponentFromRaw(
            execCmd("dumpsys activity activities | grep 'topResumedActivity'"),
            SOURCE_RESUMED_ACTIVITY
        )?.let { return it }
        parseComponentFromRaw(
            execCmd("dumpsys activity activities | grep 'mResumedActivity'"),
            SOURCE_RESUMED_ACTIVITY
        )?.let { return it }
        return parseComponentFromRaw(
            execCmd("dumpsys activity | grep 'top-activity'"),
            SOURCE_TOP_ACTIVITY
        )
    }

    private fun shouldAggregateForegroundCandidates(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val fingerprint = Build.FINGERPRINT.lowercase()
        val isMiuiFamily = manufacturer.contains("xiaomi") ||
            brand.contains("xiaomi") ||
            brand.contains("redmi") ||
            brand.contains("poco") ||
            fingerprint.contains("miui")
        return isMiuiFamily && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    private fun scoreResolvedComponent(componentInfo: CurrentComponentInfo): Int {
        var score = when (componentInfo.source) {
            SOURCE_RESUMED_ACTIVITY -> 400
            SOURCE_TOP_ACTIVITY -> 350
            SOURCE_CURRENT_FOCUS -> 300
            SOURCE_FOCUSED_WINDOW -> 250
            SOURCE_FOCUSED_APP -> 200
            else -> 0
        }
        if (componentInfo.rawLine.contains("topResumedActivity", ignoreCase = true)) {
            score += 40
        }
        if (componentInfo.rawLine.contains("mResumedActivity", ignoreCase = true)) {
            score += 30
        }
        if (componentInfo.packageName == "com.android.systemui" ||
            componentInfo.packageName == "com.miui.systemui" ||
            componentInfo.packageName == "com.miui.home"
        ) {
            score -= 80
        }
        return score
    }

    private fun resolveComponentFromSource(source: String): CurrentComponentInfo? {
        return when (source) {
            SOURCE_FOCUSED_APP -> resolveFromFocusedApp()
            SOURCE_CURRENT_FOCUS -> resolveFromWindow(SOURCE_CURRENT_FOCUS)
            SOURCE_FOCUSED_WINDOW -> resolveFromWindow(SOURCE_FOCUSED_WINDOW)
            SOURCE_TOP_ACTIVITY -> resolveFromTopActivity()
            else -> null
        }
    }

    private fun buildForegroundSourceSnapshot(source: String): ForegroundSourceSnapshot {
        val raw = queryForegroundSourceRaw(source)
        val resolved = parseComponentFromRaw(raw, source)
        val score = resolved?.let(::scoreResolvedComponent)
        return ForegroundSourceSnapshot(
            source = source,
            raw = raw,
            resolved = resolved,
            score = score
        )
    }

    private fun buildForegroundDetectionSnapshotJson(): JSONObject {
        val screenshotMode = getScreenshotModeForCurrentComponent()
        val shouldAggregate = shouldAggregateForegroundCandidates()
        val sources = linkedSetOf<String>().apply {
            addAll(preferredSourcesForMode(screenshotMode))
            add(SOURCE_RESUMED_ACTIVITY)
            add(SOURCE_TOP_ACTIVITY)
            add(SOURCE_CURRENT_FOCUS)
            add(SOURCE_FOCUSED_WINDOW)
            add(SOURCE_FOCUSED_APP)
        }
        val snapshots = sources.map(::buildForegroundSourceSnapshot)
        val selected = if (shouldAggregate) {
            snapshots.filter { it.resolved != null }.maxByOrNull { it.score ?: Int.MIN_VALUE }
        } else {
            val preferred = preferredSourcesForMode(screenshotMode)
            preferred.firstNotNullOfOrNull { source ->
                snapshots.firstOrNull { it.source == source && it.resolved != null }
            }
        }

        return JSONObject().apply {
            put("screenshotMode", screenshotMode)
            put("shouldAggregate", shouldAggregate)
            put("selectedSource", selected?.source ?: "")
            put("selectedPackage", selected?.resolved?.packageName ?: "")
            put("selectedActivity", selected?.resolved?.activityName ?: "")
            put("sources", JSONArray().apply {
                snapshots.forEach { snapshot ->
                    put(JSONObject().apply {
                        put("source", snapshot.source)
                        put("raw", snapshot.raw)
                        put("resolvedPackage", snapshot.resolved?.packageName ?: "")
                        put("resolvedActivity", snapshot.resolved?.activityName ?: "")
                        put("score", snapshot.score ?: JSONObject.NULL)
                        put("selected", snapshot.source == selected?.source)
                    })
                }
            })
        }
    }

    private fun resolveCurrentComponentInfo(screenshotMode: String): CurrentComponentInfo? {
        val shouldAggregate = shouldAggregateForegroundCandidates()
        val preferredSources = if (shouldAggregate) {
            listOf(
                SOURCE_TOP_ACTIVITY,
                SOURCE_CURRENT_FOCUS,
                SOURCE_FOCUSED_WINDOW,
                SOURCE_FOCUSED_APP
            )
        } else {
            preferredSourcesForMode(screenshotMode)
        }

        if (shouldAggregate) {
            return preferredSources
                .mapNotNull(::resolveComponentFromSource)
                .maxByOrNull(::scoreResolvedComponent)
        }

        preferredSources.forEach { source ->
            val componentInfo = resolveComponentFromSource(source)
            if (componentInfo != null) {
                return componentInfo
            }
        }
        return null
    }

    
    @JvmStatic
    fun log(message: String) {
        Log.e("GlobalApi",message)
    }

    @JvmStatic
    fun sleep(millis: Int) {
        Thread.sleep(millis.toLong())
    }

    @JvmStatic
    fun sleep(millisMin: Int, millisMax: Int) {
        val range = (millisMin..millisMax).random()
        // SystemClock.sleep(range.toLong())
        Thread.sleep(range.toLong())
    }

    @JvmStatic
    fun random(min: Int, max: Int): Int {
        val range = (min..max).random()
        return range
    }

    @JvmStatic
    fun getCurrentPackage(): String {
        val screenshotMode = getScreenshotModeForCurrentComponent()
        val componentInfo = resolveCurrentComponentInfo(screenshotMode)
        if (componentInfo != null) {
            logJsApiDetect(
                "getCurrentPackage",
                "mode=$screenshotMode source=${componentInfo.source} result=${componentInfo.packageName}"
            )
            return componentInfo.packageName
        }
        if (currentPackageName.isNotEmpty()) {
            logJsApiDetect(
                "getCurrentPackage",
                "mode=$screenshotMode source=accessibility result=$currentPackageName"
            )
        }
        return currentPackageName
    }

    @JvmStatic
    fun getCurrentActivity(): String {
        val screenshotMode = getScreenshotModeForCurrentComponent()
        val componentInfo = resolveCurrentComponentInfo(screenshotMode)
        if (componentInfo != null) {
            val activity = normalizeActivityName(componentInfo.packageName, componentInfo.activityName)
            logJsApiDetect(
                "getCurrentActivity",
                "mode=$screenshotMode source=${componentInfo.source} package=${componentInfo.packageName} raw=${componentInfo.activityName} result=$activity"
            )
            return activity
        }
        val fallbackActivity = normalizeActivityName(currentPackageName, currentActivityName)
            .ifEmpty { currentActivityName }
        if (fallbackActivity.isNotEmpty()) {
            logJsApiDetect(
                "getCurrentActivity",
                "mode=$screenshotMode source=accessibility package=$currentPackageName raw=$currentActivityName result=$fallbackActivity"
            )
        }
        return fallbackActivity
    }

    @JvmStatic
    fun getForegroundDetectionSnapshot(): String {
        return buildForegroundDetectionSnapshotJson().toString()
    }

    @JvmStatic
    fun logForegroundDetectionSnapshot(): String {
        val snapshot = getForegroundDetectionSnapshot()
        logJsApiDetect("foregroundSnapshot", snapshot)
        LogApi.addLog("$JSAPI_DETECT_PREFIX type=foregroundSnapshot $snapshot", false)
        return snapshot
    }

    @JvmStatic
    fun getCurrentWindow(str: String): String {
        var keyWord = "mFocusedWindow=Window"
        if (str != "") {
            keyWord = str
        }
        val commandResult = ShellUtils.execute("dumpsys window | grep $keyWord")
        return commandResult.success
    }

    @JvmStatic
    fun click(x: Int, y: Int) {
        logJsApiTouch("action=click x=$x y=$y")
        handleClick(x, y, 150)
    }

    @JvmStatic
    fun longClick(x: Int, y: Int) {
        handleClick(x, y, 600)
    }

    @JvmStatic
    fun longClick(x: Int, y: Int, duration: Int) {
        handleClick(x, y, duration)
    }

    @JvmStatic
    fun press(x: Int, y: Int, duration: Int) {
        handleClick(x, y, duration)
    }

    @JvmStatic
    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, duration: Int) {
        logJsApiTouch("action=swipe x1=$x1 y1=$y1 x2=$x2 y2=$y2 duration=$duration")
        handleSwipe(x1, y1, x2, y2, duration)
    }

    @JvmStatic
    fun start() {
        EventBus.getDefault().post(TaskEvent(START_TASK))
    }

    @JvmStatic
    fun stop() {
        EventBus.getDefault().post(TaskEvent(STOP_TASK))
    }

    @JvmStatic
    fun next() {
        EventBus.getDefault().post(TaskEvent(NEXT_TASK))
    }


    @JvmStatic
    fun download(url: String, path: String): String {
        val tempPath = "${activity.cacheDir.absolutePath}/$path"
        var file = File(tempPath)
        if (file.exists()) {
            file.delete()
        }
        file = ImageHelper.download(url, tempPath, 20)
        logd("file=${file.absolutePath}")
        return file.absolutePath
    }

    @JvmStatic
    fun uninstallApk(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.setData(Uri.parse("package:$packageName"))
        getApp().startActivity(intent)
    }

    @JvmStatic
    fun installApk(path: String): Boolean {
        val file = File(path)
        if (!file.exists()) {
            return false
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    getApp(), "${getApp().packageName}.fileprovider", File(path)
                )
            } else {
                Uri.fromFile(File(path))
            }
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // if (!context.packageManager.canRequestPackageInstalls()) {
        //     val settingIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
        //         data = Uri.parse("package:${context.packageName}")
        //     }
        //     (context as Activity).startActivityForResult(settingIntent, REQUEST_INSTALL_PERMISSION)
        //     return
        // }
        getApp().startActivity(intent)
        return true
    }

    @JvmStatic
    fun changeFloat(position: String) {
        EventBus.getDefault().post(ChangeFloatEvent(position))
    }

    /**
    * 获取当前悬浮窗位置 (x,y)
    * @return 格式为 "x,y" 的字符串，例如 "120,350"
    */
    @JvmStatic
    fun getFloatPosition(): String {
        val mmkv = getMmkv()
        val x = mmkv.getInt("startX", 0)
        val y = mmkv.getInt("startY", 300)
        return "$x,$y"
    }
}
