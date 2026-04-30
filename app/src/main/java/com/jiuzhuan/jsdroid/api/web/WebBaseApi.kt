package com.jiuzhuan.jsdroid.api.web

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.JavascriptInterface
import com.jiuzhuan.jsdroid.BuildConfig
import com.jiuzhuan.jsdroid.R
import com.jiuzhuan.jsdroid.api.getApp
import kotlin.system.exitProcess

class WebBaseApi {
    private val TAG: String = javaClass.simpleName

    @JavascriptInterface
    fun getAppVersionCode(): Int {
        return BuildConfig.VERSION_CODE
    }

    @JavascriptInterface
    fun getAppVersionName(): String {
        return BuildConfig.VERSION_NAME
    }

    @JavascriptInterface
    fun getAppName(): String {
        return getApp().resources.getString(R.string.app_name)
    }

    @JavascriptInterface
    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        getApp().startActivity(intent)
    }

    @JavascriptInterface
    fun exit() {
        exitProcess(0)
    }

    @JavascriptInterface
    fun getInstalledApps(): String {
        val packageManager = getApp().packageManager
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val packageList = arrayListOf<String>()
        for (app in apps) {
            if ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                if (app.packageName != "com.jiuzhuan.k24" || app.packageName != "com.jiuzhuan.jsdroid") {
                    packageList.add(app.packageName)
                }
            }
        }
        return packageList.joinToString(",")
    }
}
