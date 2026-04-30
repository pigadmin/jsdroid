package com.jiuzhuan.jsdroid.api.web

import android.annotation.SuppressLint
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.webkit.JavascriptInterface
import com.jiuzhuan.jsdroid.api.getApp
import com.jiuzhuan.jsdroid.utils.getScreenInfo
import com.jiuzhuan.jsdroid.utils.showException
import java.util.UUID

class WebDeviceApi {
    private val TAG: String = javaClass.simpleName

    @JavascriptInterface
    fun getUUID(prefix: String): String {
        try {
            val androidId = getAndroidID()
            if (!TextUtils.isEmpty(androidId)) {
                return getUdid(prefix + 2, androidId)
            }
        } catch (e: Exception) {
            showException(e)
        }
        return getUdid(prefix + 9, "")
    }

    @JavascriptInterface
    @SuppressLint("HardwareIds")
    fun getAndroidID(): String {
        val id = Settings.Secure.getString(
            getApp().contentResolver, Settings.Secure.ANDROID_ID
        )
        if ("9774d56d682e549c" == id) return ""
        return id ?: ""
    }

    private fun getUdid(prefix: String, id: String): String {
        if (id == "") {
            return prefix + UUID.randomUUID().toString().replace("-", "")
        }
        return prefix + UUID.nameUUIDFromBytes(id.toByteArray()).toString().replace("-", "")
    }

    @JavascriptInterface
    fun getInfo(): String {
        val (width, height, density) = getScreenInfo()
        return "${Build.MANUFACTURER}---${Build.MODEL}---${width}x${height}-${density}"
    }
}
