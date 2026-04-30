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
    val b = browser

    @JavascriptInterface
    fun d(message: String) {
        Log.d("WebLogApi",message)
    }

    @JavascriptInterface
    fun e(message: String) {
        Log.e("WebLogApi",message)
    }

    @JavascriptInterface
    fun v(message: String) {
        Log.v("WebLogApi",message)
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
    fun getLogs(day: Int, pageSize: Int = 20, page: Int = 1, callbackId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var start = System.currentTimeMillis() - day * 60 * 60 * 1000
                var end = System.currentTimeMillis()

                val limit = pageSize
                val offset = page

                val logs = AppDatabase.getInstance().logDao()
                    .getByDate(start, end, limit = limit, offset = (offset - 1) * limit)

                val json = Gson().toJson(logs)
//                logd(json)
                sendResultToJs(callbackId, json)
            } catch (e: Exception) {
                showException(e)
                sendResultToJs(callbackId, "{ error: '${e.message}' }")
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
