package com.jiuzhuan.jsdroid.utils

import android.graphics.Bitmap
import android.media.Image
import android.util.Log
import androidx.core.graphics.createBitmap
import com.jiuzhuan.jsdroid.BuildConfig
import com.jiuzhuan.jsdroid.events.MessageEvent
import com.jiuzhuan.jsdroid.room.AppDatabase
import com.jiuzhuan.jsdroid.room.LogEntity
import com.jiuzhuan.jsdroid.room.level_nomal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.greenrobot.eventbus.EventBus
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

fun addLogs(message: String, showFloatToast: Boolean = false, level: String = level_nomal) {
    CoroutineScope(Dispatchers.IO).launch {
        val log = LogEntity(
            timestamp = System.currentTimeMillis(), level = level, message = message
        )
        AppDatabase.getInstance().logDao().insert(log)
    }
    if (showFloatToast) {
        EventBus.getDefault().post(MessageEvent(message))
    }
}


val LINE_SEP: String = System.getProperty("line.separator")
val okHttpClient =
    OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS).build()

fun fetchJsContent(url: String): String {
    try {
        val request: Request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        val body = response.body
        return body?.string() ?: ""
    } catch (e: IOException) {
        showException(e)
        return ""
    }
}

fun logd(vararg msg: String) {
    if (BuildConfig.DEBUG) {
        Log.e("Utils", msg.toString())
    }
}

fun showException(e: Exception) {
    if (BuildConfig.DEBUG) {
        e.printStackTrace()
    }
}

fun execCmd(
    vararg commands: String
): String {
    var result = -1
    if (commands.isEmpty()) {
        return ""
    }
    var process: Process? = null
    var successResult: BufferedReader? = null
    var successMsg: StringBuilder? = null

    var os: DataOutputStream? = null
    try {
        val isRooted = false
        process = Runtime.getRuntime().exec(if (isRooted) "su" else "sh", null, null)
        os = DataOutputStream(process.outputStream)
        for (command in commands) {
            os.write(command.toByteArray())
            os.writeBytes(LINE_SEP)
            os.flush()
        }
        os.writeBytes("exit$LINE_SEP")
        os.flush()
        result = process.waitFor()
        if (result != 0) {
            return ""
        }
        successMsg = StringBuilder()
        successResult = BufferedReader(
            InputStreamReader(process.inputStream, "UTF-8")
        )

        var line: String?
        if ((successResult.readLine().also { line = it }) != null) {
            successMsg.append(line)
            while ((successResult.readLine().also { line = it }) != null) {
                successMsg.append("@@").append(line)
            }
        }
    } catch (e: Exception) {
        showException(e)
        return ""
    } finally {
        try {
            os?.close()
            successResult?.close()
            process?.destroy()
        } catch (e: IOException) {
            showException(e)
        }
    }
//    logd(*commands, successMsg.toString())
    return successMsg.toString()
}

fun Image.toBitmap(): Bitmap {
    val planes = this.planes
    val buffer = planes[0].buffer
    val pixelStride = planes[0].pixelStride
    val rowStride = planes[0].rowStride
    val rowPadding = rowStride - pixelStride * width

    val bitmap = createBitmap(width + rowPadding / pixelStride, height)
    bitmap.copyPixelsFromBuffer(buffer)
    return bitmap
}
