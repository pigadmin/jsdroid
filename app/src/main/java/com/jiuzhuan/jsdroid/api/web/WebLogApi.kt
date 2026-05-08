package com.jiuzhuan.jsdroid.api.web

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import com.blankj.utilcode.util.LogUtils
import com.google.gson.Gson
import com.jiuzhuan.jsdroid.events.MessageEvent
import com.jiuzhuan.jsdroid.room.AppDatabase
import com.jiuzhuan.jsdroid.room.LogEntity
import com.jiuzhuan.jsdroid.room.level_nomal
import com.jiuzhuan.jsdroid.utils.BROADCAST_ACTION_MESSAGE
import com.jiuzhuan.jsdroid.utils.showException
import com.jiuzhuan.jsdroid.widget.Browser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

class WebLogApi(browser: Browser) {
    private companion object {
        const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L
    }

    val b = browser

    private data class LogsPageResult(
        val list: List<LogEntity>,
        val page: Int,
        val pageSize: Int,
        val total: Int,
        val pageCount: Int,
        val error: String? = null,
    )

    @JavascriptInterface
    fun d(message: String) {
        Log.d("WebLogApi", message)
    }

    @JavascriptInterface
    fun e(message: String) {
        Log.e("WebLogApi", message)
    }

    @JavascriptInterface
    fun v(message: String) {
        Log.v("WebLogApi", message)
    }

    @JavascriptInterface
    fun addLog(message: String, level: String = level_nomal, showFloatToast: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            val log = LogEntity(
                timestamp = System.currentTimeMillis(), level = level, message = message
            )
            AppDatabase.getInstance().logDao().insert(log)
        }
        if (showFloatToast) {
            EventBus.getDefault().post(MessageEvent(message))
        }
    }

    @JavascriptInterface
    fun getLogs(startTime: Long, endTime: Long, pageSize: Int = 20, page: Int = 1, callbackId: String) {
        queryLogs(startTime, endTime, pageSize, page, callbackId)
    }

    @JavascriptInterface
    fun getLogsByDay(day: Int, pageSize: Int = 20, page: Int = 1, callbackId: String) {
        val safeDay = day.coerceAtLeast(1)
        val endTime = System.currentTimeMillis()
        val startTime = endTime - safeDay * DAY_IN_MILLIS
        queryLogs(startTime, endTime, pageSize, page, callbackId)
    }

    private fun queryLogs(
        startTime: Long,
        endTime: Long,
        pageSize: Int,
        page: Int,
        callbackId: String,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val gson = Gson()
            val safeStartTime = minOf(startTime, endTime).coerceAtLeast(0L)
            val safeEndTime = maxOf(startTime, endTime).coerceAtLeast(safeStartTime)
            val safePageSize = pageSize.coerceIn(1, 200)
            val safePage = page.coerceAtLeast(1)
            try {
                val offset = (safePage - 1) * safePageSize
                val logDao = AppDatabase.getInstance().logDao()
                val logs = logDao.getByTimeRange(
                    safeStartTime,
                    safeEndTime,
                    limit = safePageSize,
                    offset = offset,
                )
                val total = logDao.countByTimeRange(safeStartTime, safeEndTime)
                val pageCount = if (total == 0) 0 else (total + safePageSize - 1) / safePageSize
                val payload = LogsPageResult(
                    list = logs,
                    page = safePage,
                    pageSize = safePageSize,
                    total = total,
                    pageCount = pageCount,
                )

                val json = gson.toJson(payload)
                sendResultToJs(callbackId, json)
            } catch (e: Exception) {
                showException(e)
                val payload = LogsPageResult(
                    list = emptyList(),
                    page = safePage,
                    pageSize = safePageSize,
                    total = 0,
                    pageCount = 0,
                    error = e.message ?: "getLogs failed",
                )
                sendResultToJs(callbackId, gson.toJson(payload))
            }
        }
    }

    @JavascriptInterface
    fun clearLogs() {
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getInstance().logDao().delete()
        }
    }

    private fun sendResultToJs(callbackId: String, result: String) {
        Handler(Looper.getMainLooper()).post {
            val jsCode = "window.handleLogsResult('$callbackId', $result)"
            b.evaluateJavascript(jsCode, null)
        }
    }
}