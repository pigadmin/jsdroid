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

    /**
     * 模板匹配（返回中心坐标）
     * 当图片加载失败、匹配失败或发生异常时返回 null
     */
    fun templateMatch(
        sourcePath: String, templatePath: String, maxVal: Double
    ): Point? {
        return try {
            val srcMat = Imgcodecs.imread(sourcePath)
            val templateMat = Imgcodecs.imread(templatePath)
            if (srcMat.empty() || templateMat.empty()) return null

            val resultMat = Mat()
            if (sourcePath.contains("_gray")) {
                val srcGray = Mat()
                val templateGray = Mat()
                Imgproc.cvtColor(srcMat, srcGray, Imgproc.COLOR_BGR2GRAY)
                Imgproc.cvtColor(templateMat, templateGray, Imgproc.COLOR_BGR2GRAY)
                Imgproc.matchTemplate(srcGray, templateGray, resultMat, Imgproc.TM_CCOEFF_NORMED)
            } else {
                Imgproc.matchTemplate(srcMat, templateMat, resultMat, Imgproc.TM_CCOEFF_NORMED)
            }

            val minMaxLoc = Core.minMaxLoc(resultMat)
            if (minMaxLoc.maxVal > maxVal) {
                Point(
                    minMaxLoc.maxLoc.x + templateMat.cols() / 2,
                    minMaxLoc.maxLoc.y + templateMat.rows() / 2
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 灰度模板匹配（强制灰度）
     * 当图片加载失败、匹配失败或发生异常时返回 null
     */
    fun templateMatchG(
        sourcePath: String, templatePath: String, maxVal: Double
    ): Point? {
        return try {
            val srcMat = Imgcodecs.imread(sourcePath)
            val templateMat = Imgcodecs.imread(templatePath)
            if (srcMat.empty() || templateMat.empty()) return null

            val srcGray = Mat()
            val templateGray = Mat()
            Imgproc.cvtColor(srcMat, srcGray, Imgproc.COLOR_BGR2GRAY)
            Imgproc.cvtColor(templateMat, templateGray, Imgproc.COLOR_BGR2GRAY)

            val resultMat = Mat()
            Imgproc.matchTemplate(srcGray, templateGray, resultMat, Imgproc.TM_CCOEFF_NORMED)

            val minMaxLoc = Core.minMaxLoc(resultMat)
            if (minMaxLoc.maxVal > maxVal) {
                Point(
                    minMaxLoc.maxLoc.x + templateMat.cols() / 2,
                    minMaxLoc.maxLoc.y + templateMat.rows() / 2
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 特征匹配（ORB + 单应性变换）
     * 当图片加载失败、特征点不足或发生异常时返回 null
     */
    fun featureMatch(sourcePath: String, templatePath: String): Point? {
        return try {
            val srcMat = Imgcodecs.imread(sourcePath)
            val templateMat = Imgcodecs.imread(templatePath)
            if (srcMat.empty() || templateMat.empty()) return null

            val orb = ORB.create()
            val keypointsSrc = MatOfKeyPoint()
            val descriptorsSrc = Mat()
            val keypointsTemplate = MatOfKeyPoint()
            val descriptorsTemplate = Mat()
            orb.detectAndCompute(srcMat, Mat(), keypointsSrc, descriptorsSrc)
            orb.detectAndCompute(templateMat, Mat(), keypointsTemplate, descriptorsTemplate)

            val matcher = BFMatcher.create(BFMatcher.BRUTEFORCE_HAMMING)
            val matches = MatOfDMatch()
            matcher.match(descriptorsTemplate, descriptorsSrc, matches)

            val matchesList = matches.toList()
            if (matchesList.isEmpty()) return null
            val minDistance = (matchesList.minBy { it.distance }?.distance ?: 0.0).toDouble()
            val goodMatches = matchesList.filter { it.distance <= maxOf(3 * minDistance, 50.0) }
            if (goodMatches.isEmpty()) return null

            val srcPoints = MatOfPoint2f().apply {
                fromList(goodMatches.map { keypointsSrc.toList()[it.trainIdx].pt })
            }
            val dstPoints = MatOfPoint2f().apply {
                fromList(goodMatches.map { keypointsTemplate.toList()[it.queryIdx].pt })
            }
            val homography = Calib3d.findHomography(dstPoints, srcPoints, Calib3d.RANSAC, 5.0)
            if (homography.empty()) return null

            val templateCorners = MatOfPoint(
                Point(0.0, 0.0),
                Point(templateMat.cols().toDouble(), 0.0),
                Point(templateMat.cols().toDouble(), templateMat.rows().toDouble()),
                Point(0.0, templateMat.rows().toDouble())
            )
            val transformedCorners = MatOfPoint()
            Core.perspectiveTransform(templateCorners, transformedCorners, homography)

            val pointList = transformedCorners.toList()
            if (pointList.isEmpty()) return null
            Point(
                pointList.map { it.x }.average(),
                pointList.map { it.y }.average()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 多目标模板匹配
     * 当图片加载失败或发生异常时返回 null；正常无匹配时返回空列表
     */
    fun getImagePoints(
        sourcePath: String, templatePath: String, threshold: Double, maxMatches: Int,
        distanceThreshold: Int
    ): List<Point>? {
        return try {
            val srcMat = Imgcodecs.imread(sourcePath)
            val templateMat = Imgcodecs.imread(templatePath)
            if (srcMat.empty() || templateMat.empty()) return ArrayList()

            val resultCols = srcMat.cols() - templateMat.cols() / 2
            val resultRows = srcMat.rows() - templateMat.rows() / 2
            if (resultCols <= 0 || resultRows <= 0) return ArrayList()

            val result = Mat(resultRows, resultCols, CvType.CV_32FC1)
            Imgproc.matchTemplate(srcMat, templateMat, result, Imgproc.TM_CCOEFF_NORMED)

            val points: MutableList<Point> = ArrayList()
            var times = 0
            while (points.size < maxMatches) {
                val minMaxLocResult = Core.minMaxLoc(result)
                if (minMaxLocResult.maxVal >= threshold) {
                    val matchLoc = minMaxLocResult.maxLoc
                    var isDuplicate = false
                    for (p in points) {
                        if (abs(p.x - matchLoc.x) < distanceThreshold &&
                            abs(p.y - matchLoc.y) < distanceThreshold
                        ) {
                            isDuplicate = true
                            break
                        }
                    }
                    if (!isDuplicate) {
                        points.add(Point(matchLoc.x, matchLoc.y))
                        Imgproc.rectangle(
                            result,
                            Rect(
                                Point(matchLoc.x, matchLoc.y),
                                Size(templateMat.cols().toDouble(), templateMat.rows().toDouble())
                            ),
                            Scalar(0.0), -1
                        )
                        times = 0
                    }
                } else {
                    times++
                    if (times > 5) break
                }
                // 屏蔽已检测区域
                Imgproc.rectangle(
                    result,
                    Rect(
                        Point(minMaxLocResult.maxLoc.x, minMaxLocResult.maxLoc.y),
                        Size(templateMat.cols().toDouble(), templateMat.rows().toDouble())
                    ),
                    Scalar(0.0), -1
                )
            }
            points
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 比色（检查指定坐标颜色相似度）
     * 当图片加载失败、坐标越界或异常时返回 -1
     */
    fun hasColor(sourcePath: String, x: Int, y: Int, color: Int): Int {
        return try {
            val bitmap = BitmapFactory.decodeFile(sourcePath) ?: return -1
            if (x < 0 || y < 0 || x >= bitmap.width || y >= bitmap.height) return -1

            val pColor = bitmap.getPixel(x, y)
            val r = pColor.red.toDouble()
            val g = pColor.green.toDouble()
            val b = pColor.blue.toDouble()

            val semblance = (255 - (abs(color.red - r) * 255 * 0.297 +
                    abs(color.green - g) * 255 * 0.593 +
                    abs(color.blue - b) * 255 * 11.0 / 100)) / 255
            abs(semblance).toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }
}