@file:JvmName("ImageApi")

package com.jiuzhuan.jsdroid.rhino.api

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.media.Image
import android.util.Log
import androidx.core.graphics.createBitmap
import com.jiuzhuan.jsdroid.BuildConfig
import com.jiuzhuan.jsdroid.api.getApp
import com.jiuzhuan.jsdroid.api.getMmkv
import com.jiuzhuan.jsdroid.opencv.ImageMatcher.getImagePoints
import com.jiuzhuan.jsdroid.opencv.ImageMatcher.templateMatch
import com.jiuzhuan.jsdroid.opencv.ImageMatcher.templateMatchG
import com.jiuzhuan.jsdroid.rhino.api.GlobalApi.sleep
import com.jiuzhuan.jsdroid.rhino.helper.ImageHelper
import com.jiuzhuan.jsdroid.rhino.helper.Screencap
import org.opencv.core.Point
import java.io.File
import java.io.FileOutputStream
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("StaticFieldLeak")
object ImageApi {
    private val mMmkv = getMmkv()
    private val mApp = getApp()

    private fun logd(message: String) {
        if (BuildConfig.DEBUG) {
            Log.e("ImageApi", message)
        }
    }

    // 统一的错误日志：输出到 logcat 和用户面板
    private fun logError(message: String) {
        Log.e("ImageApi", message)
        LogApi.addLog(message, false, "error", true)
    }

