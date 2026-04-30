package com.jiuzhuan.jsdroid.rhino.helper

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.TimeUnit

object ImageHelper {
    fun download(imageUrl: String, imagePath: String, timeout: Long): File {
        val outputFile = File(imagePath)
        if (outputFile.exists()) outputFile.delete()

        val client = OkHttpClient.Builder().connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS).writeTimeout(timeout, TimeUnit.SECONDS).build()

        val request = Request.Builder().url(imageUrl).build()

        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val body = response.body ?: throw IllegalStateException("res 404")
            FileOutputStream(outputFile).use { fos ->
                fos.write(body.bytes())
                fos.flush()
            }
            return outputFile
        }
        val fileName = URL(imageUrl).path.substringAfterLast("/")
        throw IllegalStateException(" $fileName download fail")
    }
}
