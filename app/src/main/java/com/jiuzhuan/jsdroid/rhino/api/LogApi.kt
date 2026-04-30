@file:JvmName("LogApi")

package com.jiuzhuan.jsdroid.rhino.api

import android.util.Log
import com.jiuzhuan.jsdroid.room.level_nomal
import com.jiuzhuan.jsdroid.utils.addLogs

object LogApi {
    @JvmStatic
    fun d(message: Any) {
        Log.d("LogApi", message.toString())
    }

    @JvmStatic
    fun e(message: Any) {
        Log.e("LogApi", message.toString())
    }

    @JvmStatic
    fun v(message: Any) {
        Log.v("LogApi", message.toString())
    }

    @JvmStatic
    fun addLog(message: String, showFloatToast: Boolean, level: String = level_nomal) {
        addLogs(message, showFloatToast, level)
    }

}
