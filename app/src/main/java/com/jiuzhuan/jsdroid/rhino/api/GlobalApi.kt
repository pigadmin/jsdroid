@file:JvmName("GlobalApi")

package com.jiuzhuan.jsdroid.rhino.api

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.jiuzhuan.jsdroid.api.getApp
import com.jiuzhuan.jsdroid.base.activity
import com.jiuzhuan.jsdroid.events.ChangeFloatEvent
import com.jiuzhuan.jsdroid.events.TaskEvent
import com.jiuzhuan.jsdroid.events.TaskEvent.Companion.NEXT_TASK
import com.jiuzhuan.jsdroid.events.TaskEvent.Companion.START_TASK
import com.jiuzhuan.jsdroid.events.TaskEvent.Companion.STOP_TASK
import com.jiuzhuan.jsdroid.rhino.helper.ImageHelper
import com.jiuzhuan.jsdroid.utils.ShellUtils
import com.jiuzhuan.jsdroid.utils.logd
import com.ss.android.ugc.aweme.live.livehostimpl.currentActivityName
import com.ss.android.ugc.aweme.live.livehostimpl.currentPackageName
import com.ss.android.ugc.aweme.live.livehostimpl.handleClick
import com.ss.android.ugc.aweme.live.livehostimpl.handleSwipe
import org.greenrobot.eventbus.EventBus
import java.io.File

object GlobalApi {
    @JvmStatic
    fun log(message: String) {
        Log.e("GlobalApi",message)
    }

    @JvmStatic
    fun sleep(millis: Int) {
        Thread.sleep(millis.toLong())
    }

    @JvmStatic
    fun sleep(millisMin: Int, millisMax: Int) {
        val range = (millisMin..millisMax).random()
        // SystemClock.sleep(range.toLong())
        Thread.sleep(range.toLong())
    }

    @JvmStatic
    fun random(min: Int, max: Int): Int {
        val range = (min..max).random()
        return range
    }

    @JvmStatic
    fun getCurrentPackage(): String {
        return currentPackageName
    }

    @JvmStatic
    fun getCurrentActivity(): String {
        return currentActivityName
    }

    @JvmStatic
    fun getCurrentWindow(str: String): String {
        var keyWord = "mFocusedWindow=Window"
        if (str != "") {
            keyWord = str
        }
        val commandResult = ShellUtils.execute("dumpsys window | grep $keyWord")
        return commandResult.success
    }

    @JvmStatic
    fun click(x: Int, y: Int) {
        handleClick(x, y, 150)
    }

    @JvmStatic
    fun longClick(x: Int, y: Int) {
        handleClick(x, y, 600)
    }

    @JvmStatic
    fun longClick(x: Int, y: Int, duration: Int) {
        handleClick(x, y, duration)
    }

    @JvmStatic
    fun press(x: Int, y: Int, duration: Int) {
        handleClick(x, y, duration)
    }

    @JvmStatic
    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, duration: Int) {
        handleSwipe(x1, y1, x2, y2, duration)
    }

    @JvmStatic
    fun start() {
        EventBus.getDefault().post(TaskEvent(START_TASK))
    }

    @JvmStatic
    fun stop() {
        EventBus.getDefault().post(TaskEvent(STOP_TASK))
    }

    @JvmStatic
    fun next() {
        EventBus.getDefault().post(TaskEvent(NEXT_TASK))
    }


    @JvmStatic
    fun download(url: String, path: String): String {
        val tempPath = "${activity.cacheDir.absolutePath}/$path"
        var file = File(tempPath)
        if (file.exists()) {
            file.delete()
        }
        file = ImageHelper.download(url, tempPath, 20)
        logd("file=${file.absolutePath}")
        return file.absolutePath
    }

    @JvmStatic
    fun uninstallApk(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.setData(Uri.parse("package:$packageName"))
        getApp().startActivity(intent)
    }

    @JvmStatic
    fun installApk(path: String): Boolean {
        val file = File(path)
        if (!file.exists()) {
            return false
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    getApp(), "${getApp().packageName}.fileprovider", File(path)
                )
            } else {
                Uri.fromFile(File(path))
            }
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // if (!context.packageManager.canRequestPackageInstalls()) {
        //     val settingIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
        //         data = Uri.parse("package:${context.packageName}")
        //     }
        //     (context as Activity).startActivityForResult(settingIntent, REQUEST_INSTALL_PERMISSION)
        //     return
        // }
        getApp().startActivity(intent)
        return true
    }

    @JvmStatic
    fun changeFloat(position: String) {
        EventBus.getDefault().post(ChangeFloatEvent(position))
    }
}
