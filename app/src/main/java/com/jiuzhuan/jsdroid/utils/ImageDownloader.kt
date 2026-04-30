package com.jiuzhuan.jsdroid.utils

import com.jiuzhuan.jsdroid.api.getApp
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object ImageDownloader {
    interface DownloadCallback {
        fun onSuccess(file: File)
        fun onFailure(e: String)
    }

    fun downloadImage(
        url: String, savePath: String, timeout: Long = 30, callback: DownloadCallback
    ) {
        try {
//            val dir = getOrCreateDirectory(DOWNLOAD_DIR)
            // check file
            val outputFile = File(savePath)
            if (outputFile.exists()) outputFile.delete()

            // config
            val client = OkHttpClient.Builder().connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS).writeTimeout(timeout, TimeUnit.SECONDS)
                .build()

            // download
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logd("HTTP error: ${response.code}")
                    callback.onFailure("HTTP error: ${response.code}")
                }

                response.body?.let { body ->
                    FileOutputStream(outputFile).use { fos ->
                        fos.write(body.bytes())
                    }
                    callback.onSuccess(outputFile)
                } ?: logd("Empty response body")
            }
        } catch (e: Exception) {
            showException(e)
            callback.onFailure(e.toString())
        }
    }

    fun downloadImageAndDir(
        url: String,
        dirName: String,
        fileName: String,
        timeout: Long = 30,
        callback: DownloadCallback
    ) {
        try {
            val dir = getOrCreateDirectory(dirName)

            // create file
            val outputFile = File(dir, fileName)
            if (outputFile.exists()) outputFile.delete()

            // config
            val client = OkHttpClient.Builder().connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS).writeTimeout(timeout, TimeUnit.SECONDS)
                .build()

            // download
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logd("HTTP error: ${response.code}")
                }

                response.body?.let { body ->
                    FileOutputStream(outputFile).use { fos ->
                        fos.write(body.bytes())
                    }
                    callback.onSuccess(outputFile)
                } ?: logd("Empty response body")
            }
        } catch (e: Exception) {
            showException(e)
            callback.onFailure(e.toString())
        }
    }

    private fun getOrCreateDirectory(
        dirName: String
    ): File {
        val dir = File(getApp().getExternalFilesDir(null), dirName)
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                logd("Failed to create directory: ${dir.absolutePath}")
            }
        }
        return dir
    }
}
