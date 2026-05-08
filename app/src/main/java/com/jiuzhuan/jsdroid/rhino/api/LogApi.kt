@file:JvmName("LogApi")

package com.jiuzhuan.jsdroid.rhino.api

import android.util.Log
import com.jiuzhuan.jsdroid.room.level_nomal
import com.jiuzhuan.jsdroid.utils.addLogs

object LogApi {
    private const val MAX_LOG_LENGTH = 4000 // Android Log 单条最大长度限制

    // 分页输出 Log.d
    @JvmStatic
    fun d(message: Any) {
        logWithChunk(Log.DEBUG, "LogApi", message.toString())
    }

    // 分页输出 Log.e
    @JvmStatic
    fun e(message: Any) {
        logWithChunk(Log.ERROR, "LogApi", message.toString())
    }

    // 分页输出 Log.v
    @JvmStatic
    fun v(message: Any) {
        logWithChunk(Log.VERBOSE, "LogApi", message.toString())
    }

    // 辅助函数：将长消息分页后输出到 Logcat
    private fun logWithChunk(priority: Int, tag: String, message: String) {
        val length = message.length
        if (length <= MAX_LOG_LENGTH) {
            when (priority) {
                Log.DEBUG -> Log.d(tag, message)
                Log.ERROR -> Log.e(tag, message)
                Log.VERBOSE -> Log.v(tag, message)
                else -> Log.println(priority, tag, message)
            }
            return
        }

        // 分页输出
        var start = 0
        var chunkCount = 1
        while (start < length) {
            var end = start + MAX_LOG_LENGTH
            if (end >= length) end = length
            val chunk = message.substring(start, end)
            when (priority) {
                Log.DEBUG -> Log.d(tag, "$chunk [chunk $chunkCount]")
                Log.ERROR -> Log.e(tag, "$chunk [chunk $chunkCount]")
                Log.VERBOSE -> Log.v(tag, "$chunk [chunk $chunkCount]")
                else -> Log.println(priority, tag, "$chunk [chunk $chunkCount]")
            }
            start = end
            chunkCount++
        }
    }

    // -------- addLog 重载（兼容原有调用）--------

    @JvmStatic
    fun addLog(message: String, showFloatToast: Boolean) {
        addLogs(message, showFloatToast, level_nomal, true)
    }

    // 3参数版本：兼容旧 JS 代码（不传 log 参数），默认写入日志文件
    @JvmStatic
    fun addLog(message: String, showFloatToast: Boolean, level: String) {
        addLogs(message, showFloatToast, level, true)
    }

    // 4参数版本：完全控制，支持不写入日志文件
    @JvmStatic
    fun addLog(message: String, showFloatToast: Boolean, level: String, log: Boolean) {
        addLogs(message, showFloatToast, level, log)
    }

    // -------- 多参数版本（新增）--------
    // 支持传入多个消息片段，自动拼接后调用 addLogs
    @JvmStatic
    fun addLog(vararg messages: String, showFloatToast: Boolean = false, level: String = level_nomal, log: Boolean = true) {
        val combinedMessage = messages.joinToString(separator = "")
        addLogs(combinedMessage, showFloatToast, level, log)
    }
}