    @JvmStatic
    @Synchronized
    fun takeCapture(id: String, area: String): String? {
        return try {
            val newId = id.ifEmpty { "1" }
            logd("takeCapture: id=$newId, area=$area")
            val imgPath = "${mApp.cacheDir.absolutePath}/$newId.jpg"
            var image: Image? = null
            var bitmap: Bitmap? = null
            try {
                for (i in 1..10) {
                    image = Screencap.getImageReader().acquireLatestImage()
                    if (image != null) break
                    sleep(16)
                }
                bitmap = safeCrop(image?.toBitmap(), area)
                if (bitmap == null) {
                    logError("截图或裁剪失败，bitmap 为 null")
                    return null
                }
                if (bitmap.save(imgPath, 50)) {
                    logd("保存成功 $imgPath")
                } else {
                    logError("截图保存失败: $imgPath")
                    return null
                }
            } finally {
                image?.close()
                bitmap?.recycle()
            }
            imgPath
        } catch (e: Exception) {
            logError("takeCapture 异常: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    @Synchronized
    fun findImage(id: String, template: String, threshold: Double): Point? {
        return try {
            val newId = id.ifEmpty { "1" }
            logd("findImage: id=$newId, template=$template, threshold=$threshold")
            val resUrl = mMmkv.getString("resUrl", "")
            val imgUrl = resUrl + template
            val tempFile = template.replace("/", "_")
            val tempPath = "${mApp.cacheDir.absolutePath}/$tempFile"
            val imgFile = File(tempPath)

            val templatePath = if (imgFile.exists()) {
                imgFile.absolutePath
            } else {
                val file = ImageHelper.download(imgUrl, tempPath, 60)
                if (file == null) {
                    logError("图片下载失败(可能404): $imgUrl")
                    return null
                }
                if (!file.exists()) {
                    logError("下载后的文件不存在: ${file.absolutePath}")
                    return null
                }
                logd("下载成功 file=${file.absolutePath}")
                file.absolutePath
            }

            if (!File(templatePath).exists()) {
                logError("模板文件不存在(404): $templatePath")
                return null
            }

            val match = templateMatch(newId, templatePath, threshold) ?: return null
            logd("离线/在线找图, 模版: $newId, 小图: $templatePath, 匹配度: $threshold, 返回: $match")
            match
        } catch (e: Exception) {
            logError("findImage 异常: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    @Synchronized
    fun findImageG(id: String, template: String, threshold: Double): Point? {
        return try {
            val newId = id.ifEmpty { "1" }
            logd("findImageG: id=$newId, template=$template, threshold=$threshold")
            val resUrl = mMmkv.getString("resUrl", "")
            val imgUrl = resUrl + template
            val tempFile = template.replace("/", "_")
            val tempPath = "${mApp.cacheDir.absolutePath}/$tempFile"
            val imgFile = File(tempPath)

            val templatePath = if (imgFile.exists()) {
                imgFile.absolutePath
            } else {
                val file = ImageHelper.download(imgUrl, tempPath, 60)
                if (file == null) {
                    logError("图片下载失败(可能404): $imgUrl")
                    return null
                }
                if (!file.exists()) {
                    logError("下载后的文件不存在: ${file.absolutePath}")
                    return null
                }
                logd("下载成功 file=${file.absolutePath}")
                file.absolutePath
            }

            if (!File(templatePath).exists()) {
                logError("模板文件不存在(404): $templatePath")
                return null
            }

            val match = templateMatchG(newId, templatePath, threshold) ?: return null
            logd("离线/在线灰度找图, 模版: $newId, 小图: $templatePath, 匹配度: $threshold, 返回: $match")
            match
        } catch (e: Exception) {
            logError("findImageG 异常: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    @Synchronized
    fun findImages(
        id: String, template: String, threshold: Double, maxMatches: Int
    ): List<Point>? {
        return try {
            val newId = id.ifEmpty { "1" }
            logd("findImages: id=$newId, template=$template, threshold=$threshold")
            val resUrl = mMmkv.getString("resUrl", "")
            val imgUrl = resUrl + template
            val tempFile = template.replace("/", "_")
            val tempPath = "${mApp.cacheDir.absolutePath}/$tempFile"
            val imgFile = File(tempPath)

            val templatePath = if (imgFile.exists()) {
                imgFile.absolutePath
            } else {
                val file = ImageHelper.download(imgUrl, tempPath, 60)
                if (file == null) {
                    logError("图片下载失败(可能404): $imgUrl")
                    return null
                }
                if (!file.exists()) {
                    logError("下载后的文件不存在: ${file.absolutePath}")
                    return null
                }
                logd("下载成功 file=${file.absolutePath}")
                file.absolutePath
            }

            if (!File(templatePath).exists()) {
                logError("模板文件不存在(404): $templatePath")
                return null
            }

            val match = getImagePoints(newId, templatePath, threshold, maxMatches, 10)
            logd("离线/在线多目标找图, 模版: $newId, 小图: $templatePath, 匹配度: $threshold, 返回: $match")
            match
        } catch (e: Exception) {
            logError("findImages 异常: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    @Synchronized
    fun findImageCorners(id: String, template: String, threshold: Double): String? {
        return try {
            val newId = id.ifEmpty { "1" }
            logd("findImageCorners: id=$newId, template=$template, threshold=$threshold")
            val resUrl = mMmkv.getString("resUrl", "")
            val imgUrl = resUrl + template
            val tempFile = template.replace("/", "_")
            val tempPath = "${mApp.cacheDir.absolutePath}/$tempFile"
            val imgFile = File(tempPath)

            val templatePath = if (imgFile.exists()) {
                imgFile.absolutePath
            } else {
                val file = ImageHelper.download(imgUrl, tempPath, 60)
                if (file == null) {
                    logError("图片下载失败(可能404): $imgUrl")
                    return null
                }
                if (!file.exists()) {
                    logError("下载后的文件不存在: ${file.absolutePath}")
                    return null
                }
                logd("下载成功 file=${file.absolutePath}")
                file.absolutePath
            }

            if (!File(templatePath).exists()) {
                logError("模板文件不存在(404): $templatePath")
                return null
            }

            val center = templateMatch(newId, templatePath, threshold) ?: return null

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(templatePath, options)
            val tplW = options.outWidth.toDouble()
            val tplH = options.outHeight.toDouble()
            if (tplW <= 0 || tplH <= 0) {
                logError("模板尺寸获取失败: $templatePath")
                return null
            }

            val corners = listOf(
                Point(center.x - tplW / 2, center.y - tplH / 2),
                Point(center.x + tplW / 2, center.y - tplH / 2),
                Point(center.x + tplW / 2, center.y + tplH / 2),
                Point(center.x - tplW / 2, center.y + tplH / 2)
            )

            val jsonArray = JSONArray()
            for (p in corners) {
                jsonArray.put(JSONObject().apply {
                    put("x", p.x)
                    put("y", p.y)
                })
            }
            val resultJson = jsonArray.toString()
            logd("findImageCorners 结果: $resultJson")
            resultJson
        } catch (e: Exception) {
            logError("findImageCorners 异常: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    @Synchronized
    fun findImageCornersG(id: String, template: String, threshold: Double): String? {
        return try {
            val newId = id.ifEmpty { "1" }
            logd("findImageCornersG: id=$newId, template=$template, threshold=$threshold")
            val resUrl = mMmkv.getString("resUrl", "")
            val imgUrl = resUrl + template
            val tempFile = template.replace("/", "_")
            val tempPath = "${mApp.cacheDir.absolutePath}/$tempFile"
            val imgFile = File(tempPath)

            val templatePath = if (imgFile.exists()) {
                imgFile.absolutePath
            } else {
                val file = ImageHelper.download(imgUrl, tempPath, 60)
                if (file == null) {
                    logError("图片下载失败(可能404): $imgUrl")
                    return null
                }
                if (!file.exists()) {
                    logError("下载后的文件不存在: ${file.absolutePath}")
                    return null
                }
                logd("下载成功 file=${file.absolutePath}")
                file.absolutePath
            }

            if (!File(templatePath).exists()) {
                logError("模板文件不存在(404): $templatePath")
                return null
            }

            val center = templateMatchG(newId, templatePath, threshold) ?: return null

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(templatePath, options)
            val tplW = options.outWidth.toDouble()
            val tplH = options.outHeight.toDouble()
            if (tplW <= 0 || tplH <= 0) {
                logError("模板尺寸获取失败: $templatePath")
                return null
            }

            val corners = listOf(
                Point(center.x - tplW / 2, center.y - tplH / 2),
                Point(center.x + tplW / 2, center.y - tplH / 2),
                Point(center.x + tplW / 2, center.y + tplH / 2),
                Point(center.x - tplW / 2, center.y + tplH / 2)
            )

            val jsonArray = JSONArray()
            for (p in corners) {
                jsonArray.put(JSONObject().apply {
                    put("x", p.x)
                    put("y", p.y)
                })
            }
            val resultJson = jsonArray.toString()
            logd("findImageCornersG 结果: $resultJson")
            resultJson
        } catch (e: Exception) {
            logError("findImageCornersG 异常: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    // ---------- 内部工具方法 ----------

    private fun safeCrop(bitmap: Bitmap?, area: String): Bitmap? {
        if (area.isNotEmpty()) {
            val parts = area.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val x = parts[0].toDouble().toInt()
            val y = parts[1].toDouble().toInt()
            var width = parts[2].toDouble().toInt()
            var height = parts[3].toDouble().toInt()
            if (bitmap == null || width <= 0 || height <= 0) return null
            val bitmapWidth = bitmap.width
            val bitmapHeight = bitmap.height
            if (x + width > bitmapWidth) width = bitmapWidth - x
            if (y + height > bitmapHeight) height = bitmapHeight - y
            if (width <= 0 || height <= 0) return null
            return Bitmap.createBitmap(bitmap, x, y, width, height)
        }
        return bitmap
    }

    private fun Image.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width
        var bitmap = createBitmap(width + rowPadding / pixelStride, height)
        bitmap.copyPixelsFromBuffer(buffer)
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
        return bitmap
    }

    // 注意：此处 save 扩展函数为 public，方便外部调用
    fun Bitmap.save(path: String, quality: Int): Boolean {
        return try {
            FileOutputStream(path).use { fos ->
                compress(Bitmap.CompressFormat.JPEG, quality, fos)
            }
            true
        } catch (e: Exception) {
            logError("Bitmap 保存失败: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}