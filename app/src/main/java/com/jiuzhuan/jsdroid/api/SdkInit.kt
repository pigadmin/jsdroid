package com.jiuzhuan.jsdroid.api

import android.app.Application
import com.tencent.mmkv.MMKV

var sMmkv: MMKV? = null
var sApp: Application? = null

fun initSdk(app: Application) {
    sApp = app

    MMKV.initialize(app)
    sMmkv = MMKV.defaultMMKV()
}

fun getApp(): Application {
    val app = sApp
    if (app != null) return app else throw IllegalStateException("sApp can't be null.")
}

fun getMmkv(): MMKV {
    val mmkv = sMmkv
    if (mmkv != null) return mmkv else throw IllegalStateException("sMmkv can't be null.")
}
