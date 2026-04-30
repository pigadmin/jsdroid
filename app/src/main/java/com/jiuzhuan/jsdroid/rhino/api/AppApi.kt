@file:JvmName("AppApi")

package com.jiuzhuan.jsdroid.rhino.api

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.jiuzhuan.jsdroid.BuildConfig
import com.jiuzhuan.jsdroid.R
import com.jiuzhuan.jsdroid.api.getApp
import com.jiuzhuan.jsdroid.utils.showException


object AppApi {

    @JvmStatic
    fun appName(): String {
        return getApp().resources.getString(R.string.app_name)
    }

    @JvmStatic
    fun versionCode(): Int {
        return BuildConfig.VERSION_CODE
    }

    @JvmStatic
    fun versionName(): String {
        return BuildConfig.VERSION_NAME
    }

    @JvmStatic
    fun launchApp(appName: String): Boolean {
        val packageManager = getApp().packageManager
        val apps = packageManager?.getInstalledApplications(PackageManager.GET_META_DATA)

        var targetPackageName: String? = null
        if (apps != null) {
            for (app in apps) {
                val appLabel = packageManager.getApplicationLabel(app).toString()
                if (appLabel.equals(appName, ignoreCase = true)) {
                    targetPackageName = app.packageName
                    break
                }
            }
        }

        targetPackageName?.let { pkg ->
            val intent = packageManager?.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                getApp().startActivity(intent)
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun launchPackage(packageName: String): Boolean {
        try {
            val packageManager = getApp().packageManager
            val intent = packageManager?.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                getApp().startActivity(intent)
                return true
            }
        } catch (e: Exception) {
            showException(e)
        }
        return false
    }

    @JvmStatic
    fun getPackageName(appName: String): String {
        val packageManager = getApp().packageManager
        val apps = packageManager?.getInstalledApplications(PackageManager.GET_META_DATA)

        if (apps != null) {
            for (app in apps) {
                val currentAppName = packageManager?.getApplicationLabel(app).toString()
                if (currentAppName.equals(appName, ignoreCase = true)) {
                    return app.packageName
                }
            }
        }
        return ""
    }

    @JvmStatic
    fun getAppName(packageName: String): String {
        return try {
            val packageManager = getApp().packageManager
            val appInfo = packageManager?.getApplicationInfo(packageName, 0)
            if (appInfo != null) {
                packageManager?.getApplicationLabel(appInfo).toString()
            } else {
                ""
            }
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        } catch (e: Exception) {
            ""
        }
    }

    @JvmStatic
    fun openAppSetting(packageName: String): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.setData(Uri.parse("package:$packageName"))
        getApp().startActivity(intent)
        return true
    }

    fun viewFile(path: String) {
    }

    fun editFile(path: String) {
    }

    fun uninstall(packageName: String) {
    }

    fun openUrl(url: String) {
    }

    fun sendEmail(options: Any) {
    }

    fun startActivity(name: String) {
    }

    fun intent(options: Any) {
    }

    fun startActivity(options: Any) {
    }

    fun sendBroadcast(options: Any) {
    }

    fun startService(options: Any) {
    }

    fun sendBroadcast(name: String) {
    }

    fun intentToShell(options: Any) {
    }

    fun parseUri(uri: String) {
    }

    fun getUriForFile(path: String) {
    }

    @JvmStatic
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
