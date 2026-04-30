package com.jiuzhuan.jsdroid.opencv

import android.graphics.BitmapFactory
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import org.opencv.calib3d.Calib3d
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.features2d.BFMatcher
import org.opencv.features2d.ORB
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import kotlin.math.abs

object ImageMatcher {
    // 模板匹配（返回中心坐标）
    fun templateMatch(
        sourcePath: String, templatePath: String, maxVal: Double
    ): Point? {
        val srcMat = Imgcodecs.imread(sourcePath)
        val templateMat = Imgcodecs.imread(templatePath)
        if (srcMat.empty() || templateMat.empty()) return null

        val resultMat = Mat()
        if(sourcePath.contains("_gray")){
            // 灰度化
            val srcGray = Mat()
            val templateGray = Mat()
            Imgproc.cvtColor(srcMat, srcGray, Imgproc.COLOR_BGR2GRAY)
            Imgproc.cvtColor(templateMat, templateGray, Imgproc.COLOR_BGR2GRAY)
            // 执行匹配
            Imgproc.matchTemplate(srcGray, templateGray, resultMat, Imgproc.TM_CCOEFF_NORMED)
        }else{
            Imgproc.matchTemplate(srcMat, templateMat, resultMat, Imgproc.TM_CCOEFF_NORMED)
        }

        // 获取最佳匹配位置
        val minMaxLoc = Core.minMaxLoc(resultMat)
        return if (minMaxLoc.maxVal > maxVal) { // 置信度阈值
            Point(
                minMaxLoc.maxLoc.x + templateMat.cols() / 2,
                minMaxLoc.maxLoc.y + templateMat.rows() / 2
            )
        } else {
            null
        }
    }

