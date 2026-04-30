package com.jiuzhuan.jsdroid.api.web

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.jiuzhuan.jsdroid.api.getApp

class WebToastApi {
    private val TAG: String = javaClass.simpleName

    private val uiHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun showShort(text: String) {
        uiHandler.post {
            Toast.makeText(getApp(), text, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun showLong(text: String) {
        uiHandler.post {
            Toast.makeText(getApp(), text, Toast.LENGTH_LONG).show()
        }
    }

    @JavascriptInterface
    fun toast(text: String, isLong: Boolean) {
        uiHandler.post {
            if (isLong) {
                Toast.makeText(getApp(), text, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(getApp(), text, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
