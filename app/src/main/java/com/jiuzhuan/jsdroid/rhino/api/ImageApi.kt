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
import com.jiuzhuan.jsdroid.rhino.api.GlobalApi.sleep
import com.jiuzhuan.jsdroid.rhino.helper.ImageHelper
import com.jiuzhuan.jsdroid.rhino.helper.Screencap
import org.opencv.core.Point
import java.io.File
import java.io.FileOutputStream

@SuppressLint("StaticFieldLeak")
object ImageApi {
    private val mMmkv = getMmkv()
    private val mApp = getApp()

    private fun logd(message: String) {
        if (BuildConfig.DEBUG) {
            Log.e("ImageApi", message)
        }
    }

    @JvmStatic
    @Synchronized
    fun takeCapture(id: String, area: String): String {
        val newId = id.ifEmpty { "1" }
        logd("id=$newId, area=$area")
        val imgPath = "${mApp.cacheDir.absolutePath}/$newId.jpg"
        var image: Image? = null
        var bitmap: Bitmap? = null
        try {
            for (i in 1..10) {
                image = Screencap.getImageReader().acquireLatestImage()
                if (image != null) { // 条件成立时退出循环
                    break
                }
                sleep(16)
            }
            bitmap = safeCrop(image?.toBitmap(), area)

            if (bitmap!!.save(imgPath, 50)) {
                logd("保存成功${imgPath}")
            } else {
                logd("保存失败")
            }

        } finally {
            image?.close()
            bitmap?.recycle()
        }
        return imgPath
    }

    @JvmStatic
    @Synchronized
    fun findImage(id: String, template: String, threshold: Double): Point? {
        val newId = id.ifEmpty { "1" }
        logd("id=$newId, template=$template, threshold=$threshold")
        val resUrl = mMmkv.getString("resUrl", "")

        val imgUrl = resUrl + template

        val tempFile = template.replace("/", "_")
        val tempPath = "${mApp.cacheDir.absolutePath}/$tempFile"

        val imgFile = File(tempPath)
        if (imgFile.exists()) {
            val match = templateMatch(newId, imgFile.absolutePath, threshold) ?: return null
            logd(
                "离线找图, 模版: $id, 小图: ${imgFile.absolutePath}, 匹配度: $threshold, 返回: $match"
            )
            return match
        } else {
            val file = ImageHelper.download(imgUrl, tempPath, 60)
            logd("file=${file.absolutePath}")

            val match = templateMatch(
                newId, file.absolutePath, threshold
            ) ?: return null
            logd(
                "在线找图, 模版: $newId, 小图: ${imgFile.absolutePath}, 匹配度: $threshold, 返回: $match"
            )
            return match
        }
    }


    @JvmStatic
    @Synchronized
    fun findImages(
        id: String, template: String, threshold: Double, maxMatches: Int
    ): List<Point>? {
        val newId = id.ifEmpty { "1" }
        logd("id=$newId, template=$template, threshold=$threshold")
        val resUrl = mMmkv.getString("resUrl", "")

        val imgUrl = resUrl + template

        val tempFile = template.replace("/", "_")
        val tempPath = "${mApp.cacheDir.absolutePath}/$tempFile"

        val imgFile = File(tempPath)

        if (imgFile.exists()) {
            val match = getImagePoints(
                newId, imgFile.absolutePath, threshold, maxMatches, 10
            )
            logd(
                "离线找图, 模版: $newId, 小图: ${imgFile.absolutePath}, 匹配度: $threshold, 返回: $match"
            )
            return match
        } else {
            val file = ImageHelper.download(imgUrl, tempPath, 60)
            logd("file=${file.absolutePath}")
            val match = getImagePoints(
                newId, imgFile.absolutePath, threshold, maxMatches, 10
            )
            logd(
                "在线找图, 模版: $newId, 小图: ${imgFile.absolutePath}, 匹配度: $threshold, 返回: $match"
            )
            return match
        }
    }

    private fun safeCrop(bitmap: Bitmap?, area: String): Bitmap? {
        if (area.isNotEmpty()) {
            val parts = area.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            // 参数校验
            val x = parts[0].toDouble().toInt()
            val y = parts[1].toDouble().toInt()
            var width = parts[2].toDouble().toInt()
            var height = parts[3].toDouble().toInt()
            if (bitmap == null || width <= 0 || height <= 0) return null
            // 获取 Bitmap 尺寸
            val bitmapWidth = bitmap.width
            val bitmapHeight = bitmap.height
            // 调整 width/height 不超过剩余区域
            if (x + width > bitmapWidth) {
                width = bitmapWidth - x
            }
            if (y + height > bitmapHeight) {
                height = bitmapWidth - y
            }
            // 二次校验（避免调整后仍不合法）
            if (width <= 0 || height <= 0) return null
            // 执行裁剪
            return Bitmap.createBitmap(bitmap, x, y, width, height)
        }
        return bitmap
    }

//    private fun getArea(area: String): Rect? {
//        if (area.isNotEmpty()) {
//            val parts = area.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//            if (parts.size == 4) {
//                val rect = Rect()
//                rect.left = parts[0].toDouble().toInt()
//                rect.top = parts[1].toDouble().toInt()
//                rect.right = rect.left + parts[2].toDouble().toInt()
//                rect.bottom = rect.top + parts[3].toDouble().toInt()
//                if (rect.bottom > rect.top && rect.bottom > 0 && rect.right > rect.left && rect.right > 0) {
//                    return rect
//                }
//            }
//        }
//        return null
//    }

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

    fun Bitmap.save(path: String, quality: Int): Boolean {
        try {
            val fos = FileOutputStream(path)
            compress(Bitmap.CompressFormat.JPEG, quality, fos)
            fos.flush()
            fos.close()
        } catch (ignored: Exception) {
            ignored.printStackTrace()
            return false
        }
        return true
    }
}
