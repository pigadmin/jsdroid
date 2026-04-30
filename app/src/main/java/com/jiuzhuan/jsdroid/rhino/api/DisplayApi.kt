@file:JvmName("DisplayApi")

package com.jiuzhuan.jsdroid.rhino.api

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.jiuzhuan.jsdroid.utils.showException

object DisplayApi {
    private var mDisplayWidth = 0
    private var mDisplayHeight = 0
    private var mDisplayDensityDpi = 0

    fun init(context: Context) {
        try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val displayMetrics = DisplayMetrics()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = windowManager.currentWindowMetrics
                mDisplayWidth = windowMetrics.bounds.width()
                mDisplayHeight = windowMetrics.bounds.height()
                mDisplayDensityDpi = context.resources.configuration.densityDpi
            } else {
                windowManager.defaultDisplay.getRealMetrics(displayMetrics)
                mDisplayWidth = displayMetrics.widthPixels
                mDisplayHeight = displayMetrics.heightPixels
                mDisplayDensityDpi = displayMetrics.densityDpi
            }
        } catch (e: Exception) {
            showException(e)
        }
    }

    @JvmStatic
    fun getDisplayWidth(): Int = mDisplayWidth

    @JvmStatic
    fun getDisplayHeight(): Int = mDisplayHeight

    @JvmStatic
    fun getDisplayDensityDpi(): Int = mDisplayDensityDpi
}
