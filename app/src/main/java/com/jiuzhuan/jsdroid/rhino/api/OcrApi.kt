@file:JvmName("OcrApi")

package com.jiuzhuan.jsdroid.rhino.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.jiuzhuan.jsdroid.utils.showException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object OcrApi {
    private val executor = Executors.newSingleThreadExecutor()
    private val builder = ChineseTextRecognizerOptions.Builder()

    @JvmStatic
    fun recognizeText(path: String): String {
        val bitmap: Bitmap = BitmapFactory.decodeFile(path)
        val recognizer = TextRecognition.getClient(builder.build())
        val latch = CountDownLatch(1)
        var result = ""

        executor.execute {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    result = visionText.text
                }
                .addOnFailureListener { e ->
                    showException(e)
                }
                .addOnCompleteListener {
                    latch.countDown()
                    recognizer.close()
                }

        }
        latch.await(1, TimeUnit.SECONDS)
        return result
    }
}
