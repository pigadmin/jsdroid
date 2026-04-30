@file:JvmName("ToastApi")

package com.jiuzhuan.jsdroid.rhino.api

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.jiuzhuan.jsdroid.api.getApp

object ToastApi {
    private val uiHandler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun shortToast(text: String) {
        uiHandler.post {
            Toast.makeText(getApp(), text, Toast.LENGTH_SHORT).show()
        }
    }

    @JvmStatic
    fun langToast(text: String) {
        uiHandler.post {
            Toast.makeText(getApp(), text, Toast.LENGTH_LONG).show()
        }
    }
}
