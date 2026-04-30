package com.jiuzhuan.jsdroid.api.web

import android.webkit.JavascriptInterface
import com.tencent.mmkv.MMKV

class WebMmkvApi {
    private val TAG: String = javaClass.simpleName

    private val mmkv = MMKV.defaultMMKV()

    @JavascriptInterface
    fun put(key: String, value: Any) {
        when (value) {
            is Boolean -> mmkv.putBoolean(key, value)
            is Int -> mmkv.putInt(key, value)
            is Float -> mmkv.putFloat(key, value)
            is Long -> mmkv.putLong(key, value)
            else -> mmkv.putString(key, value.toString())
        }
    }

    @JavascriptInterface
    fun get(key: String): Any? {
        return if (!containsKey(key)) {
            false
        } else {
            mmkv.getString(key, "")
        }
    }

    @JavascriptInterface
    fun putString(key: String, value: String) {
        mmkv.putString(key, value.toString())
    }

    @JavascriptInterface
    fun getString(key: String): String? {
        return if (!containsKey(key)) {
            ""
        } else {
            mmkv.getString(key, "")
        }
    }

    @JavascriptInterface
    fun getBoolean(key: String): Boolean {
        return if (!containsKey(key)) {
            false
        } else {
            return mmkv.getBoolean(key, false)
        }
    }

    @JavascriptInterface
    fun getInt(key: String): Int {
        return if (containsKey(key)) {
            0
        } else {
            return mmkv.getInt(key, 0)
        }
    }

    @JavascriptInterface
    fun getFloat(key: String): Float {
        return if (containsKey(key)) {
            0f
        } else {
            return mmkv.getFloat(key, 0f)
        }
    }

    @JavascriptInterface
    fun getLong(key: String): Long {
        return if (containsKey(key)) {
            0L
        } else {
            return mmkv.getLong(key, 0L)
        }
    }

    @JavascriptInterface
    fun containsKey(key: String): Boolean {
        return mmkv.containsKey(key)
    }

    @JavascriptInterface
    fun remove(key: String): Boolean {
        return if (containsKey(key)) {
            false
        } else {
            mmkv.remove(key)
            true
        }
    }

    @JavascriptInterface
    fun clearAll(): Boolean {
        mmkv.clearAll()
        return true
    }
}
