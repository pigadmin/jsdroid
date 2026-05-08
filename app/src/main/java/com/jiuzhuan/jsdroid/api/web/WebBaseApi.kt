package com.jiuzhuan.jsdroid.api.web

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.JavascriptInterface
import com.jiuzhuan.jsdroid.BuildConfig
import com.jiuzhuan.jsdroid.R
import com.jiuzhuan.jsdroid.api.getApp
import org.json.JSONObject
import java.lang.reflect.Modifier
import kotlin.system.exitProcess

class WebBaseApi {
    private val TAG: String = javaClass.simpleName

    @JavascriptInterface
    fun getPackageName(): String {
        return BuildConfig.APPLICATION_ID
    }

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
    fun getBuildConfig(): String {
        return JSONObject().apply {
            BuildConfig::class.java.fields
                .filter { Modifier.isStatic(it.modifiers) && shouldExportBuildConfigField(it.name) }
                .sortedBy { it.name }
                .forEach { field ->
                    put(field.name, field.get(null) ?: JSONObject.NULL)
                }
        }.toString()
    }

    /**
     * 修复缺失的方法：过滤需要导出的BuildConfig字段
     */
    private fun shouldExportBuildConfigField(fieldName: String): Boolean {
        // 过滤掉不需要暴露给JS的字段，按需调整
        return !fieldName.startsWith("APPLICATION_ID") &&
               !fieldName.startsWith("DEBUG") &&
               !fieldName.startsWith("BUILD_TYPE")
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
            // 过滤系统应用
            if ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                // 修复逻辑bug：|| 改为 &&，否则判断永远生效
                if (app.packageName != "com.jiuzhuan.k24" && app.packageName != "com.jiuzhuan.jsdroid") {
                    packageList.add(app.packageName)
                }
            }
        }
        return packageList.joinToString(",")
    }
}