package com.jiuzhuan.jsdroid

import android.app.Application
import android.webkit.WebView
import com.jiuzhuan.jsdroid.api.initSdk
import com.jiuzhuan.jsdroid.rhino.api.DisplayApi

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        initSdk(this)
        DisplayApi.init(this)
        System.loadLibrary("opencv_java4")
        WebView.setDataDirectorySuffix(getProcessName())
    }
}
