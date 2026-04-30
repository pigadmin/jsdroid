@file:JvmName("StoragesApi")

package com.jiuzhuan.jsdroid.rhino.api

import com.tencent.mmkv.MMKV

object StoragesApi {
    private val mmkv = MMKV.defaultMMKV()

    @JvmStatic
    fun put(key: String, value: Any) {
        when (value) {
            is Boolean -> mmkv.putBoolean(key, value)
            is Int -> mmkv.putInt(key, value)
            is Float -> mmkv.putFloat(key, value)
            is Long -> mmkv.putLong(key, value)
            else -> mmkv.putString(key, value.toString())
        }
    }

    @JvmStatic
    fun get(key: String): Any? {
        return if (!containsKey(key)) {
            false
        } else {
            mmkv.getString(key, "")
        }
    }

    @JvmStatic
    fun putString(key: String, value: String) {
        mmkv.putString(key, value.toString())
    }

    @JvmStatic
    fun getString(key: String): String? {
        return if (!containsKey(key)) {
            ""
        } else {
            mmkv.getString(key, "")
        }
    }

    @JvmStatic
    fun getBoolean(key: String): Boolean {
        return if (!containsKey(key)) {
            false
        } else {
            return mmkv.getBoolean(key, false)
        }
    }

    @JvmStatic
    fun getInt(key: String): Int {
        return if (containsKey(key)) {
            0
        } else {
            return mmkv.getInt(key, 0)
        }
    }

    @JvmStatic
    fun getFloat(key: String): Float {
        return if (containsKey(key)) {
            0f
        } else {
            return mmkv.getFloat(key, 0f)
        }
    }

    @JvmStatic
    fun getLong(key: String): Long {
        return if (containsKey(key)) {
            0L
        } else {
            return mmkv.getLong(key, 0L)
        }
    }

    @JvmStatic
    fun containsKey(key: String): Boolean {
        return mmkv.containsKey(key)
    }

    @JvmStatic
    fun remove(key: String): Boolean {
        return if (containsKey(key)) {
            false
        } else {
            mmkv.remove(key)
            true
        }
    }

    @JvmStatic
    fun clearAll(): Boolean {
        mmkv.clearAll()
        return true
    }
}
