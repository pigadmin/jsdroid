package com.jiuzhuan.jsdroid.api.web

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.webkit.JavascriptInterface
import com.jiuzhuan.jsdroid.api.getApp
import com.jiuzhuan.jsdroid.base.activity
import com.jiuzhuan.jsdroid.events.ChangeFloatEvent
import com.jiuzhuan.jsdroid.events.TaskEvent
import com.jiuzhuan.jsdroid.events.TaskEvent.Companion.START_TASK
import com.jiuzhuan.jsdroid.events.TaskEvent.Companion.STOP_TASK
import com.jiuzhuan.jsdroid.rhino.api.GlobalApi
import com.jiuzhuan.jsdroid.service.FloatLogoService
import com.jiuzhuan.jsdroid.utils.showException
import com.ss.android.ugc.aweme.live.livehostimpl.AudioAccessibilityService
import com.ss.android.ugc.aweme.live.livehostimpl.rhinoService
import org.greenrobot.eventbus.EventBus

class WebJsApi {
    @JavascriptInterface
    fun isRunning(): Boolean {
        return AudioAccessibilityService.isRunning()
    }

    @JavascriptInterface
    fun start() {
        if (rhinoService == null) {
            openAccessibility()
        } else {
            try {
                val activityManager =
                    getApp().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.appTasks.first().setExcludeFromRecents(true)
            } catch (e: Exception) {
                showException(e)
            }finally {
                EventBus.getDefault().post(TaskEvent(START_TASK))
            }
        }
    }

    @JavascriptInterface
    fun stop() {
        EventBus.getDefault().post(TaskEvent(STOP_TASK))
    }

    @JavascriptInterface
    fun getForegroundDetectionSnapshot(): String {
        return GlobalApi.getForegroundDetectionSnapshot()
    }

    @JavascriptInterface
    fun logForegroundDetectionSnapshot(): String {
        return GlobalApi.logForegroundDetectionSnapshot()
    }

    @JavascriptInterface
    fun gc() {
        System.gc()
    }

    @JavascriptInterface
    fun openFloat() {
        val context = getApp()
        val intent = Intent(context, FloatLogoService::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startService(intent)
    }

    @JavascriptInterface
    fun changeFloat(position: String) {
        EventBus.getDefault().post(ChangeFloatEvent(position))
    }

    private fun openAccessibility() {
        AlertDialog.Builder(activity).setTitle("需要无障碍权限").setMessage("请前往设置开启服务")
            .setPositiveButton("去开启") { _, _ ->
                activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }.show()
    }
}
