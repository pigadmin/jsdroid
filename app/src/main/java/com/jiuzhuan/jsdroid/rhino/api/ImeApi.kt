@file:JvmName("ToastApi")

package com.jiuzhuan.jsdroid.rhino.api

import android.content.Intent
import com.jiuzhuan.jsdroid.api.getApp
import com.jiuzhuan.jsdroid.utils.ShellUtils


object ImeApi {

    @JvmStatic
    fun setMethod(methodName: String) {
        ShellUtils.execute("ime set $methodName")
    }

    @JvmStatic
    fun input(str: String?) {
        val app = getApp()
        val intent = Intent("IME_INPUT")
        intent.putExtra("data", str)
        app.sendBroadcast(intent)
    }

}