    fun templateMatchG(
        sourcePath: String, templatePath: String, maxVal: Double
    ): Point? {
        val srcMat = Imgcodecs.imread(sourcePath)
        val templateMat = Imgcodecs.imread(templatePath)
        if (srcMat.empty() || templateMat.empty()) return null

        // 灰度化
        val srcGray = Mat()
        val templateGray = Mat()
        Imgproc.cvtColor(srcMat, srcGray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.cvtColor(templateMat, templateGray, Imgproc.COLOR_BGR2GRAY)

        // 执行匹配
        val resultMat = Mat()
        Imgproc.matchTemplate(srcGray, templateGray, resultMat, Imgproc.TM_CCOEFF_NORMED)

        // 获取最佳匹配位置
        val minMaxLoc = Core.minMaxLoc(resultMat)
        return if (minMaxLoc.maxVal > maxVal) { // 置信度阈值
            Point(
                minMaxLoc.maxLoc.x + templateMat.cols() / 2,
                minMaxLoc.maxLoc.y + templateMat.rows() / 2
            )
        } else {
            null
        }
    }

    // 特征匹配（返回中心坐标）
    fun featureMatch(sourcePath: String, templatePath: String): Point? {
        val srcMat = Imgcodecs.imread(sourcePath)
        val templateMat = Imgcodecs.imread(templatePath)
        if (srcMat.empty() || templateMat.empty()) return null

        // ORB 特征检测
        val orb = ORB.create()
        val keypointsSrc = MatOfKeyPoint()
        val descriptorsSrc = Mat()
        val keypointsTemplate = MatOfKeyPoint()
        val descriptorsTemplate = Mat()
        orb.detectAndCompute(srcMat, Mat(), keypointsSrc, descriptorsSrc)
        orb.detectAndCompute(templateMat, Mat(), keypointsTemplate, descriptorsTemplate)

        // 特征匹配
        val matcher = BFMatcher.create(BFMatcher.BRUTEFORCE_HAMMING)
        val matches = MatOfDMatch()
        matcher.match(descriptorsTemplate, descriptorsSrc, matches)

        // 筛选优质匹配
        val matchesList = matches.toList()
        if (matchesList.isEmpty()) return null
        val minDistance = (matchesList.minBy { it.distance }?.distance ?: 0.0).toDouble()
        val goodMatches = matchesList.filter { it.distance <= maxOf(3 * minDistance, 50.0) }

        // 计算单应性矩阵
        val srcPoints = MatOfPoint2f().apply {
            fromList(goodMatches.map { keypointsSrc.toList()[it.trainIdx].pt })
        }
        val dstPoints = MatOfPoint2f().apply {
            fromList(goodMatches.map { keypointsTemplate.toList()[it.queryIdx].pt })
        }
        val homography = Calib3d.findHomography(dstPoints, srcPoints, Calib3d.RANSAC, 5.0)

        // 计算变换后中心点
        val templateCorners = MatOfPoint(
            Point(0.0, 0.0),
            Point(templateMat.cols().toDouble(), 0.0),
            Point(templateMat.cols().toDouble(), templateMat.rows().toDouble()),
            Point(0.0, templateMat.rows().toDouble())
        )
        val transformedCorners = MatOfPoint()
        Core.perspectiveTransform(templateCorners, transformedCorners, homography)

        return Point(
            transformedCorners.toList().map { it.x }.average(),
            transformedCorners.toList().map { it.y }.average()
        )
    }

    fun getImagePoints(
        sourcePath: String, templatePath: String, threshold: Double, maxMatches: Int,  //最大找图数量
        distanceThreshold: Int //去重距离阈值，默认10
    ): List<Point>? {
        val srcMat = Imgcodecs.imread(sourcePath)
        val templateMat = Imgcodecs.imread(templatePath)
        if (srcMat.empty() || templateMat.empty()) return ArrayList()

        val resultCols = srcMat.cols() - templateMat.cols() / 2
        val resultRows = srcMat.rows() - templateMat.rows() / 2
        val result = Mat(resultRows, resultCols, CvType.CV_32FC1)
        Imgproc.matchTemplate(srcMat, templateMat, result, Imgproc.TM_CCOEFF_NORMED)

        if (resultCols <= 0 || resultRows <= 0) {
            return ArrayList()
        }

        val points: MutableList<Point> = ArrayList()
        var times = 0
        while (points.size < maxMatches) {
            val minMaxLocResult = Core.minMaxLoc(result)
            if (minMaxLocResult.maxVal >= threshold) {
                val matchLoc = minMaxLocResult.maxLoc
                var isDuplicate = false
                for (p in points) {
                    if (abs(p.x - matchLoc.x) < distanceThreshold && abs(p.y - matchLoc.y) < distanceThreshold) {
                        isDuplicate = true
                        break
                    }
                }
                if (!isDuplicate) {
                    points.add(Point(matchLoc.x, matchLoc.y))
                    Imgproc.rectangle(
                        result, Rect(
                            Point(matchLoc.x, matchLoc.y),
                            Size(templateMat.cols().toDouble(), templateMat.rows().toDouble())
                        ), Scalar(0.0), -1
                    )
                    times = 0
                }
            } else {
                times++
                if (times > 5) {
                    break
                }
            }
            Imgproc.rectangle(
                result, Rect(
                    Point(minMaxLocResult.maxLoc.x, minMaxLocResult.maxLoc.y),
                    Size(templateMat.cols().toDouble(), templateMat.rows().toDouble())
                ), Scalar(0.0), -1
            )
        }
        return points
    }

    fun hasColor(sourcePath: String, x: Int, y: Int, color: Int): Int {
        val bitmap = BitmapFactory.decodeFile(sourcePath) ?: return -1
        val pColor = bitmap.getPixel(x, y)
        val rgb =
            doubleArrayOf(pColor.red.toDouble(), pColor.green.toDouble(), pColor.blue.toDouble())
        if (rgb.size == 3) {
            val r = rgb[0]
            val g = rgb[1]
            val b = rgb[2]
            val semblance: Double =
                (255 - (Math.abs(color.red - r) * 255 * 0.297 + Math.abs(color.green - g) * 255 * 0.593 + Math.abs(
                    color.blue - b
                ) * 255 * 11.0 / 100)) / 255
            return abs(semblance).toInt()
        } else {
            return -1
        }
    }
}
