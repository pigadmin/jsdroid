package com.jiuzhuan.jsdroid.rhino.helper

import android.media.Image
import android.media.ImageReader

object Screencap {
    private val sLock = Any()
    private lateinit var mImageReader: ImageReader

    fun setImageReader(reader: ImageReader) {
        mImageReader = reader
    }

    fun getImageReader(): ImageReader {
        return mImageReader
    }

    fun takeImage(): Image? {
        return synchronized(sLock) {
            var newImage = mImageReader.acquireLatestImage()
            if (newImage == null) {
                newImage = mImageReader.acquireNextImage()
            }
            if (newImage == null) {
                Thread.sleep(16)
                takeImage()
            }
            newImage
        }
    }

    fun closeAll() {
        mImageReader.close()
    }


}
